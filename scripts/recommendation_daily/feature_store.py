from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Iterable

import numpy as np
import torch

from .backbone import JinaClipBackbone
from .config import AppConfig
from .data import RecipeRecord, resolve_image_path


TEXT_VECTOR_FILENAME = "text_vectors.npy"
IMAGE_VECTOR_FILENAME = "image_vectors.npy"
FEATURE_MANIFEST_FILENAME = "recipe_feature_manifest.json"


@dataclass(frozen=True)
class FeatureSyncResult:
    changed_recipe_ids: list[int]
    processed_recipe_ids: list[int]
    text_vector_path: Path
    image_vector_path: Path
    manifest_path: Path


class FeatureStore:
    def __init__(self, config: AppConfig) -> None:
        self.config = config
        self.text_vector_path = config.paths.embeddings_dir / TEXT_VECTOR_FILENAME
        self.image_vector_path = config.paths.embeddings_dir / IMAGE_VECTOR_FILENAME
        self.manifest_path = config.paths.manifests_dir / FEATURE_MANIFEST_FILENAME

    def load_manifest(self) -> dict[str, dict]:
        if not self.manifest_path.exists():
            return {}
        try:
            data = json.loads(self.manifest_path.read_text(encoding="utf-8"))
            return data if isinstance(data, dict) else {}
        except Exception:
            return {}

    def save_manifest(self, manifest: dict[str, dict]) -> None:
        self.manifest_path.parent.mkdir(parents=True, exist_ok=True)
        self.manifest_path.write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def sync_recipe_features(
        self,
        recipes: Iterable[RecipeRecord],
        recipe_mapping: dict[str, int],
        extractor: JinaClipBackbone,
        *,
        full_refresh: bool = False,
    ) -> FeatureSyncResult:
        recipe_list = list(recipes)
        manifest = self.load_manifest()
        required_size = max(recipe_mapping.values(), default=-1) + 1
        text_matrix = self._load_matrix(self.text_vector_path, required_size)
        image_matrix = self._load_matrix(self.image_vector_path, required_size)

        changed_recipe_ids: list[int] = []
        pending: list[tuple[RecipeRecord, int, str, str, str]] = []
        processed_recipe_ids: list[int] = []

        for recipe in recipe_list:
            recipe_key = str(recipe.recipe_id)
            if recipe_key not in recipe_mapping:
                continue
            vector_index = recipe_mapping[recipe_key]
            recipe_manifest = manifest.get(recipe_key)
            text_hash = _hash_text(recipe.text_input)
            image_path = resolve_image_path(self.config, recipe)
            image_hash = _hash_image_signature(image_path)
            modality_status = _resolve_modality_status(recipe.text_input, image_path)
            needs_refresh = (
                full_refresh
                or recipe_manifest is None
                or recipe_manifest.get("feature_version") != self.config.model.feature_version
                or int(recipe_manifest.get("vector_index", -1)) != vector_index
                or recipe_manifest.get("text_hash") != text_hash
                or recipe_manifest.get("image_hash") != image_hash
            )
            processed_recipe_ids.append(recipe.recipe_id)
            if needs_refresh:
                pending.append((recipe, vector_index, text_hash, image_hash, modality_status))

        batch_size = max(self.config.model.extract_batch_size, 1)
        for start in range(0, len(pending), batch_size):
            batch = pending[start:start + batch_size]
            texts = [recipe.text_input for recipe, *_ in batch]
            image_paths = [resolve_image_path(self.config, recipe) for recipe, *_ in batch]
            text_features = extractor.encode_text(texts).detach().cpu().numpy().astype(np.float32)
            image_features = extractor.encode_images(image_paths).detach().cpu().numpy().astype(np.float32)
            for row_index, (recipe, vector_index, text_hash, image_hash, modality_status) in enumerate(batch):
                text_matrix[vector_index] = text_features[row_index]
                image_matrix[vector_index] = image_features[row_index]
                manifest[str(recipe.recipe_id)] = {
                    "recipe_id": recipe.recipe_id,
                    "feature_version": self.config.model.feature_version,
                    "text_hash": text_hash,
                    "image_hash": image_hash,
                    "text_vector_file": TEXT_VECTOR_FILENAME,
                    "image_vector_file": IMAGE_VECTOR_FILENAME,
                    "vector_index": vector_index,
                    "updated_at": datetime.now().isoformat(timespec="seconds"),
                    "modality_status": modality_status,
                }
                changed_recipe_ids.append(recipe.recipe_id)
            if extractor.device.type == "cuda":
                torch.cuda.empty_cache()

        self._save_matrix(self.text_vector_path, text_matrix)
        self._save_matrix(self.image_vector_path, image_matrix)
        self.save_manifest(manifest)
        return FeatureSyncResult(
            changed_recipe_ids=sorted(set(changed_recipe_ids)),
            processed_recipe_ids=sorted(set(processed_recipe_ids)),
            text_vector_path=self.text_vector_path,
            image_vector_path=self.image_vector_path,
            manifest_path=self.manifest_path,
        )

    def find_missing_recipe_ids(self, recipes: Iterable[RecipeRecord], recipe_mapping: dict[str, int]) -> list[int]:
        manifest = self.load_manifest()
        missing: list[int] = []
        for recipe in recipes:
            recipe_key = str(recipe.recipe_id)
            expected_index = recipe_mapping.get(recipe_key)
            if expected_index is None:
                continue
            entry = manifest.get(recipe_key)
            if entry is None:
                missing.append(recipe.recipe_id)
                continue
            if entry.get("feature_version") != self.config.model.feature_version:
                missing.append(recipe.recipe_id)
                continue
            if int(entry.get("vector_index", -1)) != expected_index:
                missing.append(recipe.recipe_id)
        return sorted(set(missing))

    def load_feature_matrices(self, required_size: int | None = None) -> tuple[np.ndarray, np.ndarray]:
        text_matrix = self._load_matrix(self.text_vector_path, required_size or 0)
        image_matrix = self._load_matrix(self.image_vector_path, required_size or 0)
        return text_matrix, image_matrix

    def _load_matrix(self, path: Path, required_size: int) -> np.ndarray:
        if path.exists():
            matrix = np.load(path)
        else:
            matrix = np.zeros((0, self.config.model.feature_dim), dtype=np.float32)
        if matrix.ndim != 2:
            matrix = np.zeros((0, self.config.model.feature_dim), dtype=np.float32)
        if matrix.shape[1] != self.config.model.feature_dim:
            resized = np.zeros((matrix.shape[0], self.config.model.feature_dim), dtype=np.float32)
            width = min(matrix.shape[1], self.config.model.feature_dim)
            if width > 0:
                resized[:, :width] = matrix[:, :width]
            matrix = resized
        if matrix.shape[0] < required_size:
            padding = np.zeros((required_size - matrix.shape[0], self.config.model.feature_dim), dtype=np.float32)
            matrix = np.vstack([matrix, padding])
        return matrix.astype(np.float32, copy=False)

    def _save_matrix(self, path: Path, matrix: np.ndarray) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        np.save(path, matrix.astype(np.float32, copy=False))


def _hash_text(text: str) -> str:
    return hashlib.sha256((text or "").strip().encode("utf-8")).hexdigest()


def _hash_image_signature(image_path: Path | None) -> str:
    if image_path is None or not image_path.exists():
        return ""
    stat = image_path.stat()
    payload = f"{image_path}:{stat.st_size}:{int(stat.st_mtime)}"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _resolve_modality_status(text_input: str, image_path: Path | None) -> str:
    has_text = bool((text_input or "").strip())
    has_image = image_path is not None and image_path.exists()
    if has_text and has_image:
        return "text+image"
    if has_text:
        return "text-only"
    if has_image:
        return "image-only"
    return "missing"
