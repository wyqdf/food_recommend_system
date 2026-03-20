# 本地 Top100 生成依赖包

这个目录保存了“在本地离线生成用户 Top100 推荐”所需的交付说明和轻量源码入口。

当前普通 Git 只同步：

- README 与模型清单
- 小体量源码入口

不随普通 Git 推送的大文件：

- 历史模型权重
- 大型缓存文件
- 全量图片数据
- 运行工件

如果要在新机器上完整复现离线 Top100 生成，还需要额外同步这些本地大文件。

## 目录结构

- `cke_full_source/`
  - `main_cke_full.py`
  - `data_loader/loader_cke_full.py`
  - `model/CKE_full.py`
  - `parser/paser_all.py`
  - 大型数据集与模型文件本地保留，不走普通 Git
- `knowledge_graph_source/`
  - `2-13-知识图谱构建.py`
  - `data/用户ID映射关系.csv`
  - `data/菜谱ID映射关系.csv`
  - `reference_output/.../traditional_model_data/*.csv`
- `selected_model_manifest.json`
  - 当前默认使用的历史最优模型说明

## 当前默认使用的模型

当前 `scripts/recommendation_daily` 默认优先读取这套模型：

- 数据集：
  - `cke_full_source/datasets/meishitianxia_v1`
- 模型：
  - `cke_full_source/trained_model/CKEFull/meishitianxia_v1/embed-dim64_relation-dim32_vocab2000_lr1e-05_pretrain2/model_epoch0030.pt`

这是目前仓库内选定的 **CKEFull 历史最优版本**，不是 `CKEFullModify`。

## ID 对齐口径

`meishitianxia_v1` 里的用户和菜谱 ID 不是当前 `foodrec` 库的主键，写库时需要做两层对齐：

1. 先按知识图谱项目内的原始映射理解旧 ID：
   - `knowledge_graph_source/data/用户ID映射关系.csv`
   - `knowledge_graph_source/data/菜谱ID映射关系.csv`
2. 再按当前库的映射表转成现库主键：
   - `user_id_mapping.old_user_id -> new_user_id`
   - `recipe_id_mapping.old_recipe_id -> new_recipe_id`

当前已验证：

- 菜谱：`24572 / 24572` 可映射
- 用户：`5460 / 5461` 可映射
- 唯一跳过的是 `old_user_id = 0`

## 当前离线结果规模

最近一次 `use-existing-model` 已生成：

- 覆盖用户：`5460`
- 每用户 `Top100`
- 总写入行数：`546000`
- 每用户主推池：`16`
- 覆盖食谱：`19544`

结果说明见：

- `runtime_data/daily_reco/manifests/daily_result_manifest.json`

## 使用方式

实际入口不在本目录，而在：

- `scripts/recommendation_daily/main.py`

常用命令：

```powershell
cd F:\Desktop\大创\foodrec
scripts\recommendation_daily\.venv\Scripts\python.exe -m scripts.recommendation_daily.main use-existing-model
```

这条命令会：

1. 读取本目录中的 `meishitianxia_v1` 数据集和历史最优 `CKEFull` 模型
2. 生成每个映射用户的 `Top100`
3. 写入当前库的 `daily_recipe_recommendations`

业务服务器只读这些结果，不参与训练。
