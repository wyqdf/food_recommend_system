from __future__ import annotations

import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
import torch

from .config import AppConfig


@dataclass
class CkeFullTrainingResult:
    save_dir: Path
    best_model_path: Path
    metrics_path: Path


def train_cke_full(
    config: AppConfig,
    *,
    incremental: bool,
    dataset_dir: Path,
    previous_model_path: Path | None = None,
) -> CkeFullTrainingResult:
    modules = _load_external_modules(config, config.cke_full.training_model_family)
    args = _build_args(
        config,
        dataset_dir=dataset_dir,
        incremental=incremental,
        previous_model_path=previous_model_path,
        model_family=config.cke_full.training_model_family,
    )
    modules["train"](args)
    best_model_path = _resolve_best_model_path(args.save_dir)
    metrics_path = Path(args.save_dir) / "metrics.tsv"
    return CkeFullTrainingResult(
        save_dir=Path(args.save_dir),
        best_model_path=best_model_path,
        metrics_path=metrics_path,
    )


def score_cke_full_topk(
    config: AppConfig,
    *,
    dataset_dir: Path,
    model_path: Path,
    topk: int,
    model_family: str | None = None,
) -> tuple[dict[int, list[tuple[int, float]]], dict[str, Any]]:
    resolved_model_family = model_family or config.cke_full.existing_model_family
    modules = _load_external_modules(config, resolved_model_family)
    args = _build_args(
        config,
        dataset_dir=dataset_dir,
        incremental=False,
        previous_model_path=model_path,
        model_family=resolved_model_family,
    )
    logging_module = modules["logging"]
    data_loader_cls = modules["data_loader_cls"]
    model_cls = modules["model_cls"]
    evaluate_fn = modules["evaluate"]

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    data = data_loader_cls(args, logging_module)
    args.n_vocab = data.n_vocab
    args.image_height = data.image_height
    args.image_width = data.image_width

    model = model_cls(args, data.n_users, data.n_items, data.n_entities, data.n_relations, data.n_vocab)
    state_dict = torch.load(model_path, map_location="cpu")
    model.load_state_dict(state_dict, strict=False)
    model.to(device)

    cf_scores, metrics = evaluate_fn(model, data, eval(args.Ks), device)
    test_users = list(data.test_user_dict.keys())
    inverse_recipe_mapping = {
        mapped_id: raw_recipe_id
        for raw_recipe_id, mapped_id in _load_id_mapping(dataset_dir / "recipe_mapping.txt").items()
    }
    inverse_user_mapping = {
        mapped_id: raw_user_id
        for raw_user_id, mapped_id in _load_id_mapping(dataset_dir / "user_mapping.txt").items()
    }

    topk_by_user: dict[int, list[tuple[int, float]]] = {}
    for row_index, mapped_user_id in enumerate(test_users):
        score_row = np.asarray(cf_scores[row_index], dtype=np.float32).copy()
        seen_items = set(data.train_user_dict.get(int(mapped_user_id), []))
        seen_items.update(data.test_user_dict.get(int(mapped_user_id), []))
        if seen_items:
            score_row[np.asarray(sorted(seen_items), dtype=np.int64)] = -np.inf
        candidate_count = min(max(topk, 1), score_row.shape[0])
        top_item_indices = np.argsort(score_row)[::-1][:candidate_count]
        ranked: list[tuple[int, float]] = []
        for item_idx in top_item_indices.tolist():
            raw_recipe_id = inverse_recipe_mapping.get(int(item_idx))
            if raw_recipe_id is None:
                continue
            ranked.append((raw_recipe_id, float(score_row[item_idx])))
        raw_user_id = inverse_user_mapping.get(int(mapped_user_id))
        if raw_user_id is not None:
            topk_by_user[int(raw_user_id)] = ranked
    return topk_by_user, metrics


def _build_args(
    config: AppConfig,
    *,
    dataset_dir: Path,
    incremental: bool,
    previous_model_path: Path | None,
    model_family: str,
):
    modules = _load_external_modules(config, model_family)
    parser_cls = modules["parser_cls"]
    parser_instance = parser_cls()
    args = parser_instance.parser.parse_args([])
    args.sdae_dim_list = eval(args.sdae_dim_list)
    args.scae_channel_list = eval(args.scae_channel_list)
    args.scae_kernel_list = eval(args.scae_kernel_list)

    existing_model_metadata = (
        _load_existing_model_metadata(Path(previous_model_path))
        if previous_model_path and Path(previous_model_path).exists()
        else {}
    )

    args.data_name = dataset_dir.name
    args.data_dir = str(dataset_dir.parent)
    args.save_dir = str(
        config.paths.checkpoints_dir
        / ("cke_full_incremental" if incremental else "cke_full_baseline")
    )
    args.embed_dim = int(existing_model_metadata.get("embed_dim", config.cke_full.embed_dim))
    args.relation_dim = int(existing_model_metadata.get("relation_dim", config.cke_full.relation_dim))
    args.lr = float(existing_model_metadata.get("lr", config.cke_full.learning_rate))
    args.n_epoch = int(existing_model_metadata.get("n_epoch", config.cke_full.epochs))
    args.evaluate_every = int(existing_model_metadata.get("evaluate_every", config.cke_full.evaluate_every))
    args.stopping_steps = int(existing_model_metadata.get("stopping_steps", config.cke_full.stopping_steps))
    args.cf_batch_size = int(existing_model_metadata.get("cf_batch_size", config.cke_full.cf_batch_size))
    args.kg_batch_size = int(existing_model_metadata.get("kg_batch_size", config.cke_full.kg_batch_size))
    args.sdae_batch_size = int(existing_model_metadata.get("sdae_batch_size", config.cke_full.sdae_batch_size))
    args.scae_batch_size = int(existing_model_metadata.get("scae_batch_size", config.cke_full.scae_batch_size))
    args.image_height = int(existing_model_metadata.get("image_height", config.cke_full.image_height))
    args.image_width = int(existing_model_metadata.get("image_width", config.cke_full.image_width))
    args.test_batch_size = int(existing_model_metadata.get("test_batch_size", config.cke_full.test_batch_size))
    args.print_every = int(existing_model_metadata.get("print_every", config.cke_full.print_every))
    if hasattr(args, "review_align_weight"):
        args.review_align_weight = float(
            existing_model_metadata.get("review_align_weight", config.cke_full.review_align_weight)
        )
    args.Ks = str(existing_model_metadata.get("Ks", config.cke_full.evaluation_ks))
    args.n_vocab = int(existing_model_metadata.get("n_vocab", getattr(args, "n_vocab", 2000)))
    args.sdae_dim_list = existing_model_metadata.get("sdae_dim_list", args.sdae_dim_list)
    args.scae_channel_list = existing_model_metadata.get("scae_channel_list", args.scae_channel_list)
    args.scae_kernel_list = existing_model_metadata.get("scae_kernel_list", args.scae_kernel_list)
    args.use_pretrain = 2 if previous_model_path and Path(previous_model_path).exists() else 0
    args.pretrain_model_path = str(previous_model_path) if previous_model_path else ""
    Path(args.save_dir).mkdir(parents=True, exist_ok=True)
    return args


