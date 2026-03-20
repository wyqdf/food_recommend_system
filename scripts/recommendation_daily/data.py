from __future__ import annotations

import json
import math
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Iterable

from .config import AppConfig
from .db import DatabaseClient


@dataclass
class RecipeRecord:
    recipe_id: int
    old_id: int | None
    title: str
    author: str
    description: str
    tips: str
    cookware: str
    image: str | None
    category_names: list[str]
    ingredient_names: list[str]
    main_ingredient_names: list[str]
    taste_name: str
    technique_name: str
    time_cost_name: str
    difficulty_name: str
    reply_count: int
    like_count: int
    rating_count: int
    create_time: datetime | None
    update_time: datetime | None

    @property
    def text_input(self) -> str:
        parts = [
            self.title,
            self.description,
            self.tips,
            " ".join(self.ingredient_names),
            " ".join(self.category_names),
            self.taste_name,
            self.technique_name,
            self.time_cost_name,
            self.difficulty_name,
        ]
        return " ".join(part.strip() for part in parts if part and part.strip())


@dataclass
class UserProfileRecord:
    user_id: int
    taboo_ingredients: list[str]
    available_cookwares: list[str]


@dataclass
class PositiveInteraction:
    user_id: int
    recipe_id: int
    weight: float
    event_time: datetime
    source: str


@dataclass
class SnapshotBundle:
    recipes: list[RecipeRecord]
    triples: list[tuple[int, str, str]]


def _chunked(values: list[int], chunk_size: int = 1000) -> Iterable[list[int]]:
    for start in range(0, len(values), chunk_size):
        yield values[start:start + chunk_size]


def ensure_runtime_dirs(config: AppConfig) -> None:
    for directory in (
        config.paths.runtime_root,
        config.paths.datasets_dir,
        config.paths.checkpoints_dir,
        config.paths.embeddings_dir,
        config.paths.manifests_dir,
        config.paths.snapshots_dir,
        config.paths.logs_dir,
    ):
        directory.mkdir(parents=True, exist_ok=True)


def _load_mapping(path: Path) -> dict[str, int]:
    if not path.exists():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def _save_mapping(path: Path, mapping: dict[str, int]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(mapping, ensure_ascii=False, indent=2), encoding="utf-8")


def update_stable_mapping(path: Path, raw_ids: list[int]) -> dict[str, int]:
    mapping = _load_mapping(path)
    next_id = max(mapping.values(), default=-1) + 1
    for raw_id in sorted(set(raw_ids)):
        key = str(raw_id)
        if key not in mapping:
            mapping[key] = next_id
            next_id += 1
    _save_mapping(path, mapping)
    return mapping


def fetch_baseline_user_ids(client: DatabaseClient, config: AppConfig) -> list[int]:
    rows = client.fetch_all(
        f"""
        SELECT user_id
        FROM (
            SELECT user_id, COUNT(*) AS total_count
            FROM (
                SELECT user_id
                FROM behavior_events
                WHERE user_id IS NOT NULL
                  AND event_type IN ('favorite_add', 'cooking_finish', 'cooking_start', 'recipe_click', 'recipe_view')
                UNION ALL
                SELECT user_id
                FROM interactions
                WHERE user_id IS NOT NULL
                  AND interaction_type = 'favorite'
                UNION ALL
                SELECT user_id
                FROM comments
                WHERE user_id IS NOT NULL
            ) merged
            GROUP BY user_id
        ) ranked
        WHERE total_count >= {config.daily.user_total_threshold}
        ORDER BY user_id ASC
        """
    )
    return [int(row["user_id"]) for row in rows if row.get("user_id") is not None]


