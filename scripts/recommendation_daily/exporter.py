from __future__ import annotations

import csv
import json
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from .config import AppConfig
from .data import PositiveInteraction, RecipeRecord, build_recipe_triples, fetch_recipe_records, resolve_image_path
from .db import DatabaseClient
from .knowledge_graph_bridge import build_filtered_knowledge_graph


@dataclass
class ExportedDataset:
    dataset_dir: Path
    user_mapping: dict[int, int]
    recipe_mapping: dict[int, int]
    recipes: list[RecipeRecord]
    triples: list[tuple[int, str, str]]
    exploration_recipe_ids: list[int]


def export_filtered_dataset(
    config: AppConfig,
    client: DatabaseClient,
    target_user_ids: list[int],
    positive_interactions: list[PositiveInteraction],
    recipes: list[RecipeRecord],
    *,
    full_refresh: bool,
) -> ExportedDataset:
    dataset_dir = config.paths.datasets_dir / config.cke_full.dataset_name
    if full_refresh and dataset_dir.exists():
        shutil.rmtree(dataset_dir)
    dataset_dir.mkdir(parents=True, exist_ok=True)

    interaction_recipe_ids = {interaction.recipe_id for interaction in positive_interactions}
    exploration_recipe_ids = select_exploration_recipe_ids(
        client,
        interaction_recipe_ids,
        config.cke_full.exploration_pool_size,
    )

    selected_recipe_ids = interaction_recipe_ids | set(exploration_recipe_ids)
    recipe_by_id = {recipe.recipe_id: recipe for recipe in recipes}
    missing_recipe_ids = sorted(selected_recipe_ids - set(recipe_by_id.keys()))
    if missing_recipe_ids:
        for recipe in fetch_recipe_records(client, missing_recipe_ids):
            recipe_by_id[recipe.recipe_id] = recipe
    selected_recipes = [recipe_by_id[recipe_id] for recipe_id in sorted(selected_recipe_ids) if recipe_id in recipe_by_id]
    selected_recipe_ids = {recipe.recipe_id for recipe in selected_recipes}
    filtered_interactions = [
        interaction
        for interaction in positive_interactions
        if interaction.recipe_id in selected_recipe_ids and interaction.user_id in set(target_user_ids)
    ]

    train_user_items, test_user_items, filtered_user_ids = _split_train_test(filtered_interactions)
    filtered_target_users = [user_id for user_id in target_user_ids if user_id in filtered_user_ids]
    user_mapping = {user_id: index for index, user_id in enumerate(filtered_target_users)}
    recipe_mapping = {recipe.recipe_id: index for index, recipe in enumerate(selected_recipes)}
    triples = build_recipe_triples(selected_recipes)

    _write_mapping_file(dataset_dir / "user_mapping.txt", user_mapping)
    _write_mapping_file(dataset_dir / "recipe_mapping.txt", recipe_mapping)
    _write_cf_file(dataset_dir / "train.txt", user_mapping, recipe_mapping, train_user_items)
    _write_cf_file(dataset_dir / "test.txt", user_mapping, recipe_mapping, test_user_items)
    recipe_json_path = dataset_dir / "菜谱RAW.json"
    _write_recipe_json(recipe_json_path, client, selected_recipes)
    build_filtered_knowledge_graph(
        config,
        recipe_json_path=recipe_json_path,
        output_kg_path=dataset_dir / "kg_final.txt",
    )
    _write_comments_csv(dataset_dir / "merged_with_sentiment.csv", client, user_mapping, recipe_mapping)
    _sync_images(dataset_dir / "images", config, selected_recipes)
    _write_manifest(
        dataset_dir / "dataset_manifest.json",
        filtered_target_users,
        selected_recipes,
        triples,
        exploration_recipe_ids,
    )

    return ExportedDataset(
        dataset_dir=dataset_dir,
        user_mapping=user_mapping,
        recipe_mapping=recipe_mapping,
        recipes=selected_recipes,
        triples=triples,
        exploration_recipe_ids=[recipe_id for recipe_id in exploration_recipe_ids if recipe_id in recipe_mapping],
    )


