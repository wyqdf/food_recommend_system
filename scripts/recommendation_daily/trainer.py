from __future__ import annotations

import random
from contextlib import nullcontext
from dataclasses import dataclass
from math import ceil
from pathlib import Path

import numpy as np
import torch
from safetensors.torch import load_file, save_file
from torch.optim import AdamW
from tqdm import tqdm

from .backbone import JinaClipBackbone
from .config import AppConfig
from .data import RecipeRecord, resolve_image_path
from .model import CkeDailyModel


@dataclass
class TrainingSnapshot:
    user_mapping: dict[str, int]
    recipe_mapping: dict[str, int]
    entity_mapping: dict[str, int]
    relation_mapping: dict[str, int]
    recipes: list[RecipeRecord]
    interactions: list[tuple[int, int, float]]
    triples: list[tuple[int, int, int]]
    target_user_ids: list[int]


class DailyTrainer:
    def __init__(self, config: AppConfig, device: torch.device | None = None) -> None:
        self.config = config
        self.device = device or torch.device("cpu")
        self.model: CkeDailyModel | None = None

    def initialize_model(
        self,
        snapshot: TrainingSnapshot,
        backbone_dim: int,
        checkpoint_path: Path | None = None,
    ) -> CkeDailyModel:
        model = CkeDailyModel(
            n_users=len(snapshot.user_mapping),
            n_items=len(snapshot.recipe_mapping),
            n_entities=len(snapshot.entity_mapping),
            n_relations=len(snapshot.relation_mapping),
            hidden_dim=self.config.model.hidden_dim,
            backbone_dim=backbone_dim,
        ).to(self.device)
        if checkpoint_path and checkpoint_path.exists():
            model.load_state_dict(load_file(str(checkpoint_path)), strict=False)
        self.model = model
        return model

    def fit(
        self,
        snapshot: TrainingSnapshot,
        text_feature_matrix: np.ndarray,
        image_feature_matrix: np.ndarray,
        checkpoint_path: Path | None = None,
        incremental: bool = False,
    ) -> Path:
        backbone_dim = self._validate_feature_matrices(text_feature_matrix, image_feature_matrix, snapshot)
        model = self.initialize_model(snapshot, backbone_dim, checkpoint_path)
        if not snapshot.interactions:
            raise RuntimeError("没有可用于训练的正样本")
        if not snapshot.recipes:
            raise RuntimeError("没有可用于训练的食谱")

        optimizer = AdamW(
            model.parameters(),
            lr=self.config.training.learning_rate,
            weight_decay=self.config.training.weight_decay,
        )
        epochs = self.config.training.incremental_epochs if incremental else self.config.training.baseline_epochs
        available_item_ids = sorted(
            {
                snapshot.recipe_mapping[str(recipe.recipe_id)]
                for recipe in snapshot.recipes
                if str(recipe.recipe_id) in snapshot.recipe_mapping
            }
        )
        if not available_item_ids:
            raise RuntimeError("没有可用于训练的食谱向量")

        all_positive_pairs = {(user_idx, item_idx) for user_idx, item_idx, _ in snapshot.interactions}
        interaction_rows = list(snapshot.interactions)
        max_interactions = self.config.training.max_user_positive_pairs * max(len(snapshot.target_user_ids), 1)
        if len(interaction_rows) > max_interactions:
            interaction_rows = interaction_rows[:max_interactions]
        triple_rows = list(snapshot.triples[: self.config.training.max_kg_triples])

        for _ in range(epochs):
            model.train()
            random.shuffle(interaction_rows)
            for batch in tqdm(
                _batch(interaction_rows, self.config.training.batch_size),
                total=max(1, ceil(len(interaction_rows) / self.config.training.batch_size)),
                desc="train-interactions",
                leave=False,
            ):
                user_ids = torch.tensor([row[0] for row in batch], device=self.device, dtype=torch.long)
                pos_item_ids = torch.tensor([row[1] for row in batch], device=self.device, dtype=torch.long)
                neg_item_ids = torch.tensor(
                    [_sample_negative(row[0], available_item_ids, all_positive_pairs) for row in batch],
                    device=self.device,
                    dtype=torch.long,
                )
                weights = torch.tensor([row[2] for row in batch], device=self.device, dtype=torch.float32)
                pos_text = _to_feature_tensor(text_feature_matrix, pos_item_ids.tolist(), self.device)
                pos_img = _to_feature_tensor(image_feature_matrix, pos_item_ids.tolist(), self.device)
                neg_text = _to_feature_tensor(text_feature_matrix, neg_item_ids.tolist(), self.device)
                neg_img = _to_feature_tensor(image_feature_matrix, neg_item_ids.tolist(), self.device)

                optimizer.zero_grad(set_to_none=True)
                loss = model.interaction_loss(
                    user_ids,
                    pos_item_ids,
                    neg_item_ids,
                    pos_text,
                    pos_img,
                    neg_text,
                    neg_img,
                    weights,
                )
                loss.backward()
                optimizer.step()

            if triple_rows:
                random.shuffle(triple_rows)
                for batch in tqdm(
                    _batch(triple_rows, self.config.training.batch_size),
                    total=max(1, ceil(len(triple_rows) / self.config.training.batch_size)),
                    desc="train-kg",
                    leave=False,
                ):
                    head_ids = torch.tensor([row[0] for row in batch], device=self.device, dtype=torch.long)
                    relation_ids = torch.tensor([row[1] for row in batch], device=self.device, dtype=torch.long)
                    tail_ids = torch.tensor([row[2] for row in batch], device=self.device, dtype=torch.long)
                    head_text = _to_feature_tensor(text_feature_matrix, head_ids.tolist(), self.device)
                    head_img = _to_feature_tensor(image_feature_matrix, head_ids.tolist(), self.device)

                    optimizer.zero_grad(set_to_none=True)
                    kg_loss = model.kg_loss(
                        head_ids,
                        relation_ids,
                        tail_ids,
                        head_text,
                        head_img,
                    ) * self.config.training.kg_loss_weight
                    kg_loss.backward()
                    optimizer.step()

        checkpoint_out = self.config.paths.checkpoints_dir / (
            "daily_incremental.safetensors" if incremental else "daily_baseline.safetensors"
        )
        checkpoint_out.parent.mkdir(parents=True, exist_ok=True)
        save_file(model.state_dict(), str(checkpoint_out))
        return checkpoint_out

    def fit_with_backbone(
        self,
        snapshot: TrainingSnapshot,
        backbone: JinaClipBackbone,
        checkpoint_path: Path | None = None,
    ) -> tuple[Path, Path]:
        model = self.initialize_model(snapshot, backbone.output_dim, checkpoint_path)
        trainable_backbone_names = backbone.enable_adapter_training()
        if not snapshot.interactions:
            raise RuntimeError("没有可用于训练的正样本")
        if not snapshot.recipes:
            raise RuntimeError("没有可用于训练的食谱")
        if not trainable_backbone_names:
            raise RuntimeError("Jina-CLIP 适配器参数不可训练")

        optimizer = AdamW(
            [
                {
                    "params": [parameter for parameter in model.parameters() if parameter.requires_grad],
                    "lr": self.config.training.learning_rate,
                    "weight_decay": self.config.training.weight_decay,
                },
                {
                    "params": [parameter for parameter in backbone.model.parameters() if parameter.requires_grad],
                    "lr": self.config.training.encoder_learning_rate,
                    "weight_decay": self.config.training.weight_decay,
                },
            ]
        )
        available_item_ids = sorted(
            {
                snapshot.recipe_mapping[str(recipe.recipe_id)]
                for recipe in snapshot.recipes
                if str(recipe.recipe_id) in snapshot.recipe_mapping
            }
        )
        if not available_item_ids:
            raise RuntimeError("没有可用于训练的食谱向量")

        recipe_by_item_idx = {
            snapshot.recipe_mapping[str(recipe.recipe_id)]: recipe
            for recipe in snapshot.recipes
            if str(recipe.recipe_id) in snapshot.recipe_mapping
        }
        all_positive_pairs = {(user_idx, item_idx) for user_idx, item_idx, _ in snapshot.interactions}
        interaction_rows = list(snapshot.interactions)
        max_interactions = self.config.training.max_user_positive_pairs * max(len(snapshot.target_user_ids), 1)
        if len(interaction_rows) > max_interactions:
            interaction_rows = interaction_rows[:max_interactions]
        triple_rows = list(snapshot.triples[: self.config.training.max_kg_triples])

        for _ in range(self.config.training.baseline_epochs):
            model.train()
            backbone.train()
            random.shuffle(interaction_rows)
            for batch in tqdm(
                _batch(interaction_rows, self.config.training.baseline_online_batch_size),
                total=max(1, ceil(len(interaction_rows) / self.config.training.baseline_online_batch_size)),
                desc="train-interactions-online",
                leave=False,
            ):
                user_ids = torch.tensor([row[0] for row in batch], device=self.device, dtype=torch.long)
                pos_item_ids = torch.tensor([row[1] for row in batch], device=self.device, dtype=torch.long)
                neg_item_ids = torch.tensor(
                    [_sample_negative(row[0], available_item_ids, all_positive_pairs) for row in batch],
                    device=self.device,
                    dtype=torch.long,
                )
                weights = torch.tensor([row[2] for row in batch], device=self.device, dtype=torch.float32)

                pos_text, pos_img = self._encode_item_batch(backbone, recipe_by_item_idx, pos_item_ids.tolist())
                neg_text, neg_img = self._encode_item_batch(backbone, recipe_by_item_idx, neg_item_ids.tolist())

                optimizer.zero_grad(set_to_none=True)
                with self._autocast():
                    loss = model.interaction_loss(
                        user_ids,
                        pos_item_ids,
                        neg_item_ids,
                        pos_text,
                        pos_img,
                        neg_text,
                        neg_img,
                        weights,
                    )
                loss.backward()
                optimizer.step()

            if triple_rows:
                random.shuffle(triple_rows)
                for batch in tqdm(
                    _batch(triple_rows, self.config.training.baseline_online_batch_size),
                    total=max(1, ceil(len(triple_rows) / self.config.training.baseline_online_batch_size)),
                    desc="train-kg-online",
                    leave=False,
                ):
                    head_ids = torch.tensor([row[0] for row in batch], device=self.device, dtype=torch.long)
                    relation_ids = torch.tensor([row[1] for row in batch], device=self.device, dtype=torch.long)
                    tail_ids = torch.tensor([row[2] for row in batch], device=self.device, dtype=torch.long)
                    head_text, head_img = self._encode_item_batch(backbone, recipe_by_item_idx, head_ids.tolist())

                    optimizer.zero_grad(set_to_none=True)
                    with self._autocast():
                        kg_loss = model.kg_loss(
                            head_ids,
                            relation_ids,
                            tail_ids,
                            head_text,
                            head_img,
                        ) * self.config.training.kg_loss_weight
                    kg_loss.backward()
                    optimizer.step()

        checkpoint_out = self.config.paths.checkpoints_dir / "daily_baseline.safetensors"
        adapter_out = self.config.paths.checkpoints_dir / self.config.model.adapter_checkpoint_name
        checkpoint_out.parent.mkdir(parents=True, exist_ok=True)
        save_file(model.state_dict(), str(checkpoint_out))
        backbone.save_adapter(adapter_out)
        return checkpoint_out, adapter_out

    @torch.no_grad()
    def export_item_embeddings(
        self,
        snapshot: TrainingSnapshot,
        text_feature_matrix: np.ndarray,
        image_feature_matrix: np.ndarray,
        output_path: Path,
    ) -> Path:
        if self.model is None:
            raise RuntimeError("model is not initialized")
        self._validate_feature_matrices(text_feature_matrix, image_feature_matrix, snapshot)
        self.model.eval()

        total_items = max(snapshot.recipe_mapping.values(), default=-1) + 1
        if total_items <= 0:
            raise RuntimeError("没有可导出的食谱映射")

        ordered_indices = sorted(
            snapshot.recipe_mapping[str(recipe.recipe_id)]
            for recipe in snapshot.recipes
            if str(recipe.recipe_id) in snapshot.recipe_mapping
        )
        exported = np.zeros((total_items, self.config.model.hidden_dim), dtype=np.float32)
        if not ordered_indices:
            output_path.parent.mkdir(parents=True, exist_ok=True)
            np.save(output_path, exported)
            return output_path

        for batch_indices in tqdm(
            _batch(ordered_indices, self.config.training.eval_batch_size),
            total=max(1, ceil(len(ordered_indices) / self.config.training.eval_batch_size)),
            desc="export-embeddings",
            leave=False,
        ):
            item_ids = torch.tensor(batch_indices, device=self.device, dtype=torch.long)
            text_features = _to_feature_tensor(text_feature_matrix, batch_indices, self.device)
            image_features = _to_feature_tensor(image_feature_matrix, batch_indices, self.device)
            item_vectors = self.model.item_representation(item_ids, text_features, image_features)
            exported[batch_indices] = item_vectors.cpu().numpy().astype(np.float32)

        output_path.parent.mkdir(parents=True, exist_ok=True)
        np.save(output_path, exported)
        return output_path

    def _validate_feature_matrices(
        self,
        text_feature_matrix: np.ndarray,
        image_feature_matrix: np.ndarray,
        snapshot: TrainingSnapshot,
    ) -> int:
        if text_feature_matrix.ndim != 2 or image_feature_matrix.ndim != 2:
            raise RuntimeError("食谱向量矩阵维度不正确")
        if text_feature_matrix.shape != image_feature_matrix.shape:
            raise RuntimeError("文本和图片向量矩阵形状不一致")
        required_size = max(snapshot.recipe_mapping.values(), default=-1) + 1
        if text_feature_matrix.shape[0] < required_size:
            raise RuntimeError("食谱向量矩阵行数不足，无法覆盖当前映射")
        return int(text_feature_matrix.shape[1])

    def _encode_item_batch(
        self,
        backbone: JinaClipBackbone,
        recipe_by_item_idx: dict[int, RecipeRecord],
        item_indices: list[int],
    ) -> tuple[torch.Tensor, torch.Tensor]:
        recipes = [recipe_by_item_idx[item_idx] for item_idx in item_indices]
        texts = [recipe.text_input for recipe in recipes]
        image_paths = [resolve_image_path(self.config, recipe) for recipe in recipes]
        text_features = backbone.encode_text(texts)
        image_features = backbone.encode_images(image_paths)
        return text_features, image_features

    def _autocast(self):
        if self.device.type != "cuda":
            return nullcontext()
        dtype = torch.bfloat16 if self.config.model.use_bfloat16 else torch.float16
        return torch.autocast(device_type="cuda", dtype=dtype)


def _to_feature_tensor(matrix: np.ndarray, indices: list[int], device: torch.device) -> torch.Tensor:
    if not indices:
        return torch.zeros((0, matrix.shape[1]), device=device, dtype=torch.float32)
    values = matrix[np.asarray(indices, dtype=np.int64)]
    return torch.as_tensor(values, device=device, dtype=torch.float32)


def _sample_negative(user_idx: int, candidates: list[int], positives: set[tuple[int, int]]) -> int:
    sampled = random.choice(candidates)
    while (user_idx, sampled) in positives:
        sampled = random.choice(candidates)
    return sampled


def _batch(items: list, batch_size: int):
    for start in range(0, len(items), batch_size):
        yield items[start:start + batch_size]