def fetch_incremental_user_ids(client: DatabaseClient, config: AppConfig) -> list[int]:
    last_success = client.latest_successful_finished_at()
    params: list[Any] = [config.daily.user_lookback_days, config.daily.user_lookback_days, config.daily.user_lookback_days]
    selects = [
        f"""
        SELECT user_id
        FROM (
            SELECT user_id, COUNT(*) AS total_count
            FROM (
                SELECT user_id
                FROM behavior_events
                WHERE user_id IS NOT NULL
                  AND event_type IN ('favorite_add', 'cooking_finish', 'cooking_start', 'recipe_click', 'recipe_view')
                  AND create_time >= DATE_SUB(NOW(), INTERVAL %s DAY)
                UNION ALL
                SELECT user_id
                FROM interactions
                WHERE user_id IS NOT NULL
                  AND interaction_type = 'favorite'
                  AND create_time >= DATE_SUB(NOW(), INTERVAL %s DAY)
                UNION ALL
                SELECT user_id
                FROM comments
                WHERE user_id IS NOT NULL
                  AND COALESCE(publish_time, create_time) >= DATE_SUB(NOW(), INTERVAL %s DAY)
            ) recent_sources
            GROUP BY user_id
        ) recent_users
        WHERE total_count >= {config.daily.user_total_threshold}
        """
    ]
    if last_success is not None:
        params.extend([last_success, last_success, last_success])
        selects.append(
            f"""
            SELECT user_id
            FROM (
                SELECT user_id, COUNT(*) AS delta_count
                FROM (
                    SELECT user_id
                    FROM behavior_events
                    WHERE user_id IS NOT NULL
                      AND event_type IN ('favorite_add', 'cooking_finish', 'cooking_start', 'recipe_click', 'recipe_view')
                      AND create_time > %s
                    UNION ALL
                    SELECT user_id
                    FROM interactions
                    WHERE user_id IS NOT NULL
                      AND interaction_type = 'favorite'
                      AND create_time > %s
                    UNION ALL
                    SELECT user_id
                    FROM comments
                    WHERE user_id IS NOT NULL
                      AND COALESCE(publish_time, create_time) > %s
                ) delta_sources
                GROUP BY user_id
            ) delta_users
            WHERE delta_count >= {config.daily.user_incremental_threshold}
            """
        )
    rows = client.fetch_all(
        f"""
        SELECT DISTINCT user_id
        FROM (
            {' UNION DISTINCT '.join(selects)}
        ) candidate_users
        ORDER BY user_id ASC
        """,
        tuple(params),
    )
    return [int(row["user_id"]) for row in rows if row.get("user_id") is not None]


def fetch_baseline_recipe_ids(client: DatabaseClient, config: AppConfig) -> list[int]:
    rows = client.fetch_all(
        f"""
        SELECT recipe_id
        FROM (
            SELECT recipe_id, COUNT(*) AS total_count
            FROM (
                SELECT recipe_id
                FROM behavior_events
                WHERE recipe_id IS NOT NULL
                  AND event_type IN ('favorite_add', 'cooking_finish', 'cooking_start', 'recipe_click', 'recipe_view')
                UNION ALL
                SELECT recipe_id
                FROM interactions
                WHERE recipe_id IS NOT NULL
                  AND interaction_type = 'favorite'
                UNION ALL
                SELECT recipe_id
                FROM comments
                WHERE recipe_id IS NOT NULL
            ) merged
            GROUP BY recipe_id
        ) ranked
        WHERE total_count >= {config.daily.recipe_incremental_threshold}
        ORDER BY recipe_id ASC
        """
    )
    return [int(row["recipe_id"]) for row in rows if row.get("recipe_id") is not None]


def fetch_incremental_recipe_ids(client: DatabaseClient, config: AppConfig) -> list[int]:
    last_success = client.latest_successful_finished_at()
    params: list[Any] = []
    behavior_time_filter = ""
    interaction_time_filter = ""
    comment_time_filter = ""
    if last_success is not None:
        behavior_time_filter = "AND create_time > %s"
        interaction_time_filter = "AND create_time > %s"
        comment_time_filter = "AND COALESCE(publish_time, create_time) > %s"
        params.extend([last_success, last_success, last_success])
    rows = client.fetch_all(
        f"""
        SELECT DISTINCT recipe_id
        FROM (
            SELECT id AS recipe_id
            FROM recipes
            WHERE status = 1
              AND (create_time >= CURDATE() OR update_time >= CURDATE())
            UNION DISTINCT
            SELECT recipe_id
            FROM (
                SELECT recipe_id, COUNT(*) AS event_count
                FROM (
                    SELECT recipe_id
                    FROM behavior_events
                    WHERE recipe_id IS NOT NULL
                      AND event_type IN ('favorite_add', 'cooking_finish', 'cooking_start', 'recipe_click', 'recipe_view')
                      {behavior_time_filter}
                    UNION ALL
                    SELECT recipe_id
                    FROM interactions
                    WHERE recipe_id IS NOT NULL
                      AND interaction_type = 'favorite'
                      {interaction_time_filter}
                    UNION ALL
                    SELECT recipe_id
                    FROM comments
                    WHERE recipe_id IS NOT NULL
                      {comment_time_filter}
                ) delta_sources
                GROUP BY recipe_id
            ) delta_recipes
            WHERE event_count >= {config.daily.recipe_incremental_threshold}
        ) merged
        """,
        tuple(params),
    )
    return [int(row["recipe_id"]) for row in rows if row.get("recipe_id") is not None]


