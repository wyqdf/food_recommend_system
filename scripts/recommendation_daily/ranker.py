from __future__ import annotations

import json
import math
from collections import Counter
from dataclasses import dataclass
from datetime import date, datetime

from .config import AppConfig
from .data import PositiveInteraction, RecipeRecord, UserProfileRecord


@dataclass
class RankedRecipe:
    recipe_id: int
    score: float
    reason_json: str
    rank_no: int
    selected_for_delivery: int


def generate_daily_recommendations(
    config: AppConfig,
    recipes: list[RecipeRecord],
    user_profiles: dict[int, UserProfileRecord],
    dynamic_interest_sets: dict[int, list[PositiveInteraction]],
    recent_pushed: dict[int, set[int]],
    recent_cooked: dict[int, set[int]],
    model_rankings: dict[int, list[tuple[int, float]]],
    exploration_recipe_ids: list[int],
    seen_recipe_ids_by_user: dict[int, set[int]],
    business_date: str,
) -> dict[int, list[RankedRecipe]]:
    recipe_by_id = {recipe.recipe_id: recipe for recipe in recipes}
    business_day = date.fromisoformat(business_date)
    results: dict[int, list[RankedRecipe]] = {}

    for user_id, model_candidates in model_rankings.items():
        profile = user_profiles.get(
            user_id,
            UserProfileRecord(user_id=user_id, taboo_ingredients=[], available_cookwares=[]),
        )
        seen_recipe_ids = seen_recipe_ids_by_user.get(user_id, set())
        candidate_rows = _build_candidate_rows(
            config=config,
            recipe_by_id=recipe_by_id,
            profile=profile,
            model_candidates=model_candidates,
            exploration_recipe_ids=exploration_recipe_ids,
            seen_recipe_ids=seen_recipe_ids,
            dynamic_seeds=dynamic_interest_sets.get(user_id, []),
            recent_pushed=recent_pushed.get(user_id, set()),
            recent_cooked=recent_cooked.get(user_id, set()),
            business_day=business_day,
        )
        if not candidate_rows:
            results[user_id] = []
            continue

        ranked_full = sorted(candidate_rows, key=lambda item: item["final_score"], reverse=True)[:config.daily.cke_candidate_topk]
        delivery_ids = _select_delivery_ids(ranked_full, config)

        ranked_recipes: list[RankedRecipe] = []
        for rank_no, row in enumerate(ranked_full, start=1):
            ranked_recipes.append(
                RankedRecipe(
                    recipe_id=row["recipe_id"],
                    score=float(row["final_score"]),
                    reason_json=build_reason_json(
                        row["recipe"],
                        recipe_by_id,
                        dynamic_interest_sets.get(user_id, []),
                    ),
                    rank_no=rank_no,
                    selected_for_delivery=1 if row["recipe_id"] in delivery_ids else 0,
                )
            )
        results[user_id] = ranked_recipes
    return results


def build_reason_json(
    recipe: RecipeRecord,
    recipe_by_id: dict[int, RecipeRecord],
    dynamic_seeds: list[PositiveInteraction],
) -> str:
    matched_tags: list[str] = []
    graph_path: list[str] = []
    main_reason = "今日推荐"
    for interaction in dynamic_seeds:
        seed = recipe_by_id.get(interaction.recipe_id)
        if seed is None or seed.recipe_id == recipe.recipe_id:
            continue
        shared_main = sorted(set(recipe.main_ingredient_names) & set(seed.main_ingredient_names))
        shared_ingredients = sorted(set(recipe.ingredient_names) & set(seed.ingredient_names))
        shared_categories = sorted(set(recipe.category_names) & set(seed.category_names))
        if shared_main:
            main_reason = f"与你近期偏好的“{shared_main[0]}”主食材相关"
            matched_tags = shared_main[:3]
            graph_path = [f"seed_recipe:{seed.recipe_id}", "main_ingredient", shared_main[0], f"recipe:{recipe.recipe_id}"]
            break
        if shared_ingredients:
            main_reason = f"与你最近关注的“{shared_ingredients[0]}”相关"
            matched_tags = shared_ingredients[:3]
            graph_path = [f"seed_recipe:{seed.recipe_id}", "ingredient", shared_ingredients[0], f"recipe:{recipe.recipe_id}"]
            break
        if shared_categories:
            main_reason = f"与你近期偏好的“{shared_categories[0]}”分类相关"
            matched_tags = shared_categories[:3]
            graph_path = [f"seed_recipe:{seed.recipe_id}", "category", shared_categories[0], f"recipe:{recipe.recipe_id}"]
            break
    payload = {
        "main_reason": main_reason,
        "matched_tags": matched_tags if matched_tags else (recipe.category_names[:2] or recipe.main_ingredient_names[:2]),
        "graph_path": graph_path if graph_path else _fallback_graph_path(recipe),
    }
    return json.dumps(payload, ensure_ascii=False)


