#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
配置文件 - 管理数据管道的所有路径参数
"""

import os

# 项目根目录
PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))

# 原始数据目录
DATA_DIR = os.path.join(PROJECT_ROOT, 'data')

# 输出目录
OUTPUT_DIRS = {
    # 训练模型使用的数据
    'train_data': os.path.join(PROJECT_ROOT, 'output', 'train_data'),

    # 中间过渡数据
    'intermediate_data': os.path.join(PROJECT_ROOT, 'output', 'intermediate_data'),

    # Neo4j导入数据
    'neo4j_data': os.path.join(PROJECT_ROOT, 'output', 'neo4j_data')
}

# 输入文件路径
INPUT_FILES = {
    'interactions': os.path.join(DATA_DIR, '互动.csv'),
    'recipes': os.path.join(DATA_DIR, '菜谱RAW.json'),
    'comments': os.path.join(DATA_DIR, '评论RAW.csv'),
    'recipes_csv': os.path.join(DATA_DIR, '菜谱RAW.csv'),
}

# 确保所有输出目录存在
def create_output_dirs():
    """创建所有必要的输出目录"""
    for dir_path in OUTPUT_DIRS.values():
        os.makedirs(dir_path, exist_ok=True)

# 配置参数
CONFIG = {
    # 过滤参数
    'min_user_interactions': 10,
    'min_recipe_interactions': 10,

    # 训练测试集分割比例
    'test_ratio': 0.2,

    # 输出目录
    'output_dirs': OUTPUT_DIRS,

    # 输入文件
    'input_files': INPUT_FILES
}