def fetch_inactive_recipe_ids(client: DatabaseClient, recipe_ids: list[int]) -> list[int]:
    if not recipe_ids:
        return []
    inactive: list[int] = []
    for id_chunk in _chunked(sorted(set(recipe_ids)), chunk_size=2000):
        placeholders = ", ".join(["%s"] * len(id_chunk))
        rows = client.fetch_all(
            f"""
            SELECT id
            FROM recipes
            WHERE id IN ({placeholders})
              AND status <> 1
            """,
            tuple(id_chunk),
        )
        inactive.extend(int(row["id"]) for row in rows if row.get("id") is not None)
    return sorted(set(inactive))


def fetch_recipe_records(client: DatabaseClient, recipe_ids: list[int] | None = None) -> list[RecipeRecord]:
    def fetch_recipe_chunk(id_chunk: list[int] | None) -> list[dict[str, Any]]:
        filter_sql = ""
        params: list[Any] = []
        if id_chunk:
            placeholders = ", ".join(["%s"] * len(id_chunk))
            filter_sql = f"AND r.id IN ({placeholders})"
            params.extend(id_chunk)
        return client.fetch_all(
            f"""
            SELECT
                r.id AS recipe_id,
                r.old_id,
                r.title,
                COALESCE(r.author, '') AS author,
                COALESCE(r.description, '') AS description,
                COALESCE(r.tips, '') AS tips,
                COALESCE(r.cookware, '') AS cookware,
                r.image,
                COALESCE(r.reply_count, 0) AS reply_count,
                COALESCE(r.like_count, 0) AS like_count,
                COALESCE(r.rating_count, 0) AS rating_count,
                r.create_time,
                r.update_time,
                COALESCE(ta.name, '') AS taste_name,
                COALESCE(tech.name, '') AS technique_name,
                COALESCE(tc.name, '') AS time_cost_name,
                COALESCE(d.name, '') AS difficulty_name
            FROM recipes r
            LEFT JOIN tastes ta ON ta.id = r.taste_id
            LEFT JOIN techniques tech ON tech.id = r.technique_id
            LEFT JOIN time_costs tc ON tc.id = r.time_cost_id
            LEFT JOIN difficulties d ON d.id = r.difficulty_id
            WHERE r.status = 1
            {filter_sql}
            ORDER BY r.id ASC
            """,
            tuple(params),
        )

    if recipe_ids:
        rows: list[dict[str, Any]] = []
        for id_chunk in _chunked(sorted(set(recipe_ids)), chunk_size=2000):
            rows.extend(fetch_recipe_chunk(id_chunk))
    else:
        rows = fetch_recipe_chunk(None)
    if not rows:
        return []
    ids = [int(row["recipe_id"]) for row in rows]
    category_rows: list[dict[str, Any]] = []
    ingredient_rows: list[dict[str, Any]] = []
    for id_chunk in _chunked(ids, chunk_size=2000):
        placeholders = ", ".join(["%s"] * len(id_chunk))
        category_rows.extend(
            client.fetch_all(
                f"""
                SELECT rc.recipe_id, c.name
                FROM recipe_categories rc
                INNER JOIN categories c ON c.id = rc.category_id
                WHERE rc.recipe_id IN ({placeholders})
                ORDER BY rc.recipe_id ASC, c.name ASC
                """,
                tuple(id_chunk),
            )
        )
        ingredient_rows.extend(
            client.fetch_all(
                f"""
                SELECT ri.recipe_id, i.name, ri.ingredient_type
                FROM recipe_ingredients ri
                INNER JOIN ingredients i ON i.id = ri.ingredient_id
                WHERE ri.recipe_id IN ({placeholders})
                ORDER BY ri.recipe_id ASC, i.name ASC
                """,
                tuple(id_chunk),
            )
        )
    categories_by_recipe: dict[int, list[str]] = {}
    for row in category_rows:
        categories_by_recipe.setdefault(int(row["recipe_id"]), []).append(str(row["name"]))
    ingredients_by_recipe: dict[int, list[str]] = {}
    main_ingredients_by_recipe: dict[int, list[str]] = {}
    for row in ingredient_rows:
        recipe_id = int(row["recipe_id"])
        ingredient_name = str(row["name"])
        ingredients_by_recipe.setdefault(recipe_id, []).append(ingredient_name)
        if row["ingredient_type"] == "main":
            main_ingredients_by_recipe.setdefault(recipe_id, []).append(ingredient_name)
    records: list[RecipeRecord] = []
    for row in rows:
        recipe_id = int(row["recipe_id"])
        records.append(
            RecipeRecord(
                recipe_id=recipe_id,
                old_id=row["old_id"],
                title=str(row["title"]),
                author=str(row["author"]),
                description=str(row["description"]),
                tips=str(row["tips"]),
                cookware=str(row["cookware"]),
                image=row["image"],
                category_names=categories_by_recipe.get(recipe_id, []),
                ingredient_names=ingredients_by_recipe.get(recipe_id, []),
                main_ingredient_names=main_ingredients_by_recipe.get(recipe_id, []),
                taste_name=str(row["taste_name"]),
                technique_name=str(row["technique_name"]),
                time_cost_name=str(row["time_cost_name"]),
                difficulty_name=str(row["difficulty_name"]),
                reply_count=int(row["reply_count"] or 0),
                like_count=int(row["like_count"] or 0),
                rating_count=int(row["rating_count"] or 0),
                create_time=row.get("create_time"),
                update_time=row.get("update_time"),
            )
        )
    return records


