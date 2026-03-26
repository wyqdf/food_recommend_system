from __future__ import annotations

import os
import re
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import parse_qs, urlparse


PLACEHOLDER_PATTERN = re.compile(r"\$\{([^:}]+):?([^}]*)}")


@dataclass(frozen=True)
class DatabaseSettings:
    host: str
    port: int
    database: str
    username: str
    password: str
    charset: str = "utf8mb4"


@dataclass(frozen=True)
class PathSettings:
    project_root: Path
    backend_properties: Path
    top100_bundle_root: Path
    runtime_root: Path
    datasets_dir: Path
    checkpoints_dir: Path
    embeddings_dir: Path
    manifests_dir: Path
    snapshots_dir: Path
    logs_dir: Path
    image_root: Path
    external_cke_root: Path
    knowledge_graph_root: Path


@dataclass(frozen=True)
class ModelSettings:
    backbone_name: str = "jinaai/jina-clip-v2"
    hidden_dim: int = 256
    feature_dim: int = 1024
    feature_version: str = "jina-clip-v2-static-v1"
    adapter_checkpoint_name: str = "jina_clip_adapter.safetensors"
    finetune_encoder_for_baseline: bool = True
    max_text_length: int = 256
    extract_batch_size: int = 2
    use_bfloat16: bool = True


@dataclass(frozen=True)
class TrainingSettings:
    batch_size: int = 64
    eval_batch_size: int = 128
    baseline_online_batch_size: int = 2
    baseline_export_batch_size: int = 8
    baseline_epochs: int = 1
    incremental_epochs: int = 1
    learning_rate: float = 2e-4
    encoder_learning_rate: float = 5e-5
    weight_decay: float = 1e-4
    replay_buffer_users: int = 256
    replay_buffer_recipes: int = 512
    interaction_negatives: int = 2
    kg_negatives: int = 1
    kg_loss_weight: float = 0.2
    max_user_positive_pairs: int = 4
    max_kg_triples: int = 20000
    ranking_user_batch_size: int = 128


@dataclass(frozen=True)
class DailySettings:
    strong_events: tuple[str, ...] = ("favorite_add", "cooking_finish")
    medium_events: tuple[str, ...] = ("cooking_start",)
    weak_events: tuple[str, ...] = ("recipe_click", "recipe_view")
    ignored_events: tuple[str, ...] = (
        "page_view",
        "search_submit",
        "search_history_click",
        "search_suggestion_click",
        "search_sort_change",
        "cooking_mode_leave",
    )
    user_lookback_days: int = 30
    user_total_threshold: int = 8
    user_incremental_threshold: int = 5
    recipe_incremental_threshold: int = 3
    cke_candidate_topk: int = 100
    recent_recall_topk: int = 30
    topk_after_diversity: int = 100
    final_delivery_count: int = 16
    max_same_category: int = 4
    max_same_main_ingredient: int = 4
    pushed_cooldown_days: int = 3
    cooked_cooldown_days: int = 7
    pushed_cooldown_multiplier: float = 0.35
    cooked_cooldown_multiplier: float = 0.50
    timezone: str = "Asia/Shanghai"
    model_version: str = "cke-full-two-stage-v1"


@dataclass(frozen=True)
class CkeFullSettings:
    dataset_name: str = "daily_local"
    existing_dataset_name: str = "meishitianxia_v1"
    training_model_family: str = "CKEFullModify"
    existing_model_family: str = "CKEFull"
    embed_dim: int = 64
    relation_dim: int = 32
    learning_rate: float = 1e-4
    epochs: int = 30
    evaluate_every: int = 5
    stopping_steps: int = 8
    cf_batch_size: int = 256
    kg_batch_size: int = 256
    sdae_batch_size: int = 256
    scae_batch_size: int = 32
    image_height: int = 64
    image_width: int = 64
    test_batch_size: int = 128
    print_every: int = 50
    review_align_weight: float = 0.5
    model_topk_count: int = 90
    exploration_insert_count: int = 10
    exploration_pool_size: int = 500
    evaluation_ks: str = "[20, 40, 60, 80, 100]"


@dataclass(frozen=True)
class AppConfig:
    database: DatabaseSettings
    paths: PathSettings
    model: ModelSettings
    training: TrainingSettings
    daily: DailySettings
    cke_full: CkeFullSettings


def _resolve_placeholder(raw: str) -> str:
    def replace(match: re.Match[str]) -> str:
        env_name = match.group(1)
        default_value = match.group(2) or ""
        return os.environ.get(env_name, default_value)

    return PLACEHOLDER_PATTERN.sub(replace, raw).strip()


def _load_application_properties(path: Path) -> dict[str, str]:
    data: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        data[key.strip()] = _resolve_placeholder(value)
    return data


def _parse_jdbc_url(jdbc_url: str) -> tuple[str, int, str]:
    normalized = jdbc_url.replace("jdbc:", "", 1)
    parsed = urlparse(normalized)
    database = parsed.path.lstrip("/")
    query = parse_qs(parsed.query)
    host = parsed.hostname or "127.0.0.1"
    port = parsed.port or 3306
    if "serverTimezone" in query and query["serverTimezone"]:
        os.environ.setdefault("TZ", query["serverTimezone"][0])
    return host, port, database


def load_config() -> AppConfig:
    project_root = Path(__file__).resolve().parents[2]
    properties_path = project_root / "backend" / "let-me-cook" / "src" / "main" / "resources" / "application.properties"
    props = _load_application_properties(properties_path)
    host, port, database = _parse_jdbc_url(props["spring.datasource.url"])

    runtime_root = project_root / "runtime_data" / "daily_reco"
    top100_bundle_root = project_root / "local_top100_cke_full"
    internal_cke_root = top100_bundle_root / "cke_full_source"
    fallback_cke_root = project_root.parent / "KGAT-pytorch-master" / "KGAT-pytorch-master"
    external_cke_root = internal_cke_root if internal_cke_root.exists() else fallback_cke_root
    internal_kg_root = top100_bundle_root / "knowledge_graph_source"
    fallback_kg_root = project_root.parent / "美食天下-KG"
    knowledge_graph_root = internal_kg_root if internal_kg_root.exists() else fallback_kg_root
    paths = PathSettings(
        project_root=project_root,
        backend_properties=properties_path,
        top100_bundle_root=top100_bundle_root,
        runtime_root=runtime_root,
        datasets_dir=runtime_root / "datasets",
        checkpoints_dir=runtime_root / "checkpoints",
        embeddings_dir=runtime_root / "embeddings",
        manifests_dir=runtime_root / "manifests",
        snapshots_dir=runtime_root / "snapshots",
        logs_dir=runtime_root / "logs",
        image_root=Path(r"F:\Desktop\images\resized_128x128"),
        external_cke_root=external_cke_root,
        knowledge_graph_root=knowledge_graph_root,
    )
    database_settings = DatabaseSettings(
        host=host,
        port=port,
        database=database,
        username=props["spring.datasource.username"],
        password=props["spring.datasource.password"],
    )
    return AppConfig(
        database=database_settings,
        paths=paths,
        model=ModelSettings(),
        training=TrainingSettings(),
        daily=DailySettings(),
        cke_full=CkeFullSettings(),
    )