def _build_candidate_rows(
    config: AppConfig,
    recipe_by_id: dict[int, RecipeRecord],
    profile: UserProfileRecord,
    model_candidates: list[tuple[int, float]],
    exploration_recipe_ids: list[int],
    seen_recipe_ids: set[int],
    dynamic_seeds: list[PositiveInteraction],
    recent_pushed: set[int],
    recent_cooked: set[int],
    business_day: date,
) -> list[dict]:
    candidate_rows: list[dict] = []
    added_recipe_ids: set[int] = set()

    for recipe_id, model_score in model_candidates[:config.cke_full.model_topk_count]:
        recipe = recipe_by_id.get(recipe_id)
        if recipe is None or recipe_id in added_recipe_ids or _blocked_by_profile(profile, recipe):
            continue
        candidate_rows.append(
            _build_candidate_row(
                recipe,
                model_score,
                dynamic_seeds,
                recipe_by_id,
                business_day,
            )
        )
        added_recipe_ids.add(recipe_id)

    added_exploration = 0
    for recipe_id in exploration_recipe_ids:
        if added_exploration >= config.cke_full.exploration_insert_count:
            break
        if recipe_id in added_recipe_ids or recipe_id in seen_recipe_ids:
            continue
        recipe = recipe_by_id.get(recipe_id)
        if recipe is None or _blocked_by_profile(profile, recipe):
            continue
        candidate_rows.append(
            _build_candidate_row(
                recipe,
                0.0,
                dynamic_seeds,
                recipe_by_id,
                business_day,
            )
        )
        added_recipe_ids.add(recipe_id)
        added_exploration += 1

    for recipe_id, model_score in model_candidates[config.cke_full.model_topk_count:]:
        if len(candidate_rows) >= config.daily.cke_candidate_topk:
            break
        if recipe_id in added_recipe_ids:
            continue
        recipe = recipe_by_id.get(recipe_id)
        if recipe is None or _blocked_by_profile(profile, recipe):
            continue
        candidate_rows.append(
            _build_candidate_row(
                recipe,
                model_score,
                dynamic_seeds,
                recipe_by_id,
                business_day,
            )
        )
        added_recipe_ids.add(recipe_id)

    if not candidate_rows:
        return []

    _normalize_field(candidate_rows, "cke_raw", "cke_score")
    _normalize_field(candidate_rows, "dynamic_raw", "dynamic_score")
    _normalize_field(candidate_rows, "trend_raw", "trend_score")
    for row in candidate_rows:
        final_score = (
            0.65 * row["cke_score"]
            + 0.25 * row["dynamic_score"]
            + 0.05 * row["freshness_raw"]
            + 0.05 * row["trend_score"]
        )
        row["final_score"] = _apply_cooldowns(
            final_score,
            row["recipe_id"],
            recent_pushed,
            recent_cooked,
            config,
        )
    return candidate_rows


def _build_candidate_row(
    recipe: RecipeRecord,
    model_score: float,
    dynamic_seeds: list[PositiveInteraction],
    recipe_by_id: dict[int, RecipeRecord],
    business_day: date,
) -> dict:
    return {
        "recipe": recipe,
        "recipe_id": recipe.recipe_id,
        "cke_raw": model_score,
        "dynamic_raw": _dynamic_match_score(recipe, dynamic_seeds, recipe_by_id),
        "freshness_raw": _freshness_score(recipe.create_time, business_day),
        "trend_raw": math.log1p(max(recipe.like_count, 0)),
    }


def _dynamic_match_score(
    recipe: RecipeRecord,
    dynamic_seeds: list[PositiveInteraction],
    recipe_by_id: dict[int, RecipeRecord],
) -> float:
    score = 0.0
    for rank, interaction in enumerate(dynamic_seeds, start=1):
        seed_recipe = recipe_by_id.get(interaction.recipe_id)
        if seed_recipe is None or seed_recipe.recipe_id == recipe.recipe_id:
            continue
        rank_decay = 1.0 / math.log2(rank + 1)
        relation_score = _relation_match_score(recipe, seed_recipe)
        if relation_score <= 0:
            continue
        score += interaction.weight * rank_decay * relation_score
    return score