def load_snapshot_bundle(config: AppConfig) -> SnapshotBundle:
    recipes_path = config.paths.snapshots_dir / "recipes.json"
    triples_path = config.paths.snapshots_dir / "kg_triples.json"
    recipes: list[RecipeRecord] = []
    triples: list[tuple[int, str, str]] = []
    if recipes_path.exists():
        raw_recipes = json.loads(recipes_path.read_text(encoding="utf-8"))
        recipes = [RecipeRecord(**row) for row in raw_recipes]
    if triples_path.exists():
        raw_triples = json.loads(triples_path.read_text(encoding="utf-8"))
        triples = [(int(recipe_id), str(relation), str(tail)) for recipe_id, relation, tail in raw_triples]
    return SnapshotBundle(recipes=recipes, triples=triples)


def merge_recipe_snapshot(
    existing_recipes: list[RecipeRecord],
    updated_recipes: list[RecipeRecord],
    removed_recipe_ids: list[int],
) -> list[RecipeRecord]:
    merged = {recipe.recipe_id: recipe for recipe in existing_recipes}
    for recipe_id in removed_recipe_ids:
        merged.pop(recipe_id, None)
    for recipe in updated_recipes:
        merged[recipe.recipe_id] = recipe
    return [merged[recipe_id] for recipe_id in sorted(merged)]


def merge_triple_snapshot(
    existing_triples: list[tuple[int, str, str]],
    updated_recipes: list[RecipeRecord],
    removed_recipe_ids: list[int],
) -> list[tuple[int, str, str]]:
    affected_recipe_ids = set(removed_recipe_ids) | {recipe.recipe_id for recipe in updated_recipes}
    retained = [triple for triple in existing_triples if triple[0] not in affected_recipe_ids]
    retained.extend(build_recipe_triples(updated_recipes))
    retained.sort(key=lambda item: (item[0], item[1], item[2]))
    return retained


def fetch_user_profiles(client: DatabaseClient, user_ids: list[int]) -> dict[int, UserProfileRecord]:
    if not user_ids:
        return {}
    profiles: dict[int, UserProfileRecord] = {}
    for user_chunk in _chunked(sorted(set(user_ids)), chunk_size=2000):
        placeholders = ", ".join(["%s"] * len(user_chunk))
        rows = client.fetch_all(
            f"""
            SELECT user_id, taboo_ingredients_json, available_cookwares_json
            FROM user_preference_profiles
            WHERE user_id IN ({placeholders})
            """,
            tuple(user_chunk),
        )
        for row in rows:
            profiles[int(row["user_id"])] = UserProfileRecord(
                user_id=int(row["user_id"]),
                taboo_ingredients=_parse_json_list(row["taboo_ingredients_json"]),
                available_cookwares=_parse_json_list(row["available_cookwares_json"]),
            )
    return profiles


