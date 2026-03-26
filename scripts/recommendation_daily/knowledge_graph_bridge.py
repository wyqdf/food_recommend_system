from __future__ import annotations

import importlib.util
import json
import shutil
import sys
from pathlib import Path

from .config import AppConfig


def build_filtered_knowledge_graph(
    config: AppConfig,
    *,
    recipe_json_path: Path,
    output_kg_path: Path,
) -> Path:
    kg_root = config.paths.knowledge_graph_root
    config_path = kg_root / "config.py"
    script_path = kg_root / "2-13-知识图谱构建.py"
    if not config_path.exists() or not script_path.exists():
        raise FileNotFoundError(f"未找到知识图谱程序目录: {kg_root}")

    output_root = config.paths.runtime_root / "kg_program_output"
    output_dirs = {
        "train_data": str(output_root / "train_data"),
        "intermediate_data": str(output_root / "intermediate_data"),
        "neo4j_data": str(output_root / "neo4j_data"),
    }

    config_module = _load_module(config_path, "kg_external_config")
    original_config_module = sys.modules.get("config")
    try:
        config_module.OUTPUT_DIRS = output_dirs
        config_module.CONFIG["output_dirs"] = output_dirs
        config_module.create_output_dirs()
        sys.modules["config"] = config_module

        kg_module = _load_module(script_path, "kg_external_pipeline")
        pipeline = kg_module.CompleteDataPipeline()
        target_recipe_json = Path(pipeline.output_dirs["intermediate_data"]) / "新菜谱.json"
        target_recipe_json.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(recipe_json_path, target_recipe_json)
        pipeline.build_knowledge_graph()
        produced_kg_path = Path(pipeline.output_dirs["train_data"]) / "kg_final.txt"
        if not produced_kg_path.exists():
            raise FileNotFoundError(f"知识图谱生成完成后未找到 kg_final.txt: {produced_kg_path}")
        output_kg_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(produced_kg_path, output_kg_path)
        manifest_path = output_kg_path.parent / "kg_program_manifest.json"
        manifest_path.write_text(
            json.dumps(
                {
                    "source_script": str(script_path),
                    "generated_kg_path": str(output_kg_path),
                    "run_dir": str(pipeline._run_dir),
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )
        return output_kg_path
    finally:
        if original_config_module is not None:
            sys.modules["config"] = original_config_module
        else:
            sys.modules.pop("config", None)


def _load_module(path: Path, module_name: str):
    spec = importlib.util.spec_from_file_location(module_name, path)
    if spec is None or spec.loader is None:
        raise ImportError(f"无法加载模块: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module