def _load_external_modules(config: AppConfig, model_family: str) -> dict[str, Any]:
    external_root = str(config.paths.external_cke_root)
    if external_root not in sys.path:
        sys.path.insert(0, external_root)
    import importlib
    import logging

    family = model_family.strip().lower()
    parser_module = importlib.import_module("parser.paser_all")
    if family == "ckefull":
        main_module = importlib.import_module("main_cke_full")
        data_loader_module = importlib.import_module("data_loader.loader_cke_full")
        model_module = importlib.import_module("model.CKE_full")
        parser_cls = parser_module.CKEFullArgs
        data_loader_cls = data_loader_module.DataLoaderCKEFull
    elif family == "ckefullmodify":
        main_module = importlib.import_module("main_cke_full_modify")
        data_loader_module = importlib.import_module("data_loader.loader_cke_full_modify")
        model_module = importlib.import_module("model.CKE_full_modify")
        parser_cls = parser_module.CKEFullModifyArgs
        data_loader_cls = data_loader_module.DataLoaderCKEFullModify
    else:
        raise ValueError(f"不支持的 CKE 模型族: {model_family}")
    return {
        "train": main_module.train,
        "evaluate": main_module.evaluate,
        "parser_cls": parser_cls,
        "data_loader_cls": data_loader_cls,
        "model_cls": model_module.CKE,
        "logging": logging,
    }


def _resolve_best_model_path(save_dir: str | Path) -> Path:
    candidates = sorted(Path(save_dir).glob("model_epoch*.pt"))
    if not candidates:
        raise FileNotFoundError(f"未找到 CKE_full 模型文件: {save_dir}")
    return candidates[-1]


def _load_id_mapping(path: Path) -> dict[int, int]:
    mapping: dict[int, int] = {}
    if not path.exists():
        return mapping
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        raw_value, mapped_value = stripped.split()
        mapping[int(raw_value)] = int(mapped_value)
    return mapping


def _load_existing_model_metadata(model_path: Path) -> dict[str, Any]:
    log_path = model_path.parent / "train.log"
    if not log_path.exists():
        return {}

    namespace_line = None
    for line in log_path.read_text(encoding="utf-8", errors="ignore").splitlines():
        if "Namespace(" in line:
            namespace_line = line[line.index("Namespace("):]
            break
    if not namespace_line:
        return {}

    metadata: dict[str, Any] = {}
    scalar_patterns: dict[str, str] = {
        "embed_dim": r"embed_dim=(\d+)",
        "relation_dim": r"relation_dim=(\d+)",
        "lr": r"lr=([0-9.eE+-]+)",
        "n_epoch": r"n_epoch=(\d+)",
        "stopping_steps": r"stopping_steps=(\d+)",
        "print_every": r"print_every=(\d+)",
        "evaluate_every": r"evaluate_every=(\d+)",
        "cf_batch_size": r"cf_batch_size=(\d+)",
        "kg_batch_size": r"kg_batch_size=(\d+)",
        "sdae_batch_size": r"sdae_batch_size=(\d+)",
        "scae_batch_size": r"scae_batch_size=(\d+)",
        "n_vocab": r"n_vocab=(\d+)",
        "review_align_weight": r"review_align_weight=([0-9.eE+-]+)",
        "image_height": r"image_height=(\d+)",
        "image_width": r"image_width=(\d+)",
        "test_batch_size": r"test_batch_size=(\d+)",
    }
    list_patterns: dict[str, str] = {
        "sdae_dim_list": r"sdae_dim_list=(\[[^\]]*\])",
        "scae_channel_list": r"scae_channel_list=(\[[^\]]*\])",
        "scae_kernel_list": r"scae_kernel_list=(\[[^\]]*\])",
    }
    string_patterns: dict[str, str] = {
        "Ks": r"Ks='([^']*)'",
    }

    for key, pattern in scalar_patterns.items():
        match = re.search(pattern, namespace_line)
        if not match:
            continue
        value = match.group(1)
        metadata[key] = float(value) if any(ch in value for ch in ".eE") else int(value)

    for key, pattern in list_patterns.items():
        match = re.search(pattern, namespace_line)
        if match:
            metadata[key] = eval(match.group(1))

    for key, pattern in string_patterns.items():
        match = re.search(pattern, namespace_line)
        if match:
            metadata[key] = match.group(1)

    return metadata