def map_external_user_ids(client: DatabaseClient, old_user_ids: list[int]) -> dict[int, int]:
    if not old_user_ids:
        return {}
    mapped: dict[int, int] = {}
    for id_chunk in _chunked(sorted(set(old_user_ids)), chunk_size=2000):
        placeholders = ", ".join(["%s"] * len(id_chunk))
        rows = client.fetch_all(
            f"""
            SELECT old_user_id, new_user_id
            FROM user_id_mapping
            WHERE old_user_id IN ({placeholders})
            """,
            tuple(id_chunk),
        )
        for row in rows:
            mapped[int(row["old_user_id"])] = int(row["new_user_id"])
    return mapped


def map_external_recipe_ids(client: DatabaseClient, old_recipe_ids: list[int]) -> dict[int, int]:
    if not old_recipe_ids:
        return {}
    mapped: dict[int, int] = {}
    for id_chunk in _chunked(sorted(set(old_recipe_ids)), chunk_size=2000):
        placeholders = ", ".join(["%s"] * len(id_chunk))
        rows = client.fetch_all(
            f"""
            SELECT old_recipe_id, new_recipe_id
            FROM recipe_id_mapping
            WHERE old_recipe_id IN ({placeholders})
            """,
            tuple(id_chunk),
        )
        for row in rows:
            mapped[int(row["old_recipe_id"])] = int(row["new_recipe_id"])
    return mapped


def fetch_positive_interactions(client: DatabaseClient, user_ids: list[int]) -> list[PositiveInteraction]:
    if not user_ids:
        return []
    result: list[PositiveInteraction] = []
    max_pairs = client._config.training.max_user_positive_pairs
    for user_chunk in _chunked(sorted(set(user_ids)), chunk_size=1000):
        placeholders = ", ".join(["%s"] * len(user_chunk))
        rows = client.fetch_all(
            f"""
            SELECT user_id, recipe_id, weight, event_time
            FROM (
                SELECT
                    aggregated.user_id,
                    aggregated.recipe_id,
                    aggregated.weight,
                    aggregated.event_time,
                    ROW_NUMBER() OVER (
                        PARTITION BY aggregated.user_id
                        ORDER BY aggregated.weight DESC, aggregated.event_time DESC, aggregated.recipe_id DESC
                    ) AS rn
                FROM (
                    SELECT
                        merged.user_id,
                        merged.recipe_id,
                        MAX(merged.weight) AS weight,
                        MAX(merged.event_time) AS event_time
                    FROM (
                        SELECT user_id, recipe_id, 4.0 AS weight, create_time AS event_time
                        FROM behavior_events
                        WHERE user_id IN ({placeholders})
                          AND recipe_id IS NOT NULL
                          AND event_type IN ('favorite_add', 'cooking_finish')
                        UNION ALL
                        SELECT user_id, recipe_id, 3.0 AS weight, COALESCE(publish_time, create_time) AS event_time
                        FROM comments
                        WHERE user_id IN ({placeholders})
                          AND recipe_id IS NOT NULL
                        UNION ALL
                        SELECT user_id, recipe_id, 2.0 AS weight, create_time AS event_time
                        FROM behavior_events
                        WHERE user_id IN ({placeholders})
                          AND recipe_id IS NOT NULL
                          AND event_type IN ('cooking_start')
                        UNION ALL
                        SELECT user_id, recipe_id, 1.0 AS weight, create_time AS event_time
                        FROM behavior_events
                        WHERE user_id IN ({placeholders})
                          AND recipe_id IS NOT NULL
                          AND event_type IN ('recipe_click')
                        UNION ALL
                        SELECT user_id, recipe_id, 0.6 AS weight, create_time AS event_time
                        FROM behavior_events
                        WHERE user_id IN ({placeholders})
                          AND recipe_id IS NOT NULL
                          AND event_type IN ('recipe_view')
                        UNION ALL
                        SELECT user_id, recipe_id, 4.0 AS weight, create_time AS event_time
                        FROM interactions
                        WHERE user_id IN ({placeholders})
                          AND recipe_id IS NOT NULL
                          AND interaction_type = 'favorite'
                    ) merged
                    GROUP BY merged.user_id, merged.recipe_id
                ) aggregated
            ) ranked
            WHERE rn <= %s
            ORDER BY user_id ASC, weight DESC, event_time DESC, recipe_id DESC
            """,
            tuple(user_chunk * 6 + [max_pairs]),
        )
        for row in rows:
            result.append(
                PositiveInteraction(
                    user_id=int(row["user_id"]),
                    recipe_id=int(row["recipe_id"]),
                    weight=float(row["weight"]),
                    event_time=row["event_time"],
                    source="aggregated",
                )
            )
    result.sort(key=lambda item: (item.user_id, -item.weight, item.event_time.timestamp() if item.event_time else 0))
    return result