def select_exploration_recipe_ids(
    client: DatabaseClient,
    excluded_recipe_ids: set[int],
    limit: int,
) -> list[int]:
    if limit <= 0:
        return []
    rows = client.fetch_all(
        """
        SELECT id
        FROM recipes
        WHERE status = 1
        ORDER BY COALESCE(update_time, create_time) DESC, id DESC
        LIMIT %s
        """,
        (max(limit * 5, limit),),
    )
    recipe_ids: list[int] = []
    for row in rows:
        recipe_id = int(row["id"])
        if recipe_id in excluded_recipe_ids:
            continue
        recipe_ids.append(recipe_id)
        if len(recipe_ids) >= limit:
            break
    return recipe_ids


def _split_train_test(
    interactions: list[PositiveInteraction],
) -> tuple[dict[int, list[int]], dict[int, list[int]], set[int]]:
    by_user: dict[int, list[PositiveInteraction]] = {}
    for interaction in interactions:
        by_user.setdefault(interaction.user_id, []).append(interaction)

    train_user_items: dict[int, list[int]] = {}
    test_user_items: dict[int, list[int]] = {}
    filtered_users: set[int] = set()
    for user_id, user_interactions in by_user.items():
        ordered = sorted(
            user_interactions,
            key=lambda item: (
                -(item.event_time.timestamp() if item.event_time else 0),
                -item.weight,
                -item.recipe_id,
            ),
        )
        unique_recipe_ids: list[int] = []
        seen_recipe_ids: set[int] = set()
        for interaction in ordered:
            if interaction.recipe_id in seen_recipe_ids:
                continue
            seen_recipe_ids.add(interaction.recipe_id)
            unique_recipe_ids.append(interaction.recipe_id)
        if len(unique_recipe_ids) < 2:
            continue
        test_user_items[user_id] = [unique_recipe_ids[0]]
        train_user_items[user_id] = unique_recipe_ids[1:]
        filtered_users.add(user_id)
    return train_user_items, test_user_items, filtered_users


def _write_mapping_file(path: Path, mapping: dict[int, int]) -> None:
    lines = [f"{raw_id} {mapped_id}" for raw_id, mapped_id in sorted(mapping.items(), key=lambda item: item[1])]
    path.write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")


def _write_cf_file(
    path: Path,
    user_mapping: dict[int, int],
    recipe_mapping: dict[int, int],
    user_items: dict[int, list[int]],
) -> None:
    lines: list[str] = []
    for user_id, mapped_user_id in sorted(user_mapping.items(), key=lambda item: item[1]):
        recipe_ids = user_items.get(user_id, [])
        if not recipe_ids:
            continue
        mapped_recipe_ids = [str(recipe_mapping[recipe_id]) for recipe_id in recipe_ids if recipe_id in recipe_mapping]
        if not mapped_recipe_ids:
            continue
        recipe_ids_part = " ".join(mapped_recipe_ids)
        lines.append(f"{mapped_user_id} {recipe_ids_part}")
    path.write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")


