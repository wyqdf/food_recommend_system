from __future__ import annotations

import os
import sys
from contextlib import contextmanager
from pathlib import Path
from typing import Iterable

import torch
import torch.nn.functional as F
from PIL import Image
from safetensors.torch import load_file, save_file
from transformers import AutoConfig, AutoImageProcessor, AutoModel, AutoProcessor, AutoTokenizer

from .config import AppConfig


class JinaClipBackbone(torch.nn.Module):
    def __init__(self, config: AppConfig, device: torch.device) -> None:
        super().__init__()
        self.config = config
        self.device = device
        model_source = _resolve_model_source(config.model.backbone_name)
        os.environ.setdefault("HF_HUB_OFFLINE", "1")
        os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
        with _patched_local_pretrained_loaders():
            model_config = AutoConfig.from_pretrained(
                model_source,
                trust_remote_code=True,
                local_files_only=True,
            )
            if getattr(model_config, "text_config", None) is not None:
                text_model_source = _resolve_model_source(str(model_config.text_config.hf_model_name_or_path))
                model_config.text_config.hf_model_name_or_path = text_model_source
                if getattr(model_config.text_config, "hf_model_config_kwargs", None) is None:
                    model_config.text_config.hf_model_config_kwargs = {}
                model_config.text_config.hf_model_config_kwargs["use_flash_attn"] = False
            if hasattr(model_config, "use_text_flash_attn"):
                model_config.use_text_flash_attn = False
            if hasattr(model_config, "use_vision_xformers"):
                model_config.use_vision_xformers = False
            if getattr(model_config, "vision_config", None) is not None and hasattr(model_config.vision_config, "x_attention"):
                model_config.vision_config.x_attention = False
            self.processor = AutoProcessor.from_pretrained(
                model_source,
                trust_remote_code=True,
                local_files_only=True,
            )
            base_model = AutoModel.from_pretrained(
                model_source,
                config=model_config,
                trust_remote_code=True,
                local_files_only=True,
                low_cpu_mem_usage=False,
                device_map=None,
                dtype=torch.bfloat16 if config.model.use_bfloat16 and torch.cuda.is_available() else torch.float32,
            )
        _patch_rotary_fallback()
        self.model = base_model.to(device)
        self.output_dim = (
            getattr(getattr(self.model, "config", None), "projection_dim", None)
            or getattr(getattr(self.model, "config", None), "hidden_size", None)
            or config.model.feature_dim
        )
        self._adapter_path = self.config.paths.checkpoints_dir / self.config.model.adapter_checkpoint_name
        self.load_adapter(self._adapter_path)

    @torch.no_grad()
    def encode_for_export(self, texts: list[str], image_paths: list[Path | None]) -> tuple[torch.Tensor, torch.Tensor]:
        self.eval()
        text_features = self.encode_text(texts)
        image_features = self.encode_images(image_paths)
        return text_features, image_features

    def encode_text(self, texts: list[str]) -> torch.Tensor:
        inputs = self.processor(
            text=texts,
            return_tensors="pt",
            truncation=True,
            padding=True,
            max_length=self.config.model.max_text_length,
        )
        inputs = {key: value.to(self.device) for key, value in inputs.items()}
        if hasattr(self.model, "get_text_features"):
            features = self.model.get_text_features(**inputs)
        else:
            outputs = self.model(**inputs)
            features = outputs.text_embeds
        return F.normalize(features.float(), dim=-1)

    def encode_images(self, image_paths: Iterable[Path | None]) -> torch.Tensor:
        images: list[Image.Image] = []
        missing_flags: list[bool] = []
        for path in image_paths:
            if path is None or not path.exists():
                images.append(Image.new("RGB", (224, 224), color="black"))
                missing_flags.append(True)
            else:
                with Image.open(path) as image:
                    images.append(image.convert("RGB"))
                missing_flags.append(False)
        inputs = self.processor(images=images, return_tensors="pt")
        inputs = {key: value.to(self.device) for key, value in inputs.items()}
        if hasattr(self.model, "get_image_features"):
            features = self.model.get_image_features(**inputs)
        else:
            outputs = self.model(**inputs)
            features = outputs.image_embeds
        features = F.normalize(features.float(), dim=-1)
        if missing_flags:
            mask = torch.tensor(missing_flags, device=features.device, dtype=torch.bool)
            features[mask] = 0
        return features

    def enable_adapter_training(self) -> list[str]:
        trainable_names: list[str] = []
        for name, parameter in self.model.named_parameters():
            parameter.requires_grad = False
            if "lora_A" in name or "lora_B" in name or name == "logit_scale":
                parameter.requires_grad = True
                trainable_names.append(name)
        self.train()
        return trainable_names

    def adapter_state_dict(self) -> dict[str, torch.Tensor]:
        state_dict = self.model.state_dict()
        return {
            name: tensor.detach().cpu()
            for name, tensor in state_dict.items()
            if "lora_A" in name or "lora_B" in name or name == "logit_scale"
        }

    def save_adapter(self, path: Path | None = None) -> Path:
        output_path = path or self._adapter_path
        output_path.parent.mkdir(parents=True, exist_ok=True)
        save_file(self.adapter_state_dict(), str(output_path))
        return output_path

    def load_adapter(self, path: Path | None = None) -> bool:
        adapter_path = path or self._adapter_path
        if not adapter_path.exists():
            return False
        adapter_state = load_file(str(adapter_path))
        self.model.load_state_dict(adapter_state, strict=False)
        return True