def fetch_all_positive_interactions(client: DatabaseClient, user_ids: list[int]) -> list[PositiveInteraction]:
    if not user_ids:
        return []
    result: list[PositiveInteraction] = []
    for user_chunk in _chunked(sorted(set(user_ids)), chunk_size=1000):
        placeholders = ", ".join(["%s"] * len(user_chunk))
        rows = client.fetch_all(
            f"""
            SELECT
                merged.user_id,
                merged.recipe_id,
                MAX(merged.weight) AS weight,
                MAX(merged.event_time) AS event_time
            FROM (
                SELECT user_id, recipe_id, 4.0 AS weight, create_time AS event_time
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type IN ('favorite_add', 'cooking_finish')
                UNION ALL
                SELECT user_id, recipe_id, 3.0 AS weight, COALESCE(publish_time, create_time) AS event_time
                FROM comments
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                UNION ALL
                SELECT user_id, recipe_id, 2.0 AS weight, create_time AS event_time
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type = 'cooking_start'
                UNION ALL
                SELECT user_id, recipe_id, 1.0 AS weight, create_time AS event_time
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type = 'recipe_click'
                UNION ALL
                SELECT user_id, recipe_id, 0.6 AS weight, create_time AS event_time
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type = 'recipe_view'
                UNION ALL
                SELECT user_id, recipe_id, 4.0 AS weight, create_time AS event_time
                FROM interactions
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND interaction_type = 'favorite'
            ) merged
            GROUP BY merged.user_id, merged.recipe_id
            ORDER BY merged.user_id ASC, event_time DESC, weight DESC, merged.recipe_id DESC
            """,
            tuple(user_chunk * 6),
        )
        for row in rows:
            result.append(
                PositiveInteraction(
                    user_id=int(row["user_id"]),
                    recipe_id=int(row["recipe_id"]),
                    weight=float(row["weight"]),
                    event_time=row["event_time"],
                    source="aggregated_full",
                )
            )
    result.sort(
        key=lambda item: (
            item.user_id,
            -(item.event_time.timestamp() if item.event_time else 0),
            -item.weight,
            -item.recipe_id,
        )
    )
    return result


def fetch_negative_overrides(client: DatabaseClient, user_ids: list[int]) -> set[tuple[int, int]]:
    if not user_ids:
        return set()
    pairs: set[tuple[int, int]] = set()
    for user_chunk in _chunked(sorted(set(user_ids)), chunk_size=1000):
        placeholders = ", ".join(["%s"] * len(user_chunk))
        rows = client.fetch_all(
            f"""
            SELECT user_id, recipe_id
            FROM (
                SELECT
                    user_id,
                    recipe_id,
                    event_type,
                    ROW_NUMBER() OVER (PARTITION BY user_id, recipe_id ORDER BY create_time DESC, id DESC) AS rn
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type IN ('favorite_add', 'favorite_remove')
            ) ranked
            WHERE rn = 1
              AND event_type = 'favorite_remove'
            """,
            tuple(user_chunk),
        )
        pairs.update((int(row["user_id"]), int(row["recipe_id"])) for row in rows)
    return pairs


def fetch_non_favorite_strengths(client: DatabaseClient, user_ids: list[int]) -> dict[tuple[int, int], float]:
    if not user_ids:
        return {}
    strengths: dict[tuple[int, int], float] = {}
    for user_chunk in _chunked(sorted(set(user_ids)), chunk_size=1000):
        placeholders = ", ".join(["%s"] * len(user_chunk))
        rows = client.fetch_all(
            f"""
            SELECT user_id, recipe_id, MAX(weight) AS weight
            FROM (
                SELECT user_id, recipe_id, 4.0 AS weight
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type = 'cooking_finish'
                UNION ALL
                SELECT user_id, recipe_id, 3.0 AS weight
                FROM comments
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                UNION ALL
                SELECT user_id, recipe_id, 2.0 AS weight
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type = 'cooking_start'
                UNION ALL
                SELECT user_id, recipe_id, 1.0 AS weight
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type = 'recipe_click'
                UNION ALL
                SELECT user_id, recipe_id, 0.6 AS weight
                FROM behavior_events
                WHERE user_id IN ({placeholders})
                  AND recipe_id IS NOT NULL
                  AND event_type = 'recipe_view'
            ) non_favorite_sources
            GROUP BY user_id, recipe_id
            """,
            tuple(user_chunk * 5),
        )
        for row in rows:
            strengths[(int(row["user_id"]), int(row["recipe_id"]))] = float(row["weight"])
    return strengths


