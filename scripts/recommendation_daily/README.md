# 每日推荐离线流程

当前口径已经收成一条简单链路：

- **本地离线机器**完成全部工作
  - 筛选目标用户和目标食谱
  - 生成筛选后的 CKE 训练数据
  - 调用仓库内知识图谱程序生成筛选后的 `kg_final.txt`
  - 使用仓库内 **CKEFull** 历史最优模型，或重训 `CKEFull`
  - 为每个目标用户生成 `Top100`
  - 直接写入 `daily_recipe_recommendations`
- **业务服务器**只读结果
  - 后端接口只读 `daily_recipe_recommendations`
  - 不参与训练
  - 不需要知道训练细节

## 当前产品口径

- `CKEFull` 产出每个目标用户的候选 `Top100`
- `Top100` 中会插入少量新品探索位
- 数据库存储：
  - 每用户每天保存 `Top100`
  - 前 `16` 条写成 `selected_for_delivery=1`
- 接口读取：
  - `GET /api/recipes/recommend?type=daily&limit=8`
  - `GET /api/recipes/recommend?type=personal&limit=12`
  - 若用户当天已有离线 `Top100`，后端优先读取这批结果
  - 若没有离线结果，再回退实时推荐

## 仓库内依赖目录

用于生成 Top100 的外部依赖已经收进仓库：

- `foodrec/local_top100_cke_full/cke_full_source/`
  - `main_cke_full.py`
  - `data_loader/loader_cke_full.py`
  - `model/CKE_full.py`
  - `parser/paser_all.py`
  - `datasets/meishitianxia_v1/`
  - `trained_model/CKEFull/meishitianxia_v1/embed-dim64_relation-dim32_vocab2000_lr1e-05_pretrain2/`
- `foodrec/local_top100_cke_full/knowledge_graph_source/`
  - `2-13-知识图谱构建.py`
  - 原始与参考映射 CSV

脚本默认优先使用仓库内这套 bundle；只有仓库内 bundle 缺失时，才会回退去找历史外部目录。

## 当前已确认使用的历史最优模型

当前 `use-existing-model` 默认使用：

- 数据集：
  - `foodrec/local_top100_cke_full/cke_full_source/datasets/meishitianxia_v1`
- 模型：
  - `foodrec/local_top100_cke_full/cke_full_source/trained_model/CKEFull/meishitianxia_v1/embed-dim64_relation-dim32_vocab2000_lr1e-05_pretrain2/model_epoch0030.pt`

这套模型对应：

- `5461` 个历史用户
- `24572` 个历史菜谱
- 通过当前库的 `user_id_mapping / recipe_id_mapping` 对齐后：
  - `5460` 个用户可映射
  - `24572` 个菜谱全部可映射

本地最近一次 `use-existing-model` 结果：

- 覆盖用户：`5460`
- 写入总行数：`546000`
- 每用户：`100` 条
- 每用户主推池：`16` 条
- 覆盖食谱：`19544`

运行结果 manifest：

- `foodrec/runtime_data/daily_reco/manifests/daily_result_manifest.json`

## 目录说明

- `main.py`
  - 入口：`bootstrap-baseline` / `run-daily` / `use-existing-model`
- `pipeline.py`
  - 主流程：筛选 -> 导出 -> 构图 -> 训练或加载已有模型 -> 打分 -> 落库
- `exporter.py`
  - 导出筛选后的 CKEFull 子数据集
- `knowledge_graph_bridge.py`
  - 对接仓库内 `knowledge_graph_source`
- `cke_full_runner.py`
  - 对接仓库内 `cke_full_source`
- `ranker.py`
  - 新品探索位、主推池选择、推荐理由生成

## 环境准备

```powershell
cd F:\Desktop\大创\foodrec
uv python install 3.11
uv venv scripts/recommendation_daily/.venv --python 3.11
scripts\recommendation_daily\.venv\Scripts\python.exe -m pip install -r scripts\recommendation_daily\requirements.txt
```

## 首轮本地 baseline

```powershell
cd F:\Desktop\大创\foodrec
scripts\recommendation_daily\.venv\Scripts\python.exe -m scripts.recommendation_daily.main bootstrap-baseline
```

这一步会：

1. 筛出满足历史交互门槛的用户
2. 导出筛选后的子数据集
3. 调用仓库内知识图谱程序生成筛选后的 `kg_final.txt`
4. 训练 `CKEFull`
5. 生成每个目标用户 `Top100`
6. 写入 `daily_recipe_recommendations`

## 直接使用已经训练好的 CKEFull

如果不想重训，直接执行：

```powershell
cd F:\Desktop\大创\foodrec
scripts\recommendation_daily\.venv\Scripts\python.exe -m scripts.recommendation_daily.main use-existing-model
```

当前默认会：

1. 使用仓库内 `local_top100_cke_full/cke_full_source/datasets/meishitianxia_v1`
2. 自动找到仓库内 `trained_model/CKEFull/meishitianxia_v1` 下历史表现最好的 checkpoint
3. 通过当前库里的 `user_id_mapping / recipe_id_mapping` 把旧 ID 对齐到新库
4. 为能对齐的用户直接生成并写入 `Top100`

## 每日更新

```powershell
cd F:\Desktop\大创\foodrec
scripts\recommendation_daily\.venv\Scripts\python.exe -m scripts.recommendation_daily.main run-daily
```

如果本地机器可以直接连接目标 MySQL，这一步会直接把结果写入目标库。

如果本地机器不能直连目标 MySQL，建议在本地执行完 `run-daily` 后，再把 `daily_recipe_recommendations` / `daily_recommend_job_runs` 的当日增量结果导入目标数据库。

## 运行工件

运行工件写在：

```text
foodrec/runtime_data/daily_reco/
```

主要内容：

- `datasets/daily_local/`
- `checkpoints/cke_full_baseline/`
- `checkpoints/cke_full_incremental/`
- `manifests/daily_result_manifest.json`
- `kg_program_output/`

这些运行工件不进 Git。