def _relation_match_score(candidate: RecipeRecord, seed_recipe: RecipeRecord) -> float:
    score = 0.0
    if set(candidate.main_ingredient_names) & set(seed_recipe.main_ingredient_names):
        score += 1.2
    normal_candidate_ingredients = set(candidate.ingredient_names) - set(candidate.main_ingredient_names)
    normal_seed_ingredients = set(seed_recipe.ingredient_names) - set(seed_recipe.main_ingredient_names)
    if normal_candidate_ingredients & normal_seed_ingredients:
        score += 0.8
    if set(candidate.category_names) & set(seed_recipe.category_names):
        score += 0.7
    if candidate.taste_name and candidate.taste_name == seed_recipe.taste_name:
        score += 0.5
    if candidate.technique_name and candidate.technique_name == seed_recipe.technique_name:
        score += 0.4
    if candidate.time_cost_name and candidate.time_cost_name == seed_recipe.time_cost_name:
        score += 0.2
    if candidate.difficulty_name and candidate.difficulty_name == seed_recipe.difficulty_name:
        score += 0.2
    return score


def _select_delivery_ids(ranked_rows: list[dict], config: AppConfig) -> set[int]:
    category_counter: Counter[str] = Counter()
    ingredient_counter: Counter[str] = Counter()
    selected_ids: set[int] = set()
    for row in ranked_rows:
        recipe: RecipeRecord = row["recipe"]
        category_key = recipe.category_names[0] if recipe.category_names else f"recipe:{recipe.recipe_id}"
        ingredient_key = recipe.main_ingredient_names[0] if recipe.main_ingredient_names else f"recipe:{recipe.recipe_id}"
        if category_counter[category_key] >= config.daily.max_same_category:
            continue
        if ingredient_counter[ingredient_key] >= config.daily.max_same_main_ingredient:
            continue
        selected_ids.add(recipe.recipe_id)
        category_counter[category_key] += 1
        ingredient_counter[ingredient_key] += 1
        if len(selected_ids) >= config.daily.final_delivery_count:
            break
    if len(selected_ids) >= config.daily.final_delivery_count:
        return selected_ids
    for row in ranked_rows:
        selected_ids.add(row["recipe_id"])
        if len(selected_ids) >= config.daily.final_delivery_count:
            break
    return selected_ids


def _normalize_field(rows: list[dict], raw_key: str, target_key: str) -> None:
    values = [float(row[raw_key]) for row in rows]
    min_value = min(values)
    max_value = max(values)
    if math.isclose(max_value, min_value):
        for row in rows:
            row[target_key] = 0.0 if math.isclose(max_value, 0.0) else 1.0
        return
    scale = max_value - min_value
    for row in rows:
        row[target_key] = (float(row[raw_key]) - min_value) / scale


def _freshness_score(create_time: datetime | None, business_day: date) -> float:
    if create_time is None:
        return 0.0
    days_ago = max((business_day - create_time.date()).days, 0)
    return float(math.exp(-days_ago / 30.0))


def _blocked_by_profile(profile: UserProfileRecord, recipe: RecipeRecord) -> bool:
    taboo_set = {item.strip() for item in profile.taboo_ingredients if item.strip()}
    cookware_set = {item.strip() for item in profile.available_cookwares if item.strip()}
    if taboo_set and taboo_set.intersection(recipe.ingredient_names):
        return True
    if cookware_set and recipe.cookware and recipe.cookware not in cookware_set:
        return True
    return False


def _apply_cooldowns(
    score: float,
    recipe_id: int,
    recent_pushed: set[int],
    recent_cooked: set[int],
    config: AppConfig,
) -> float:
    adjusted = score
    if recipe_id in recent_pushed:
        adjusted *= config.daily.pushed_cooldown_multiplier
    if recipe_id in recent_cooked:
        adjusted *= config.daily.cooked_cooldown_multiplier
    return adjusted


def _fallback_graph_path(recipe: RecipeRecord) -> list[str]:
    if recipe.category_names:
        return [f"recipe:{recipe.recipe_id}", "category", recipe.category_names[0]]
    if recipe.main_ingredient_names:
        return [f"recipe:{recipe.recipe_id}", "main_ingredient", recipe.main_ingredient_names[0]]
    if recipe.taste_name:
        return [f"recipe:{recipe.recipe_id}", "taste", recipe.taste_name]
    return [f"recipe:{recipe.recipe_id}", "recommend"]