def fetch_recent_positive_seeds(client: DatabaseClient, user_ids: list[int]) -> dict[int, list[int]]:
    if not user_ids:
        return {}
    seeds: dict[int, list[int]] = {}
    for user_chunk in _chunked(sorted(set(user_ids)), chunk_size=1000):
        placeholders = ", ".join(["%s"] * len(user_chunk))
        rows = client.fetch_all(
            f"""
            SELECT user_id, recipe_id
            FROM (
                SELECT
                    user_id,
                    recipe_id,
                    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY event_time DESC, weight DESC, recipe_id DESC) AS rn
                FROM (
                    SELECT user_id, recipe_id, create_time AS event_time, 4.0 AS weight
                    FROM behavior_events
                    WHERE user_id IN ({placeholders})
                      AND recipe_id IS NOT NULL
                      AND event_type IN ('favorite_add', 'cooking_finish')
                    UNION ALL
                    SELECT user_id, recipe_id, COALESCE(publish_time, create_time) AS event_time, 3.0 AS weight
                    FROM comments
                    WHERE user_id IN ({placeholders})
                      AND recipe_id IS NOT NULL
                    UNION ALL
                    SELECT user_id, recipe_id, create_time AS event_time, 4.0 AS weight
                    FROM interactions
                    WHERE user_id IN ({placeholders})
                      AND recipe_id IS NOT NULL
                      AND interaction_type = 'favorite'
                    UNION ALL
                    SELECT user_id, recipe_id, create_time AS event_time, 2.0 AS weight
                    FROM behavior_events
                    WHERE user_id IN ({placeholders})
                      AND recipe_id IS NOT NULL
                      AND event_type = 'cooking_start'
                    UNION ALL
                    SELECT user_id, recipe_id, create_time AS event_time, 1.0 AS weight
                    FROM behavior_events
                    WHERE user_id IN ({placeholders})
                      AND recipe_id IS NOT NULL
                      AND event_type = 'recipe_click'
                    UNION ALL
                    SELECT user_id, recipe_id, create_time AS event_time, 0.6 AS weight
                    FROM behavior_events
                    WHERE user_id IN ({placeholders})
                      AND recipe_id IS NOT NULL
                      AND event_type = 'recipe_view'
                ) seed_events
            ) ranked
            WHERE rn <= 12
            ORDER BY user_id ASC, rn ASC
            """,
            tuple(user_chunk * 6),
        )
        for row in rows:
            seeds.setdefault(int(row["user_id"]), []).append(int(row["recipe_id"]))
    return seeds


def build_dynamic_interest_sets(interactions: list[PositiveInteraction]) -> dict[int, list[PositiveInteraction]]:
    by_user: dict[int, list[PositiveInteraction]] = {}
    for interaction in interactions:
        by_user.setdefault(interaction.user_id, []).append(interaction)

    dynamic_sets: dict[int, list[PositiveInteraction]] = {}
    for user_id, items in by_user.items():
        ordered = sorted(
            items,
            key=lambda item: (
                -(item.event_time.timestamp() if item.event_time else 0),
                -item.weight,
                -item.recipe_id,
            ),
        )
        take_count = max(1, math.ceil(len(ordered) * 0.3))
        dynamic_sets[user_id] = ordered[:take_count]
    return dynamic_sets


def fetch_recent_pushed_recipe_ids(client: DatabaseClient, user_ids: list[int], days: int) -> dict[int, set[int]]:
    if not user_ids or days <= 0:
        return {}
    pushed: dict[int, set[int]] = {}
    for user_chunk in _chunked(sorted(set(user_ids)), chunk_size=1000):
        placeholders = ", ".join(["%s"] * len(user_chunk))
        rows = client.fetch_all(
            f"""
            SELECT user_id, recipe_id
            FROM daily_recipe_recommendations
            WHERE user_id IN ({placeholders})
              AND selected_for_delivery = 1
              AND biz_date >= DATE_SUB(CURDATE(), INTERVAL %s DAY)
            """,
            tuple(user_chunk + [days]),
        )
        for row in rows:
            pushed.setdefault(int(row["user_id"]), set()).add(int(row["recipe_id"]))
    return pushed