def _resolve_model_source(backbone_name: str) -> str:
    if Path(backbone_name).exists():
        return backbone_name
    user_profile = os.environ.get("USERPROFILE")
    if not user_profile:
        return backbone_name
    cache_root = Path(user_profile) / ".cache" / "huggingface" / "hub"
    model_cache_dir = cache_root / f"models--{backbone_name.replace('/', '--')}"
    ref_path = model_cache_dir / "refs" / "main"
    if not ref_path.exists():
        return backbone_name
    snapshot_id = ref_path.read_text(encoding="utf-8").strip()
    snapshot_dir = model_cache_dir / "snapshots" / snapshot_id
    return str(snapshot_dir) if snapshot_dir.exists() else backbone_name


def _patch_rotary_fallback() -> None:
    target_suffix = "xlm_hyphen_roberta_hyphen_flash_hyphen_implementation"
    for module_name, module in list(sys.modules.items()):
        if module is None or target_suffix not in module_name or not module_name.endswith(".rotary"):
            continue
        if getattr(module, "_foodrec_rotary_patched", False):
            continue
        apply_rotary_emb_torch = getattr(module, "apply_rotary_emb_torch", None)
        if apply_rotary_emb_torch is None:
            continue

        def _fallback_apply_rotary(
            x,
            cos,
            sin,
            seqlen_offsets=0,
            cu_seqlens=None,
            max_seqlen=None,
            interleaved=False,
            inplace=False,
            conjugate=False,
            *args,
            **kwargs,
        ):
            if cu_seqlens is not None:
                raise RuntimeError("rotary fallback does not support cu_seqlens")
            if isinstance(seqlen_offsets, torch.Tensor):
                raise RuntimeError("rotary fallback does not support tensor seqlen_offsets")
            rotary_sin = -sin if conjugate else sin
            out = apply_rotary_emb_torch(x, cos, rotary_sin, interleaved=interleaved)
            if inplace:
                x.copy_(out)
                return x
            return out

        module.apply_rotary = _fallback_apply_rotary
        module._foodrec_rotary_patched = True


@contextmanager
def _patched_local_pretrained_loaders():
    original_auto_config = AutoConfig.from_pretrained.__func__
    original_auto_model = AutoModel.from_pretrained.__func__
    original_auto_processor = AutoProcessor.from_pretrained.__func__
    original_auto_tokenizer = AutoTokenizer.from_pretrained.__func__
    original_auto_image_processor = AutoImageProcessor.from_pretrained.__func__

    def _resolve_args(args, kwargs):
        args = list(args)
        if args:
            args[0] = _resolve_model_source(str(args[0]))
        elif "pretrained_model_name_or_path" in kwargs:
            kwargs["pretrained_model_name_or_path"] = _resolve_model_source(str(kwargs["pretrained_model_name_or_path"]))
        kwargs.setdefault("local_files_only", True)
        return tuple(args), kwargs

    def _patched_auto_config(cls, *args, **kwargs):
        resolved_args, resolved_kwargs = _resolve_args(args, kwargs)
        return original_auto_config(cls, *resolved_args, **resolved_kwargs)

    def _patched_auto_model(cls, *args, **kwargs):
        resolved_args, resolved_kwargs = _resolve_args(args, kwargs)
        return original_auto_model(cls, *resolved_args, **resolved_kwargs)

    def _patched_auto_processor(cls, *args, **kwargs):
        resolved_args, resolved_kwargs = _resolve_args(args, kwargs)
        return original_auto_processor(cls, *resolved_args, **resolved_kwargs)

    def _patched_auto_tokenizer(cls, *args, **kwargs):
        resolved_args, resolved_kwargs = _resolve_args(args, kwargs)
        return original_auto_tokenizer(cls, *resolved_args, **resolved_kwargs)

    def _patched_auto_image_processor(cls, *args, **kwargs):
        resolved_args, resolved_kwargs = _resolve_args(args, kwargs)
        return original_auto_image_processor(cls, *resolved_args, **resolved_kwargs)

    AutoConfig.from_pretrained = classmethod(_patched_auto_config)
    AutoModel.from_pretrained = classmethod(_patched_auto_model)
    AutoProcessor.from_pretrained = classmethod(_patched_auto_processor)
    AutoTokenizer.from_pretrained = classmethod(_patched_auto_tokenizer)
    AutoImageProcessor.from_pretrained = classmethod(_patched_auto_image_processor)
    try:
        yield
    finally:
        AutoConfig.from_pretrained = classmethod(original_auto_config)
        AutoModel.from_pretrained = classmethod(original_auto_model)
        AutoProcessor.from_pretrained = classmethod(original_auto_processor)
        AutoTokenizer.from_pretrained = classmethod(original_auto_tokenizer)
        AutoImageProcessor.from_pretrained = classmethod(original_auto_image_processor)