def _write_recipe_json(path: Path, client: DatabaseClient, recipes: list[RecipeRecord]) -> None:
    recipe_ids = [recipe.recipe_id for recipe in recipes]
    step_rows = _fetch_step_rows(client, recipe_ids)
    steps_by_recipe: dict[int, list[str]] = {}
    for row in step_rows:
        steps_by_recipe.setdefault(int(row["recipe_id"]), []).append(str(row["description"]))

    payload = []
    for recipe in recipes:
        sub_ingredients = [name for name in recipe.ingredient_names if name not in set(recipe.main_ingredient_names)]
        payload.append(
            {
                "id": recipe.recipe_id,
                "title": recipe.title,
                "author": recipe.author,
                "description": recipe.description,
                "tips": recipe.tips,
                "cookware": recipe.cookware,
                "ingredients": {
                    "main_ingredients": {name: "" for name in recipe.main_ingredient_names},
                    "sub_ingredients": {name: "" for name in sub_ingredients},
                    "seasoning_ingredients": {},
                },
                "steps": steps_by_recipe.get(recipe.recipe_id, []),
                "categories": recipe.category_names,
                "properties": {
                    key: value
                    for key, value in {
                        "口味": recipe.taste_name,
                        "工艺": recipe.technique_name,
                        "耗时": recipe.time_cost_name,
                        "难度": recipe.difficulty_name,
                    }.items()
                    if value
                },
                "replynum": str(recipe.reply_count),
                "likenum": recipe.like_count,
                "ratnum": str(recipe.rating_count),
            }
        )
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _write_comments_csv(
    path: Path,
    client: DatabaseClient,
    user_mapping: dict[int, int],
    recipe_mapping: dict[int, int],
) -> None:
    rows = _fetch_comment_rows(client, list(user_mapping.keys()), list(recipe_mapping.keys()))
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as file_handle:
        writer = csv.writer(file_handle)
        writer.writerow(["old_recipe_id", "old_user_id", "mapped_recipe_id", "mapped_user_id", "comment"])
        for row in rows:
            user_id = int(row["user_id"])
            recipe_id = int(row["recipe_id"])
            comment = str(row["content"] or "").strip()
            if user_id not in user_mapping or recipe_id not in recipe_mapping or not comment:
                continue
            writer.writerow(
                [
                    recipe_id,
                    user_id,
                    recipe_mapping[recipe_id],
                    user_mapping[user_id],
                    comment,
                ]
            )


def _sync_images(images_dir: Path, config: AppConfig, recipes: list[RecipeRecord]) -> None:
    images_dir.mkdir(parents=True, exist_ok=True)
    for recipe in recipes:
        source_path = resolve_image_path(config, recipe)
        target_path = images_dir / f"{recipe.recipe_id}.jpg"
        if source_path is None or not source_path.exists():
            continue
        if target_path.exists():
            continue
        try:
            target_path.hardlink_to(source_path)
        except Exception:
            shutil.copy2(source_path, target_path)


def _write_manifest(
    path: Path,
    user_ids: list[int],
    recipes: list[RecipeRecord],
    triples: list[tuple[int, str, str]],
    exploration_recipe_ids: list[int],
) -> None:
    payload = {
        "user_count": len(user_ids),
        "recipe_count": len(recipes),
        "triple_count": len(triples),
        "exploration_recipe_count": len(exploration_recipe_ids),
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _fetch_step_rows(client: DatabaseClient, recipe_ids: list[int]) -> list[dict]:
    if not recipe_ids:
        return []
    rows: list[dict] = []
    for recipe_chunk in _chunked(recipe_ids, 1000):
        placeholders = ", ".join(["%s"] * len(recipe_chunk))
        rows.extend(
            client.fetch_all(
                f"""
                SELECT recipe_id, description
                FROM cooking_steps
                WHERE recipe_id IN ({placeholders})
                ORDER BY recipe_id ASC, step_number ASC
                """,
                tuple(recipe_chunk),
            )
        )
    return rows


def _fetch_comment_rows(client: DatabaseClient, user_ids: list[int], recipe_ids: list[int]) -> list[dict]:
    if not user_ids or not recipe_ids:
        return []
    rows: list[dict] = []
    recipe_filter = sorted(set(recipe_ids))
    for user_chunk in _chunked(sorted(set(user_ids)), 500):
        user_placeholders = ", ".join(["%s"] * len(user_chunk))
        for recipe_chunk in _chunked(recipe_filter, 1000):
            recipe_placeholders = ", ".join(["%s"] * len(recipe_chunk))
            rows.extend(
                client.fetch_all(
                    f"""
                    SELECT user_id, recipe_id, content
                    FROM comments
                    WHERE user_id IN ({user_placeholders})
                      AND recipe_id IN ({recipe_placeholders})
                      AND content IS NOT NULL
                      AND content <> ''
                    ORDER BY COALESCE(publish_time, create_time) DESC, id DESC
                    """,
                    tuple(user_chunk + recipe_chunk),
                )
            )
    return rows


def _chunked(values: Iterable[int], size: int) -> Iterable[list[int]]:
    batch: list[int] = []
    for value in values:
        batch.append(int(value))
        if len(batch) >= size:
            yield batch
            batch = []
    if batch:
        yield batch