def fetch_recent_cooked_recipe_ids(client: DatabaseClient, user_ids: list[int], days: int) -> dict[int, set[int]]:
    if not user_ids or days <= 0:
        return {}
    cooked: dict[int, set[int]] = {}
    for user_chunk in _chunked(sorted(set(user_ids)), chunk_size=1000):
        placeholders = ", ".join(["%s"] * len(user_chunk))
        rows = client.fetch_all(
            f"""
            SELECT user_id, recipe_id
            FROM behavior_events
            WHERE user_id IN ({placeholders})
              AND recipe_id IS NOT NULL
              AND event_type = 'cooking_finish'
              AND create_time >= DATE_SUB(NOW(), INTERVAL %s DAY)
            """,
            tuple(user_chunk + [days]),
        )
        for row in rows:
            cooked.setdefault(int(row["user_id"]), set()).add(int(row["recipe_id"]))
    return cooked


def write_snapshot_files(
    config: AppConfig,
    recipes: list[RecipeRecord],
    interactions: list[PositiveInteraction],
    recipe_mapping: dict[str, int],
    user_mapping: dict[str, int],
    triples: list[tuple[int, str, str]],
) -> None:
    snapshot_dir = config.paths.snapshots_dir
    snapshot_dir.mkdir(parents=True, exist_ok=True)
    serialized_recipes = []
    for recipe in recipes:
        payload = dict(recipe.__dict__)
        if recipe.create_time is not None:
            payload["create_time"] = recipe.create_time.isoformat()
        if recipe.update_time is not None:
            payload["update_time"] = recipe.update_time.isoformat()
        serialized_recipes.append(payload)
    (snapshot_dir / "recipes.json").write_text(
        json.dumps(serialized_recipes, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    (snapshot_dir / "interactions.json").write_text(
        json.dumps([interaction.__dict__ for interaction in interactions], ensure_ascii=False, default=str, indent=2),
        encoding="utf-8",
    )
    (snapshot_dir / "recipe_mapping.json").write_text(json.dumps(recipe_mapping, ensure_ascii=False, indent=2), encoding="utf-8")
    (snapshot_dir / "user_mapping.json").write_text(json.dumps(user_mapping, ensure_ascii=False, indent=2), encoding="utf-8")
    (snapshot_dir / "kg_triples.json").write_text(json.dumps(triples, ensure_ascii=False, indent=2), encoding="utf-8")


def build_recipe_triples(recipes: list[RecipeRecord]) -> list[tuple[int, str, str]]:
    triples: list[tuple[int, str, str]] = []
    for recipe in recipes:
        for category in recipe.category_names:
            triples.append((recipe.recipe_id, "category", category))
        for ingredient in recipe.ingredient_names:
            triples.append((recipe.recipe_id, "ingredient", ingredient))
        if recipe.taste_name:
            triples.append((recipe.recipe_id, "taste", recipe.taste_name))
        if recipe.technique_name:
            triples.append((recipe.recipe_id, "technique", recipe.technique_name))
        if recipe.time_cost_name:
            triples.append((recipe.recipe_id, "time_cost", recipe.time_cost_name))
        if recipe.difficulty_name:
            triples.append((recipe.recipe_id, "difficulty", recipe.difficulty_name))
        if recipe.cookware:
            triples.append((recipe.recipe_id, "cookware", recipe.cookware))
    return triples


def resolve_image_path(config: AppConfig, recipe: RecipeRecord) -> Path | None:
    if recipe.old_id:
        candidate = config.paths.image_root / f"{recipe.old_id}.jpg"
        if candidate.exists():
            return candidate
    if recipe.image:
        image_path = Path(recipe.image)
        if image_path.exists():
            return image_path
    return None


def _parse_json_list(raw: Any) -> list[str]:
    if not raw:
        return []
    if isinstance(raw, list):
        return [str(item).strip() for item in raw if str(item).strip()]
    try:
        data = json.loads(raw)
        if isinstance(data, list):
            return [str(item).strip() for item in data if str(item).strip()]
    except Exception:
        return []
    return []
