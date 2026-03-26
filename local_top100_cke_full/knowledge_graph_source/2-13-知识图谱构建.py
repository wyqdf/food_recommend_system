#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
完整数据管道 - 从原始数据生成所有文件
集成所有功能：用户互动处理、知识图谱构建、Neo4j格式转换

输出结构：
  output/
  └── {时间戳}_minUser{x}_minItem{y}_density{z}/   ← 运行隔离目录
        ├── intermediate_data/
        ├── train_data/
        ├── neo4j_data/
        └── traditional_model_data/

步骤7：导出传统模型训练所需的四个文件到 traditional_model_data/
"""

import json
import pandas as pd
import random
import os
import re
from collections import defaultdict, Counter
from datetime import datetime
from tqdm import tqdm

# 导入配置文件
import config


class CompleteDataPipeline:
    def __init__(self):
        self.config = config.CONFIG
        self.data_dir = config.DATA_DIR
        _base_output_dirs = config.OUTPUT_DIRS          # 原始路径（只读，不直接使用）

        # ── 确定输出根目录（所有子目录的公共父路径，通常是 "output"） ──
        self._output_root = os.path.commonpath(list(_base_output_dirs.values()))

        # ── 生成运行隔离目录名（density 在步骤2完成后填入真实值，先占位 TBD） ──
        ts    = datetime.now().strftime('%Y%m%d_%H%M%S')
        min_u = self.config['min_user_interactions']
        min_r = self.config['min_recipe_interactions']
        self._run_folder_name = f"{ts}_minUser{min_u}_minItem{min_r}_density_TBD"
        self._run_dir         = os.path.join(self._output_root, self._run_folder_name)

        # ── 将所有原始输出子目录重定向到运行隔离目录下 ──
        self.output_dirs = {}
        for key, orig_path in _base_output_dirs.items():
            rel = os.path.relpath(orig_path, self._output_root)   # e.g. "intermediate_data"
            new_path = os.path.join(self._run_dir, rel)
            self.output_dirs[key] = new_path
            os.makedirs(new_path, exist_ok=True)

        # ── 传统模型数据目录 ──
        self.traditional_model_dir = os.path.join(self._run_dir, 'traditional_model_data')
        os.makedirs(self.traditional_model_dir, exist_ok=True)

        # ── 占位；步骤2执行后由 process_user_interactions 写入真实值 ──
        self._interaction_density = None

    # ──────────────────────────────────────────────────────────────────
    # 内部辅助：全部步骤完成后将运行目录重命名为含真实 density 的最终名称
    # ──────────────────────────────────────────────────────────────────
    def _finalize_run_folder(self):
        """将运行目录重命名为含真实 density 的最终名称，并同步更新所有内部路径。"""
        if self._interaction_density is None:
            print("警告: density 未计算，运行目录保持 TBD 命名。")
            return

        min_u = self.config['min_user_interactions']
        min_r = self.config['min_recipe_interactions']
        ts_part     = self._run_folder_name.split('_minUser')[0]
        density_str = f"{self._interaction_density:.6f}".rstrip('0').rstrip('.')

        new_folder_name = f"{ts_part}_minUser{min_u}_minItem{min_r}_density{density_str}"
        new_run_dir     = os.path.join(self._output_root, new_folder_name)
        old_run_dir     = self._run_dir

        os.rename(old_run_dir, new_run_dir)

        # 同步更新所有内部路径引用
        self._run_dir         = new_run_dir
        self._run_folder_name = new_folder_name

        for key in self.output_dirs:
            self.output_dirs[key] = self.output_dirs[key].replace(old_run_dir, new_run_dir)
        self.traditional_model_dir = self.traditional_model_dir.replace(old_run_dir, new_run_dir)

        print(f"\n运行目录已重命名: {new_run_dir}")

    # ══════════════════════════════════════════════════════════════════
    # 以下各步骤内部逻辑与原版完全一致
    # ══════════════════════════════════════════════════════════════════

    def process_interactions(self):
        """处理互动数据，生成用户-菜谱交互文件"""
        print("=== 步骤1: 处理用户互动数据 ===")

        input_file = self.config['input_files']['interactions']
        output_file = os.path.join(self.output_dirs['intermediate_data'], 'user_interactions.txt')

        print(f"正在读取互动数据: {input_file}")

        try:
            chunks = pd.read_csv(input_file, usecols=['old_user_id', 'old_recipe_id'], chunksize=100000)
            user_recipes = defaultdict(set)

            for chunk in chunks:
                for _, row in chunk.iterrows():
                    user_id = str(row['old_user_id']).strip()
                    recipe_id = str(row['old_recipe_id']).strip()
                    if user_id and recipe_id:
                        user_recipes[user_id].add(recipe_id)

            print(f"处理完成，共{len(user_recipes)}个用户的数据")

            with open(output_file, 'w', encoding='utf-8') as f:
                for user_id, recipes in user_recipes.items():
                    line = f"{user_id} {' '.join(recipes)}\n"
                    f.write(line)

            print(f"结果已保存到: {output_file}")
            return True

        except Exception as e:
            print(f"处理过程中出错: {str(e)}")
            return False

    def iterative_filter(self, user_interactions, min_user_interactions=5, min_recipe_interactions=15):
        """反复过滤用户和菜谱，直到数据稳定"""
        print("开始反复过滤...")

        filtered_interactions = user_interactions.copy()
        iteration = 0

        while True:
            iteration += 1
            prev_user_count = len(filtered_interactions)

            recipe_count = {}
            for recipes in filtered_interactions.values():
                for recipe_id in recipes:
                    recipe_count[recipe_id] = recipe_count.get(recipe_id, 0) + 1

            prev_recipe_count = len(recipe_count)

            valid_recipes = {recipe_id for recipe_id, count in recipe_count.items()
                            if count >= min_recipe_interactions}

            new_filtered_interactions = {}
            for user_id, recipes in filtered_interactions.items():
                valid_user_recipes = [recipe_id for recipe_id in recipes if recipe_id in valid_recipes]
                if len(valid_user_recipes) >= min_user_interactions:
                    new_filtered_interactions[user_id] = valid_user_recipes

            filtered_interactions = new_filtered_interactions

            new_recipe_count = set()
            for recipes in filtered_interactions.values():
                new_recipe_count.update(recipes)

            current_user_count = len(filtered_interactions)
            current_recipe_count = len(new_recipe_count)

            print(f"第{iteration}轮过滤: 用户 {prev_user_count} -> {current_user_count}, "
                  f"菜谱 {prev_recipe_count} -> {current_recipe_count}")

            if (current_user_count == prev_user_count and
                current_recipe_count == prev_recipe_count):
                break

        print(f"过滤完成，共进行{iteration}轮")
        return filtered_interactions

    def split_train_test_safe(self, user_interactions, test_ratio=0.2):
        """安全地将用户交互数据分为训练集和测试集"""
        train_data = {}
        test_data = {}

        for user_id, recipes in user_interactions.items():
            if len(recipes) < 2:
                train_data[user_id] = recipes.copy()
                test_data[user_id] = []
                continue

            shuffled_recipes = recipes.copy()
            random.shuffle(shuffled_recipes)

            test_size = max(1, int(len(shuffled_recipes) * test_ratio))
            test_size = min(test_size, len(shuffled_recipes) - 1)

            train_size = len(shuffled_recipes) - test_size

            train_data[user_id] = shuffled_recipes[:train_size]
            test_data[user_id] = shuffled_recipes[train_size:]

        users_to_remove = [user_id for user_id, recipes in test_data.items() if not recipes]
        for user_id in users_to_remove:
            del test_data[user_id]
            del train_data[user_id]

        return train_data, test_data

    def create_id_mapping(self, user_interactions):
        """创建用户和菜谱ID的映射（从0开始）"""
        all_users = set()
        all_recipes = set()

        for user_id, recipes in user_interactions.items():
            all_users.add(user_id)
            all_recipes.update(recipes)

        user_mapping = {user_id: idx for idx, user_id in enumerate(sorted(all_users))}
        recipe_mapping = {recipe_id: idx for idx, recipe_id in enumerate(sorted(all_recipes))}

        return user_mapping, recipe_mapping

    def save_mapped_data(self, data, user_mapping, recipe_mapping, file_path):
        """保存映射后的数据"""
        with open(file_path, 'w', encoding='utf-8') as f:
            for user_id, recipes in data.items():
                if recipes:
                    mapped_user_id = user_mapping[user_id]
                    mapped_recipes = [str(recipe_mapping[recipe_id]) for recipe_id in recipes]
                    f.write(f"{mapped_user_id} {' '.join(mapped_recipes)}\n")

    def process_user_interactions(self):
        """处理用户互动数据并生成训练测试集"""
        print("=== 步骤2: 处理用户互动数据并生成训练测试集 ===")

        user_interactions = {}
        input_file = os.path.join(self.output_dirs['intermediate_data'], 'user_interactions.txt')
        with open(input_file, 'r', encoding='utf-8') as f:
            for line in f:
                if line.strip():
                    parts = line.strip().split()
                    user_id = parts[0]
                    recipe_ids = parts[1:]
                    user_interactions[user_id] = recipe_ids

        print(f"原始用户数: {len(user_interactions)}")

        # 使用配置的过滤参数
        filtered_interactions = self.iterative_filter(
            user_interactions,
            min_user_interactions=self.config['min_user_interactions'],
            min_recipe_interactions=self.config['min_recipe_interactions']
        )
        print(f"过滤后用户数: {len(filtered_interactions)}")

        all_recipes = set()
        total_interactions = 0
        for recipes in filtered_interactions.values():
            total_interactions += len(recipes)
            all_recipes.update(recipes)
        print(f"过滤后菜谱数: {len(all_recipes)}")
        print(f"总交互数: {total_interactions}")

        # ── 存储 density，供流水线结束时重命名运行目录使用 ──
        _denom = len(filtered_interactions) * len(all_recipes)
        self._interaction_density = total_interactions / _denom if _denom > 0 else 0.0
        print(f"交互密度: {self._interaction_density:.6f}")

        # 使用配置的测试集比例
        train_data, test_data = self.split_train_test_safe(
            filtered_interactions,
            test_ratio=self.config['test_ratio']
        )

        train_users_with_data = sum(1 for recipes in train_data.values() if recipes)
        test_users_with_data = sum(1 for recipes in test_data.values() if recipes)
        print(f"训练集用户数: {train_users_with_data}")
        print(f"测试集用户数: {test_users_with_data}")

        user_mapping, recipe_mapping = self.create_id_mapping(filtered_interactions)
        print(f"用户ID映射数: {len(user_mapping)}")
        print(f"菜谱ID映射数: {len(recipe_mapping)}")

        # 保存到训练数据目录
        self.save_mapped_data(
            train_data,
            user_mapping,
            recipe_mapping,
            os.path.join(self.output_dirs['train_data'], 'train.txt')
        )
        self.save_mapped_data(
            test_data,
            user_mapping,
            recipe_mapping,
            os.path.join(self.output_dirs['train_data'], 'test.txt')
        )

        with open(os.path.join(self.output_dirs['intermediate_data'], 'user_mapping.txt'), 'w', encoding='utf-8') as f:
            for original_id, mapped_id in sorted(user_mapping.items(), key=lambda x: x[1]):
                f.write(f"{original_id} {mapped_id}\n")

        with open(os.path.join(self.output_dirs['intermediate_data'], 'recipe_mapping.txt'), 'w', encoding='utf-8') as f:
            for original_id, mapped_id in sorted(recipe_mapping.items(), key=lambda x: x[1]):
                f.write(f"{original_id} {mapped_id}\n")

        print("训练集和测试集已生成并保存!")

    def generate_new_recipe_json(self):
        """根据ID映射生成新的菜谱JSON文件"""
        print("=== 步骤3: 生成处理后的菜谱数据 ===")

        def read_recipe_mapping(file_path):
            mapping = {}
            with open(file_path, 'r', encoding='utf-8') as f:
                for line in f:
                    if line.strip():
                        original_id, new_id = line.strip().split()
                        mapping[int(new_id)] = int(original_id)
            return mapping

        id_mapping = read_recipe_mapping(os.path.join(self.output_dirs['intermediate_data'], 'recipe_mapping.txt'))

        with open(self.config['input_files']['recipes'], 'r', encoding='utf-8') as f:
            recipes_data = json.load(f)

        recipes_dict = {int(recipe['id']): recipe for recipe in recipes_data}

        new_recipes = []
        for new_id in sorted(id_mapping.keys()):
            original_id = id_mapping[new_id]
            if original_id in recipes_dict:
                recipe = recipes_dict[original_id]
                new_recipes.append(recipe)

        with open(os.path.join(self.output_dirs['intermediate_data'], '新菜谱.json'), 'w', encoding='utf-8') as f:
            json.dump(new_recipes, f, ensure_ascii=False, indent=2)

        print("新的菜谱JSON文件已生成!")

    def build_knowledge_graph(self):
        """构建知识图谱"""
        print("=== 步骤4: 构建知识图谱 ===")

        class RecipeKGBuilder:
            def __init__(self):
                self.edge_types = {
                    'author_of': 0, 'has_main_ingredient': 1, 'has_sub_ingredient': 2,
                    'has_seasoning': 3, 'belongs_to_category': 4, 'has_difficulty': 5,
                    'has_taste': 6, 'has_cooking_method': 7, 'has_duration': 8,
                    'uses_cookware': 9, 'has_dish_type': 10, 'has_reply_level': 11,
                    'has_like_level': 12, 'has_rating_level': 13
                }

                self.node_types = {
                    'id': 0, 'author': 1, 'ingredient': 2, 'category': 3,
                    'difficulty': 4, 'taste': 5, 'cooking_method': 6,
                    'duration': 7, 'cookware': 8, 'dish_type': 9,
                    'reply_level': 10, 'like_level': 11, 'rating_level': 12
                }

                self.nodes = {}
                self.edges = []
                self.node_counter = 0

                self.recipe_nodes = {}
                self.author_nodes = {}
                self.ingredient_nodes = {}
                self.category_nodes = {}
                self.difficulty_nodes = {}
                self.taste_nodes = {}
                self.cooking_method_nodes = {}
                self.duration_nodes = {}
                self.cookware_nodes = {}
                self.dish_type_nodes = {}
                self.reply_level_nodes = {}
                self.like_level_nodes = {}
                self.rating_level_nodes = {}
                self.output_dirs = config.OUTPUT_DIRS

            def classify_number_level(self, num_str, level_type='default'):
                try:
                    num = int(num_str) if num_str else 0
                except (ValueError, TypeError):
                    num = 0

                if level_type == 'reply':
                    if num == 0: return "回复数_0"
                    elif 1 <= num <= 10: return "回复数_1-10"
                    elif 11 <= num <= 50: return "回复数_11-50"
                    elif 51 <= num <= 100: return "回复数_51-100"
                    elif 101 <= num <= 200: return "回复数_101-200"
                    else: return "回复数_200+"

                elif level_type == 'like':
                    if num == 0: return "点赞数_0"
                    elif 1 <= num <= 5: return "点赞数_1-5"
                    elif 6 <= num <= 20: return "点赞数_6-20"
                    elif 21 <= num <= 50: return "点赞数_21-50"
                    elif 51 <= num <= 100: return "点赞数_51-100"
                    elif 101 <= num <= 200: return "点赞数_101-200"
                    else: return "点赞数_200+"

                elif level_type == 'rating':
                    if num == 0: return "评分数_0"
                    elif 1 <= num <= 20: return "评分数_1-20"
                    elif 21 <= num <= 50: return "评分数_21-50"
                    elif 51 <= num <= 100: return "评分数_51-100"
                    elif 101 <= num <= 200: return "评分数_101-200"
                    elif 201 <= num <= 500: return "评分数_201-500"
                    else: return "评分数_500+"
                return "未知等级"

            def get_or_create_node(self, name, node_type, node_dict):
                if name not in node_dict:
                    node_dict[name] = self.node_counter
                    self.nodes[self.node_counter] = (name, node_type)
                    self.node_counter += 1
                return node_dict[name]

            def extract_dish_type(self, title):
                dish_type_patterns = {
                    '红烧': '红烧类', '糖醋': '糖醋类', '清蒸': '蒸制类', '蒸': '蒸制类',
                    '炒': '炒制类', '爆炒': '爆炒类', '煎': '煎制类', '炸': '油炸类',
                    '烤': '烤制类', '炖': '炖制类', '煲': '煲类', '凉拌': '凉菜类',
                    '汤': '汤类', '羹': '羹类', '粥': '粥类', '饭': '米饭类',
                    '面': '面食类', '饺子': '饺子类', '包子': '包子类', '饼': '饼类'
                }

                dish_types = []
                for pattern, dish_type in dish_type_patterns.items():
                    if pattern in title:
                        dish_types.append(dish_type)

                if not dish_types:
                    return ['其他类']
                return dish_types

            def build_kg_from_recipes(self, recipes_data):
                all_nodes_by_type = {
                    'id': [], 'author': set(), 'ingredient': set(), 'category': set(),
                    'difficulty': set(), 'taste': set(), 'cooking_method': set(),
                    'duration': set(), 'cookware': set(), 'dish_type': set(),
                    'reply_level': set(), 'like_level': set(), 'rating_level': set()
                }

                for recipe in recipes_data:
                    if recipe['id']:
                        all_nodes_by_type['id'].append(recipe['id'])

                    if recipe.get('author'):
                        all_nodes_by_type['author'].add(recipe['author'])

                    ingredients = recipe.get('ingredients', {})
                    for ingredient in ingredients.get('main_ingredients', {}).keys():
                        all_nodes_by_type['ingredient'].add(ingredient)
                    for ingredient in ingredients.get('sub_ingredients', {}).keys():
                        all_nodes_by_type['ingredient'].add(ingredient)
                    for ingredient in ingredients.get('seasoning_ingredients', {}).keys():
                        all_nodes_by_type['ingredient'].add(ingredient)

                    for category in recipe.get('categories', []):
                        all_nodes_by_type['category'].add(category)

                    properties = recipe.get('properties', {})
                    if properties.get('难度'):
                        all_nodes_by_type['difficulty'].add(properties['难度'])
                    if properties.get('口味'):
                        all_nodes_by_type['taste'].add(properties['口味'])
                    if properties.get('工艺'):
                        all_nodes_by_type['cooking_method'].add(properties['工艺'])
                    if properties.get('耗时'):
                        all_nodes_by_type['duration'].add(properties['耗时'])

                    if recipe.get('cookware') and recipe['cookware'].strip():
                        cookwares = recipe['cookware'].split('、')
                        for cookware in cookwares:
                            all_nodes_by_type['cookware'].add(cookware)

                    dish_types = self.extract_dish_type(recipe['title'])
                    for dish_type in dish_types:
                        all_nodes_by_type['dish_type'].add(dish_type)

                    reply_level = self.classify_number_level(recipe.get('replynum', '0'), 'reply')
                    all_nodes_by_type['reply_level'].add(reply_level)

                    like_level = self.classify_number_level(recipe.get('likenum', 0), 'like')
                    all_nodes_by_type['like_level'].add(like_level)

                    rating_level = self.classify_number_level(recipe.get('ratnum', '0'), 'rating')
                    all_nodes_by_type['rating_level'].add(rating_level)

                current_id = 0
                node_type_ranges = {}

                for node_type in ['id', 'author', 'ingredient','category', 'difficulty',
                                  'taste', 'cooking_method', 'duration', 'cookware', 'dish_type',
                                  'reply_level', 'like_level', 'rating_level']:
                    start_id = current_id
                    nodes_of_type = list(all_nodes_by_type[node_type])

                    for node_name in nodes_of_type:
                        if node_type == 'id':
                            self.recipe_nodes[node_name] = current_id
                        elif node_type == 'author':
                            self.author_nodes[node_name] = current_id
                        elif node_type == 'ingredient':
                            self.ingredient_nodes[node_name] = current_id
                        elif node_type == 'category':
                            self.category_nodes[node_name] = current_id
                        elif node_type == 'difficulty':
                            self.difficulty_nodes[node_name] = current_id
                        elif node_type == 'taste':
                            self.taste_nodes[node_name] = current_id
                        elif node_type == 'cooking_method':
                            self.cooking_method_nodes[node_name] = current_id
                        elif node_type == 'duration':
                            self.duration_nodes[node_name] = current_id
                        elif node_type == 'cookware':
                            self.cookware_nodes[node_name] = current_id
                        elif node_type == 'dish_type':
                            self.dish_type_nodes[node_name] = current_id
                        elif node_type == 'reply_level':
                            self.reply_level_nodes[node_name] = current_id
                        elif node_type == 'like_level':
                            self.like_level_nodes[node_name] = current_id
                        elif node_type == 'rating_level':
                            self.rating_level_nodes[node_name] = current_id

                        self.nodes[current_id] = (node_name, self.node_types[node_type])
                        current_id += 1

                    end_id = current_id - 1
                    node_type_ranges[node_type] = (start_id, end_id, len(nodes_of_type))

                self.node_counter = current_id
                self.node_type_ranges = node_type_ranges

                columns = [':ID', 'val', ':LABEL']
                Type_id_to_type = {v: k for k, v in self.node_types.items()}
                df = pd.DataFrame(columns=columns)
                for node_id, (node_name, node_type) in self.nodes.items():
                    if node_type == 0:
                        node_type = 'recipe'
                        recipe_title = next((r['title'] for r in recipes_data if str(r['id']) == str(node_name)), node_name)
                        df.loc[node_id] = [node_id, recipe_title, node_type]
                    else:
                        df.loc[node_id] = [node_id, node_name, Type_id_to_type[node_type]]

                with open(os.path.join(self.output_dirs['neo4j_data'], 'nodes.csv'), 'w', newline='', encoding='utf-8') as f:
                    df.to_csv(f, index=False, encoding='utf-8')

                for recipe in recipes_data:
                    recipe_id = self.recipe_nodes[recipe['id']]

                    if recipe.get('author'):
                        author_id = self.author_nodes[recipe['author']]
                        self.edges.append((author_id, self.edge_types['author_of'], recipe_id))

                    ingredients = recipe.get('ingredients', {})
                    for ingredient in ingredients.get('main_ingredients', {}).keys():
                        ingredient_id = self.ingredient_nodes[ingredient]
                        self.edges.append((recipe_id, self.edge_types['has_main_ingredient'], ingredient_id))
                    for ingredient in ingredients.get('sub_ingredients', {}).keys():
                        ingredient_id = self.ingredient_nodes[ingredient]
                        self.edges.append((recipe_id, self.edge_types['has_sub_ingredient'], ingredient_id))
                    for ingredient in ingredients.get('seasoning_ingredients', {}).keys():
                        ingredient_id = self.ingredient_nodes[ingredient]
                        self.edges.append((recipe_id, self.edge_types['has_seasoning'], ingredient_id))

                    for category in recipe.get('categories', []):
                        category_id = self.category_nodes[category]
                        self.edges.append((recipe_id, self.edge_types['belongs_to_category'], category_id))

                    properties = recipe.get('properties', {})
                    if properties.get('难度'):
                        difficulty_id = self.difficulty_nodes[properties['难度']]
                        self.edges.append((recipe_id, self.edge_types['has_difficulty'], difficulty_id))
                    if properties.get('口味'):
                        taste_id = self.taste_nodes[properties['口味']]
                        self.edges.append((recipe_id, self.edge_types['has_taste'], taste_id))
                    if properties.get('工艺'):
                        cooking_method_id = self.cooking_method_nodes[properties['工艺']]
                        self.edges.append((recipe_id, self.edge_types['has_cooking_method'], cooking_method_id))
                    if properties.get('耗时'):
                        duration_id = self.duration_nodes[properties['耗时']]
                        self.edges.append((recipe_id, self.edge_types['has_duration'], duration_id))

                    if recipe.get('cookware') and recipe['cookware'].strip():
                        cookwares = recipe['cookware'].split('、')
                        for cookware in cookwares:
                            cookware = cookware.strip()
                            if cookware and cookware in self.cookware_nodes:
                                cookware_id = self.cookware_nodes[cookware]
                                self.edges.append((recipe_id, self.edge_types['uses_cookware'], cookware_id))

                    dish_types = self.extract_dish_type(recipe['title'])
                    for dish_type in dish_types:
                        dish_type_id = self.dish_type_nodes[dish_type]
                        self.edges.append((recipe_id, self.edge_types['has_dish_type'], dish_type_id))

                    reply_level = self.classify_number_level(recipe.get('replynum', '0'), 'reply')
                    reply_level_id = self.reply_level_nodes[reply_level]
                    self.edges.append((recipe_id, self.edge_types['has_reply_level'], reply_level_id))

                    like_level = self.classify_number_level(recipe.get('likenum', 0), 'like')
                    like_level_id = self.like_level_nodes[like_level]
                    self.edges.append((recipe_id, self.edge_types['has_like_level'], like_level_id))

                    rating_level = self.classify_number_level(recipe.get('ratnum', '0'), 'rating')
                    rating_level_id = self.rating_level_nodes[rating_level]
                    self.edges.append((recipe_id, self.edge_types['has_rating_level'], rating_level_id))

            def save_kg_final(self, output_path):
                with open(output_path, 'w', encoding='utf-8') as f:
                    for head, relation, tail in self.edges:
                        f.write(f"{head} {relation} {tail}\n")

            def get_statistics(self):
                stats = {
                    'total_nodes': len(self.nodes),
                    'total_edges': len(self.edges),
                    'edge_type_counts': Counter(edge[1] for edge in self.edges),
                    'node_type_counts': {
                        'recipes': len(self.recipe_nodes),
                        'authors': len(self.author_nodes),
                        'ingredients': len(self.ingredient_nodes),
                        'categories': len(self.category_nodes),
                        'difficulties': len(self.difficulty_nodes),
                        'tastes': len(self.taste_nodes),
                        'cooking_methods': len(self.cooking_method_nodes),
                        'durations': len(self.duration_nodes),
                        'cookwares': len(self.cookware_nodes),
                        'dish_types': len(self.dish_type_nodes),
                        'reply_levels': len(self.reply_level_nodes),
                        'like_levels': len(self.like_level_nodes),
                        'rating_levels': len(self.rating_level_nodes)
                    }
                }
                return stats

        with open(os.path.join(self.output_dirs['intermediate_data'], "新菜谱.json"), 'r', encoding='utf-8') as f:
            sample_recipes = json.load(f)

        kg_builder = RecipeKGBuilder()
        kg_builder.output_dirs = self.output_dirs   # 覆盖为当前运行目录下的路径
        kg_builder.build_kg_from_recipes(sample_recipes)

        stats = kg_builder.get_statistics()
        print("=== 知识图谱统计信息 ===")
        print(f"总节点数: {stats['total_nodes']}")
        print(f"总边数: {stats['total_edges']}")
        print("\n各类型节点数量:")
        for node_type, count in stats['node_type_counts'].items():
            print(f"  {node_type}: {count}")
        print("\n各类型边数量:")
        for edge_type_id, count in stats['edge_type_counts'].items():
            edge_type_name = [k for k, v in kg_builder.edge_types.items() if v == edge_type_id][0]
            print(f"  {edge_type_name}({edge_type_id}): {count}")

        kg_builder.save_kg_final(os.path.join(self.output_dirs['train_data'], 'kg_final.txt'))
        print(f"\n知识图谱已保存到 {os.path.join(self.output_dirs['train_data'], 'kg_final.txt')}")

    def convert_to_neo4j_format(self):
        """转换为Neo4j格式"""
        print("=== 步骤5: 转换为Neo4j格式 ===")

        edge_types = {
            'author_of': 0, 'has_main_ingredient': 1, 'has_sub_ingredient': 2,
            'has_seasoning': 3, 'belongs_to_category': 4, 'has_difficulty': 5,
            'has_taste': 6, 'has_cooking_method': 7, 'has_duration': 8,
            'uses_cookware': 9, 'has_dish_type': 10, 'has_reply_level': 11,
            'has_like_level': 12, 'has_rating_level': 13
        }
        dict_edge_types = {v: k for k, v in edge_types.items()}
        columns = [':START_ID', ':TYPE', ':END_ID']
        rows = []

        with open(os.path.join(self.output_dirs['train_data'], 'kg_final.txt'), 'r', encoding='utf-8') as f:
            for line in tqdm(f):
                parts = line.strip().split()
                parts[1] = dict_edge_types[int(parts[1])]
                rows.append(parts)

        df = pd.DataFrame(rows, columns=columns)
        df.to_csv(os.path.join(self.output_dirs['neo4j_data'], 'kg_final.csv'), index=False, encoding='utf-8')
        print(f"Neo4j关系文件已生成: {os.path.join(self.output_dirs['neo4j_data'], 'kg_final.csv')}")

    def create_user_interaction_graph(self):
        """创建用户互动图谱"""
        print("=== 步骤6: 创建用户互动图谱 ===")

        df = pd.read_csv(self.config['input_files']['comments'], low_memory=False)
        user_dict = (
            df.drop_duplicates(subset='author_id', keep='first')
              .set_index('author_id')['author_name']
              .to_dict()
        )

        user_map = {}
        with open(os.path.join(self.output_dirs['intermediate_data'], 'user_mapping.txt'), 'r', encoding='utf-8') as f:
            for line in f:
                original_id, mapped_id = line.strip().split()
                user_map[int(mapped_id)] = int(original_id)

        recipe_map = {}
        with open(os.path.join(self.output_dirs['intermediate_data'], 'recipe_mapping.txt'), 'r', encoding='utf-8') as f:
            for line in f:
                original_id, mapped_id = line.strip().split()
                recipe_map[int(mapped_id)] = int(original_id)

        file_name = os.path.join(self.output_dirs['neo4j_data'], 'user_nodes.csv')
        columns = [':ID', 'name', ':LABEL']
        rows = [[k, user_dict[v], 'User'] for k,v in user_map.items()]
        df = pd.DataFrame(rows, columns=columns)
        df.to_csv(file_name, index=False, encoding='utf-8')
        print(f"用户节点文件已生成: {file_name}")

        file_name = os.path.join(self.output_dirs['neo4j_data'], 'user_interactions.csv')
        columns = [':START_ID(user)', ':END_ID', ':TYPE']
        user_interactions = []

        with open(os.path.join(self.output_dirs['train_data'], "train.txt"),'r',encoding='utf-8') as f:
            for line in f:
                ids = line.strip().split()
                user_id = int(ids[0])
                recipe_ids = [int(x) for x in ids[1:]]
                for recipe_id in recipe_ids:
                    user_interactions.append([user_id, recipe_id, 'interact'])

        with open(os.path.join(self.output_dirs['train_data'], "test.txt"),'r',encoding='utf-8') as f:
            for line in f:
                ids = line.strip().split()
                user_id = int(ids[0])
                recipe_ids = [int(x) for x in ids[1:]]
                for recipe_id in recipe_ids:
                    user_interactions.append([user_id, recipe_id, 'interact'])

        df = pd.DataFrame(user_interactions, columns=columns)
        df.to_csv(file_name, index=False, encoding='utf-8')
        print(f"用户互动关系文件已生成: {file_name}")

    # ══════════════════════════════════════════════════════════════════
    # 步骤7：导出传统模型训练所需的四个文件
    # ══════════════════════════════════════════════════════════════════
    def export_traditional_model_data(self):
        """
        导出传统模型训练所需的四个文件，字段与原始输入文件完全一致：

          1. 互动.csv
             原始字段: old_recipe_id, old_user_id, mapped_recipe_id, mapped_user_id, comment
             来源: config['input_files']['interactions']，过滤为仅保留存活用户/菜谱的行

          2. 用户ID映射关系.csv
             原始字段: original_user_id, mapped_user_id

          3. 菜谱ID映射关系.csv
             原始字段: original_recipe_id, mapped_recipe_id

          4. 菜谱RAW.csv
             原始字段: id, title, author, main_ingredients, sub_ingredients,
                       seasoning_ingredients, cookware, categories, 口味, 工艺,
                       耗时, 难度, image_url, replynum, likenum, ratnum,
                       supe_uid, rc, islikecount
             来源: config['input_files']['recipes']（原始CSV），过滤为仅保留存活菜谱
        """
        print("=== 步骤7: 导出传统模型数据 ===")

        # ── 读取过滤后的 user/recipe 原始ID集合（用于筛选原始文件） ──
        survived_users = set()
        with open(os.path.join(self.output_dirs['intermediate_data'], 'user_mapping.txt'), 'r', encoding='utf-8') as f:
            for line in f:
                if line.strip():
                    original_id, _ = line.strip().split()
                    survived_users.add(str(original_id))

        survived_recipes = set()
        with open(os.path.join(self.output_dirs['intermediate_data'], 'recipe_mapping.txt'), 'r', encoding='utf-8') as f:
            for line in f:
                if line.strip():
                    original_id, _ = line.strip().split()
                    survived_recipes.add(str(original_id))

        # ── 1. 互动.csv ──
        # 严格字段: old_recipe_id, old_user_id, mapped_recipe_id, mapped_user_id, comment
        # 直接读取原始互动文件，过滤后输出，字段顺序与原文件完全一致
        df_inter_raw = pd.read_csv(self.config['input_files']['interactions'], low_memory=False, dtype=str)
        df_inter_filtered = df_inter_raw[
            df_inter_raw['old_user_id'].isin(survived_users) &
            df_inter_raw['old_recipe_id'].isin(survived_recipes)
        ].reset_index(drop=True)

        interaction_out = os.path.join(self.traditional_model_dir, '互动.csv')
        df_inter_filtered.to_csv(interaction_out, index=False, encoding='utf-8')
        print(f"  已生成: {interaction_out}  ({len(df_inter_filtered)} 条交互记录)")

        # ── 2. 用户ID映射关系.csv ──
        # 严格字段: original_user_id, mapped_user_id
        user_mapping_rows = []
        with open(os.path.join(self.output_dirs['intermediate_data'], 'user_mapping.txt'), 'r', encoding='utf-8') as f:
            for line in f:
                if line.strip():
                    original_id, mapped_id = line.strip().split()
                    user_mapping_rows.append({'original_user_id': original_id, 'mapped_user_id': mapped_id})

        df_user_map = pd.DataFrame(user_mapping_rows, columns=['original_user_id', 'mapped_user_id'])
        user_map_out = os.path.join(self.traditional_model_dir, '用户ID映射关系.csv')
        df_user_map.to_csv(user_map_out, index=False, encoding='utf-8')
        print(f"  已生成: {user_map_out}  ({len(df_user_map)} 个用户)")

        # ── 3. 菜谱ID映射关系.csv ──
        # 严格字段: original_recipe_id, mapped_recipe_id
        recipe_mapping_rows = []
        with open(os.path.join(self.output_dirs['intermediate_data'], 'recipe_mapping.txt'), 'r', encoding='utf-8') as f:
            for line in f:
                if line.strip():
                    original_id, mapped_id = line.strip().split()
                    recipe_mapping_rows.append({'original_recipe_id': original_id, 'mapped_recipe_id': mapped_id})

        df_recipe_map = pd.DataFrame(recipe_mapping_rows, columns=['original_recipe_id', 'mapped_recipe_id'])
        recipe_map_out = os.path.join(self.traditional_model_dir, '菜谱ID映射关系.csv')
        df_recipe_map.to_csv(recipe_map_out, index=False, encoding='utf-8')
        print(f"  已生成: {recipe_map_out}  ({len(df_recipe_map)} 个菜谱)")

        # ── 4. 菜谱RAW.csv ──
        # 严格字段（与原始配方CSV完全一致，列顺序不变）:
        #   id, title, author, main_ingredients, sub_ingredients,
        #   seasoning_ingredients, cookware, categories, 口味, 工艺,
        #   耗时, 难度, image_url, replynum, likenum, ratnum,
        #   supe_uid, rc, islikecount
        # 直接读取原始配方CSV，按存活菜谱ID过滤，保留全部原始列
        RAW_COLUMNS = [
            'id', 'title', 'author', 'main_ingredients', 'sub_ingredients',
            'seasoning_ingredients', 'cookware', 'categories', '口味', '工艺',
            '耗时', '难度', 'image_url', 'replynum', 'likenum', 'ratnum',
            'supe_uid', 'rc', 'islikecount'
        ]

        df_recipe_raw = pd.read_csv(self.config['input_files']['recipes_csv'], low_memory=False, dtype=str)
        df_recipe_filtered = df_recipe_raw[
            df_recipe_raw['id'].isin(survived_recipes)
        ][RAW_COLUMNS].reset_index(drop=True)

        recipe_raw_out = os.path.join(self.traditional_model_dir, '菜谱RAW.csv')
        df_recipe_filtered.to_csv(recipe_raw_out, index=False, encoding='utf-8')
        print(f"  已生成: {recipe_raw_out}  ({len(df_recipe_filtered)} 个菜谱)")

        print(f"\n传统模型数据已全部保存至: {self.traditional_model_dir}")

    # ──────────────────────────────────────────────────────────────────

    def run_complete_pipeline(self):
        """运行完整的数据管道"""
        print("=" * 50)
        print("开始运行完整数据管道")
        print(f"临时运行目录: {self._run_dir}")
        print("=" * 50)

        steps = [
            self.process_interactions,
            self.process_user_interactions,
            self.generate_new_recipe_json,
            self.build_knowledge_graph,
            self.convert_to_neo4j_format,
            self.create_user_interaction_graph,
            self.export_traditional_model_data,
        ]

        for i, step in enumerate(steps, 1):
            print(f"\n[{i}/{len(steps)}] " + "=" * 40)
            try:
                result = step()
                if result is False:
                    print(f"步骤 {i} 执行失败!")
                    return False
            except Exception as e:
                print(f"步骤 {i} 执行出错: {str(e)}")
                return False

        # ── 全部步骤完成后，将运行目录重命名为含真实 density 的最终名称 ──
        self._finalize_run_folder()

        print("\n" + "=" * 50)
        print("完整数据管道执行完成!")
        print(f"所有输出位于: {self._run_dir}")
        print("\n生成的文件:")
        print(f"  {self._run_dir}/intermediate_data/user_interactions.txt")
        print(f"  {self._run_dir}/train_data/train.txt")
        print(f"  {self._run_dir}/train_data/test.txt")
        print(f"  {self._run_dir}/intermediate_data/user_mapping.txt")
        print(f"  {self._run_dir}/intermediate_data/recipe_mapping.txt")
        print(f"  {self._run_dir}/intermediate_data/新菜谱.json")
        print(f"  {self._run_dir}/neo4j_data/nodes.csv")
        print(f"  {self._run_dir}/train_data/kg_final.txt")
        print(f"  {self._run_dir}/neo4j_data/kg_final.csv")
        print(f"  {self._run_dir}/neo4j_data/user_nodes.csv")
        print(f"  {self._run_dir}/neo4j_data/user_interactions.csv")
        print(f"  {self._run_dir}/traditional_model_data/互动.csv")
        print(f"  {self._run_dir}/traditional_model_data/用户ID映射关系.csv")
        print(f"  {self._run_dir}/traditional_model_data/菜谱ID映射关系.csv")
        print(f"  {self._run_dir}/traditional_model_data/菜谱RAW.csv")
        print("=" * 50)

        return True


def main():
    pipeline = CompleteDataPipeline()
    pipeline.run_complete_pipeline()


if __name__ == "__main__":
    main()