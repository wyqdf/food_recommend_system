from __future__ import annotations

import json
import re
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

from .cke_full_runner import score_cke_full_topk, train_cke_full
from .config import AppConfig, load_config
from .data import (
    PositiveInteraction,
    build_dynamic_interest_sets,
    ensure_runtime_dirs,
    fetch_all_positive_interactions,
    fetch_baseline_user_ids,
    fetch_negative_overrides,
    fetch_non_favorite_strengths,
    fetch_recipe_records,
    fetch_recent_cooked_recipe_ids,
    fetch_recent_pushed_recipe_ids,
    fetch_user_profiles,
    map_external_recipe_ids,
    map_external_user_ids,
)
from .db import DatabaseClient
from .exporter import export_filtered_dataset, select_exploration_recipe_ids
from .ranker import generate_daily_recommendations


@dataclass
class PipelineResult:
    checkpoint_path: Path
    dataset_dir: Path
    metrics_path: Path
    affected_users: int
    affected_recipes: int


class DailyRecommendationPipeline:
    def __init__(self, config: AppConfig | None = None) -> None:
        self.config = config or load_config()
        ensure_runtime_dirs(self.config)
        self.client = DatabaseClient(self.config)
        self.client.ensure_daily_tables()

    def bootstrap_baseline(self) -> PipelineResult:
        return self._run_pipeline(full_refresh=True)

    def run_daily(self) -> PipelineResult:
        return self._run_pipeline(full_refresh=False)

    def use_existing_model(self) -> PipelineResult:
        business_date = self._business_date()
        phase = "use_existing_model"
        run_id = self.client.start_job_run(business_date, phase, self.config.daily.model_version)
        try:
            dataset_dir = self.config.paths.external_cke_root / "datasets" / self.config.cke_full.existing_dataset_name
            if not dataset_dir.exists():
                raise FileNotFoundError(f"未找到现有 CKE 数据集: {dataset_dir}")

            model_path = self._latest_external_cke_model_path()
            if model_path is None:
                raise FileNotFoundError("未找到可用的现有 CKEFullModify 模型文件")

            raw_model_rankings, metrics = score_cke_full_topk(
                self.config,
                dataset_dir=dataset_dir,
                model_path=model_path,
                topk=self.config.daily.cke_candidate_topk,
            )
            if not raw_model_rankings:
                raise RuntimeError("现有模型没有产出任何排序结果")

            user_id_map = map_external_user_ids(self.client, list(raw_model_rankings.keys()))
            old_recipe_ids = sorted(
                {
                    recipe_id
                    for user_rankings in raw_model_rankings.values()
                    for recipe_id, _score in user_rankings
                }
            )
            recipe_id_map = map_external_recipe_ids(self.client, old_recipe_ids)
            model_rankings = _remap_model_rankings(raw_model_rankings, user_id_map, recipe_id_map)
            if not model_rankings:
                raise RuntimeError("现有模型结果无法对齐到当前库")

            target_user_ids = sorted(model_rankings.keys())
            all_interactions = fetch_all_positive_interactions(self.client, target_user_ids)
            negative_overrides = fetch_negative_overrides(self.client, target_user_ids)
            non_favorite_strengths = fetch_non_favorite_strengths(self.client, target_user_ids)
            filtered_interactions = [
                interaction
                for interaction in (
                    _apply_negative_override(interaction, negative_overrides, non_favorite_strengths)
                    for interaction in all_interactions
                )
                if interaction is not None
            ]

            interaction_recipe_ids = {interaction.recipe_id for interaction in filtered_interactions}
            model_recipe_ids = {
                recipe_id
                for user_rankings in model_rankings.values()
                for recipe_id, _score in user_rankings
            }
            exploration_recipe_ids = select_exploration_recipe_ids(
                self.client,
                excluded_recipe_ids=interaction_recipe_ids | model_recipe_ids,
                limit=self.config.cke_full.exploration_pool_size,
            )
            recipes = fetch_recipe_records(
                self.client,
                sorted(interaction_recipe_ids | model_recipe_ids | set(exploration_recipe_ids)),
            )

            recommendations = generate_daily_recommendations(
                config=self.config,
                recipes=recipes,
                user_profiles=fetch_user_profiles(self.client, target_user_ids),
                dynamic_interest_sets=build_dynamic_interest_sets(filtered_interactions),
                recent_pushed=fetch_recent_pushed_recipe_ids(
                    self.client,
                    target_user_ids,
                    self.config.daily.pushed_cooldown_days,
                ),
                recent_cooked=fetch_recent_cooked_recipe_ids(
                    self.client,
                    target_user_ids,
                    self.config.daily.cooked_cooldown_days,
                ),
                model_rankings=model_rankings,
                exploration_recipe_ids=exploration_recipe_ids,
                seen_recipe_ids_by_user=_build_seen_recipe_ids_by_user(filtered_interactions),
                business_date=business_date,
            )

            self._persist_daily_recommendations(recommendations, business_date)
            self._write_run_manifest(
                business_date=business_date,
                phase=phase,
                dataset_dir=dataset_dir,
                model_path=model_path,
                metrics=metrics,
            )

            affected_users = len(recommendations)
            affected_recipes = len(
                {
                    ranked.recipe_id
                    for user_rankings in recommendations.values()
                    for ranked in user_rankings
                }
            )
            self.client.finish_job_run(
                run_id,
                status="success",
                affected_users=affected_users,
                affected_recipes=affected_recipes,
            )
            return PipelineResult(
                checkpoint_path=model_path,
                dataset_dir=dataset_dir,
                metrics_path=model_path.parent / "metrics.tsv",
                affected_users=affected_users,
                affected_recipes=affected_recipes,
            )
        except Exception as exc:
            self.client.finish_job_run(run_id, status="failed", error_message=str(exc))
            raise

    def _run_pipeline(self, *, full_refresh: bool) -> PipelineResult:
        business_date = self._business_date()
        phase = "bootstrap_baseline" if full_refresh else "run_daily"
        run_id = self.client.start_job_run(business_date, phase, self.config.daily.model_version)
        try:
            target_user_ids = fetch_baseline_user_ids(self.client, self.config)
            if not target_user_ids:
                raise RuntimeError("没有满足历史交互门槛的用户")

            all_interactions = fetch_all_positive_interactions(self.client, target_user_ids)
            negative_overrides = fetch_negative_overrides(self.client, target_user_ids)
            non_favorite_strengths = fetch_non_favorite_strengths(self.client, target_user_ids)
            filtered_interactions = [
                interaction
                for interaction in (
                    _apply_negative_override(interaction, negative_overrides, non_favorite_strengths)
                    for interaction in all_interactions
                )
                if interaction is not None
            ]
            if not filtered_interactions:
                raise RuntimeError("没有可用于 CKE_full 训练的正向交互")

            interaction_recipe_ids = sorted({interaction.recipe_id for interaction in filtered_interactions})
            recipes = fetch_recipe_records(self.client, interaction_recipe_ids)
            dataset = export_filtered_dataset(
                self.config,
                self.client,
                target_user_ids,
                filtered_interactions,
                recipes,
                full_refresh=full_refresh,
            )

            previous_model_path = None if full_refresh else self._latest_cke_model_path()
            training_result = train_cke_full(
                self.config,
                incremental=not full_refresh,
                dataset_dir=dataset.dataset_dir,
                previous_model_path=previous_model_path,
            )

            model_rankings, metrics = score_cke_full_topk(
                self.config,
                dataset_dir=dataset.dataset_dir,
                model_path=training_result.best_model_path,
                topk=self.config.daily.cke_candidate_topk,
            )
            dynamic_interest_sets = build_dynamic_interest_sets(filtered_interactions)
            seen_recipe_ids_by_user = _build_seen_recipe_ids_by_user(filtered_interactions)
            recommendations = generate_daily_recommendations(
                config=self.config,
                recipes=dataset.recipes,
                user_profiles=fetch_user_profiles(self.client, list(model_rankings.keys())),
                dynamic_interest_sets=dynamic_interest_sets,
                recent_pushed=fetch_recent_pushed_recipe_ids(
                    self.client,
                    list(model_rankings.keys()),
                    self.config.daily.pushed_cooldown_days,
                ),
                recent_cooked=fetch_recent_cooked_recipe_ids(
                    self.client,
                    list(model_rankings.keys()),
                    self.config.daily.cooked_cooldown_days,
                ),
                model_rankings=model_rankings,
                exploration_recipe_ids=dataset.exploration_recipe_ids,
                seen_recipe_ids_by_user=seen_recipe_ids_by_user,
                business_date=business_date,
            )
            self._persist_daily_recommendations(recommendations, business_date)
            self._write_run_manifest(
                business_date=business_date,
                phase=phase,
                dataset_dir=dataset.dataset_dir,
                model_path=training_result.best_model_path,
                metrics=metrics,
            )

            affected_users = len(recommendations)
            affected_recipes = len(dataset.recipes)
            self.client.finish_job_run(
                run_id,
                status="success",
                affected_users=affected_users,
                affected_recipes=affected_recipes,
            )
            return PipelineResult(
                checkpoint_path=training_result.best_model_path,
                dataset_dir=dataset.dataset_dir,
                metrics_path=training_result.metrics_path,
                affected_users=affected_users,
                affected_recipes=affected_recipes,
            )
        except Exception as exc:
            self.client.finish_job_run(run_id, status="failed", error_message=str(exc))
            raise

    def _persist_daily_recommendations(self, recommendations, business_date: str) -> None:
        if not recommendations:
            return
        user_ids = sorted(recommendations.keys())
        for chunk in _chunked_users(user_ids, chunk_size=500):
            placeholders = ", ".join(["%s"] * len(chunk))
            self.client.execute(
                f"DELETE FROM daily_recipe_recommendations WHERE biz_date = %s AND user_id IN ({placeholders})",
                tuple([business_date] + chunk),
            )

        rows = []
        for user_id, ranked_recipes in recommendations.items():
            for ranked in ranked_recipes:
                rows.append(
                    (
                        user_id,
                        business_date,
                        ranked.recipe_id,
                        ranked.rank_no,
                        ranked.selected_for_delivery,
                        ranked.score,
                        ranked.reason_json,
                        self.config.daily.model_version,
                    )
                )
        self.client.executemany(
            """
            INSERT INTO daily_recipe_recommendations (
                user_id, biz_date, recipe_id, rank_no,
                selected_for_delivery, model_score, reason_json, model_version
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """,
            rows,
        )

    def _write_run_manifest(
        self,
        *,
        business_date: str,
        phase: str,
        dataset_dir: Path,
        model_path: Path,
        metrics,
    ) -> None:
        manifest_path = self.config.paths.manifests_dir / "daily_result_manifest.json"
        manifest_path.write_text(
            json.dumps(
                {
                    "biz_date": business_date,
                    "phase": phase,
                    "model_version": self.config.daily.model_version,
                    "dataset_dir": str(dataset_dir),
                    "model_path": str(model_path),
                    "metrics": metrics,
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )

    def _business_date(self) -> str:
        return str(datetime.now(ZoneInfo(self.config.daily.timezone)).date())

    def _latest_cke_model_path(self) -> Path | None:
        candidates = sorted(self.config.paths.checkpoints_dir.glob("cke_full_*/model_epoch*.pt"))
        return candidates[-1] if candidates else None

    def _latest_external_cke_model_path(self) -> Path | None:
        root = (
            self.config.paths.external_cke_root
            / "trained_model"
            / self.config.cke_full.existing_model_family
            / self.config.cke_full.existing_dataset_name
        )
        if not root.exists():
            return None

        best_dir = None
        best_key = None
        for candidate_dir in root.iterdir():
            if not candidate_dir.is_dir():
                continue
            train_log = candidate_dir / "train.log"
            if not train_log.exists():
                continue
            metric_key = _extract_best_metric_key(train_log)
            mtime = candidate_dir.stat().st_mtime
            candidate_key = (*metric_key, mtime)
            if best_key is None or candidate_key > best_key:
                best_key = candidate_key
                best_dir = candidate_dir

        if best_dir is None:
            return None
        checkpoints = sorted(best_dir.glob("model_epoch*.pt"))
        return checkpoints[-1] if checkpoints else None


def _apply_negative_override(
    interaction: PositiveInteraction,
    negative_overrides: set[tuple[int, int]],
    non_favorite_strengths: dict[tuple[int, int], float],
) -> PositiveInteraction | None:
    pair = (interaction.user_id, interaction.recipe_id)
    if pair not in negative_overrides:
        return interaction
    non_favorite_weight = non_favorite_strengths.get(pair)
    if non_favorite_weight is None or non_favorite_weight <= 0:
        return None
    return PositiveInteraction(
        user_id=interaction.user_id,
        recipe_id=interaction.recipe_id,
        weight=non_favorite_weight,
        event_time=interaction.event_time,
        source="aggregated_without_favorite",
    )


def _build_seen_recipe_ids_by_user(interactions: list[PositiveInteraction]) -> dict[int, set[int]]:
    seen: dict[int, set[int]] = {}
    for interaction in interactions:
        seen.setdefault(interaction.user_id, set()).add(interaction.recipe_id)
    return seen


def _chunked_users(user_ids: list[int], chunk_size: int = 500):
    for start in range(0, len(user_ids), chunk_size):
        yield user_ids[start:start + chunk_size]


def _remap_model_rankings(
    raw_rankings: dict[int, list[tuple[int, float]]],
    user_id_map: dict[int, int],
    recipe_id_map: dict[int, int],
) -> dict[int, list[tuple[int, float]]]:
    remapped: dict[int, list[tuple[int, float]]] = {}
    for old_user_id, rankings in raw_rankings.items():
        new_user_id = user_id_map.get(int(old_user_id))
        if new_user_id is None:
            continue
        converted: list[tuple[int, float]] = []
        seen_recipe_ids: set[int] = set()
        for old_recipe_id, score in rankings:
            new_recipe_id = recipe_id_map.get(int(old_recipe_id))
            if new_recipe_id is None or new_recipe_id in seen_recipe_ids:
                continue
            seen_recipe_ids.add(new_recipe_id)
            converted.append((new_recipe_id, score))
        if converted:
            remapped[new_user_id] = converted
    return remapped


def _extract_best_metric_key(train_log: Path) -> tuple[float, float, float]:
    best_precision = 0.0
    best_recall = 0.0
    best_ndcg = 0.0
    pattern = re.compile(
        r"precision@100=([0-9.]+)\s+recall@100=([0-9.]+)\s+ndcg@100=([0-9.]+)"
    )
    for line in train_log.read_text(encoding="utf-8", errors="ignore").splitlines():
        if "Best (epoch" not in line:
            continue
        match = pattern.search(line)
        if not match:
            continue
        best_precision = float(match.group(1))
        best_recall = float(match.group(2))
        best_ndcg = float(match.group(3))
    return best_ndcg, best_recall, best_precision
