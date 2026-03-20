import os
import pickle
import random
import json
import collections
from time import time

import numpy as np
import pandas as pd
import torch
from PIL import Image
from sklearn.feature_extraction.text import CountVectorizer


class DataLoaderCKEFull:
    """
    DataLoader for CKEFull — 美食天下 multimodal dataset.

    Expected directory layout  (args.data_dir / args.data_name):
        train.txt               each line: user_id  item_id  item_id …
        test.txt                same format
        kg_final.txt            each line: head  relation  tail
        recipe_mapping.txt      each line: original_id  mapped_id
        user_mapping.txt        same format
        菜谱RAW.json             JSON list of recipe dicts
        text_data_cache.pkl     (optional) pre-built (n_items, n_vocab) matrix
        images/                 <original_recipe_id>.jpg  (64×64)
    """

    def __init__(self, args, logging):
        self.args         = args
        self.data_dir     = os.path.join(args.data_dir, args.data_name)
        self.image_height = getattr(args, 'image_height', 64)
        self.image_width  = getattr(args, 'image_width',  64)

        self.cf_batch_size   = args.cf_batch_size
        self.kg_batch_size   = args.kg_batch_size
        self.sdae_batch_size = getattr(args, 'sdae_batch_size', args.cf_batch_size)
        self.scae_batch_size = getattr(args, 'scae_batch_size', 32)
        self.test_batch_size = args.test_batch_size

        # ── 1. ID mappings ────────────────────────────────────────────────────
        logging.info('[DataLoader] Loading ID mappings …')
        self.recipe_id_map = self._load_id_map('recipe_mapping.txt')  # orig → mapped
        self.user_id_map   = self._load_id_map('user_mapping.txt')
        self._mapped_to_orig_recipe = {v: k for k, v in self.recipe_id_map.items()}

        # ── 2. CF train / test ────────────────────────────────────────────────
        logging.info('[DataLoader] Loading CF interactions …')
        self.train_user_dict, n_users_train, n_items_train = \
            self._load_cf_file('train.txt')
        self.test_user_dict,  n_users_test,  n_items_test  = \
            self._load_cf_file('test.txt')

        self.n_users    = max(n_users_train, n_users_test)
        self.n_items    = max(n_items_train, n_items_test)
        self.n_cf_train = sum(len(v) for v in self.train_user_dict.values())
        self.n_cf_test  = sum(len(v) for v in self.test_user_dict.values())

        # ── 3. Knowledge Graph ────────────────────────────────────────────────
        logging.info('[DataLoader] Loading KG …')
        self._load_kg('kg_final.txt')

        # ── 4. Text features (SDAE input) ─────────────────────────────────────
        logging.info('[DataLoader] Loading text features …')
        self._load_text_features(
            cache_file='text_data_cache.pkl',
            json_file ='菜谱RAW.json',
            max_vocab = getattr(args, 'n_vocab', 2000),
        )

        # ── 5. Image directory (SCAE input) ───────────────────────────────────
        self.image_dir = os.path.join(self.data_dir, 'images')
        self.num_img_workers = getattr(args, 'num_img_workers', 16)  # 加一个线程数参数

        # ── 6. 全量预加载图像到内存 ───────────────────────────────────────────
        logging.info('[DataLoader] 正在多线程预加载所有图片到内存 (64x64) …')
        self._build_image_cache()
        self.print_info(logging)

    # ─────────────────────────────────────────── private helpers ────────────

    def _p(self, filename):
        """Return full path inside data_dir."""
        return os.path.join(self.data_dir, filename)

    def _load_id_map(self, filename):
        """Return dict  original_id (int) → mapped_id (int)."""
        id_map = {}
        with open(self._p(filename), 'r', encoding='utf-8') as f:
            for line in f:
                parts = line.strip().split()
                if len(parts) >= 2:
                    id_map[int(parts[0])] = int(parts[1])
        return id_map

    def _load_cf_file(self, filename):
        """
        Load train.txt / test.txt.
        Each line: user_id  item_id  item_id  …

        Returns
        -------
        user_dict   dict  uid → list[item_id]
        n_users     int
        n_items     int
        """
        user_dict = collections.defaultdict(list)
        max_user  = 0
        max_item  = 0
        with open(self._p(filename), 'r', encoding='utf-8') as f:
            for line in f:
                parts = list(map(int, line.strip().split()))
                if len(parts) < 2:
                    continue
                uid   = parts[0]
                items = parts[1:]
                user_dict[uid].extend(items)
                max_user = max(max_user, uid)
                if items:
                    max_item = max(max_item, max(items))
        return dict(user_dict), max_user + 1, max_item + 1

    def _load_kg(self, filename):
        """
        Load kg_final.txt  (h  r  t  per line).
        Automatically appends inverse triples with shifted relation IDs.
        """
        triples = []
        with open(self._p(filename), 'r', encoding='utf-8') as f:
            for line in f:
                parts = line.strip().split()
                if len(parts) >= 3:
                    triples.append((int(parts[0]), int(parts[1]), int(parts[2])))

        kg_fwd = pd.DataFrame(triples, columns=['h', 'r', 't'])
        n_base = int(kg_fwd['r'].max()) + 1

        kg_inv       = kg_fwd.rename(columns={'h': 't', 't': 'h'}).copy()
        kg_inv['r'] += n_base

        self.kg_data     = pd.concat([kg_fwd, kg_inv],
                                      axis=0, ignore_index=True, sort=False)
        self.n_relations = int(self.kg_data['r'].max()) + 1
        self.n_entities  = int(max(self.kg_data['h'].max(),
                                    self.kg_data['t'].max())) + 1
        self.n_kg_data   = len(self.kg_data)

        self.kg_dict       = collections.defaultdict(list)
        self.relation_dict = collections.defaultdict(list)
        for _, row in self.kg_data.iterrows():
            h, r, t = int(row['h']), int(row['r']), int(row['t'])
            self.kg_dict[h].append((t, r))
            self.relation_dict[r].append((h, t))

    def _recipe_to_text(self, recipe):
        """Concatenate all informative text fields from a recipe dict."""
        parts = []
        for field in ('title', 'description', 'tips', 'cookware'):
            v = recipe.get(field, '')
            if v:
                parts.append(str(v))
        ingr = recipe.get('ingredients', {})
        for section in ('main_ingredients', 'sub_ingredients',
                        'seasoning_ingredients'):
            for k in ingr.get(section, {}):
                parts.append(str(k))
        for step in recipe.get('steps', []):
            parts.append(str(step))
        for cat in recipe.get('categories', []):
            parts.append(str(cat))
        for k, v in recipe.get('properties', {}).items():
            parts.append(f'{k}{v}')
        return ' '.join(parts)

    def _load_text_features(self, cache_file, json_file, max_vocab):
        """
        Load or build the (n_items, n_vocab) normalised BoW matrix.

        Priority:
          1. text_data_cache.pkl  — if it already contains a usable matrix
          2. 菜谱RAW.json         — build character-level BoW from scratch
                                    then save back to cache
        Sets:
          self.n_vocab            int
          self.item_text_features np.float32  (n_items, n_vocab)
        """
        cache_path = self._p(cache_file)

        # ── try cache ─────────────────────────────────────────────────────────
        if os.path.isfile(cache_path):
            with open(cache_path, 'rb') as f:
                cache = pickle.load(f)

            # cache may be the bare matrix or a dict wrapping it
            mat = None
            if isinstance(cache, np.ndarray):
                mat = cache
            elif isinstance(cache, dict):
                for key in ('text_features', 'features', 'matrix'):
                    if key in cache and isinstance(cache[key], np.ndarray):
                        mat = cache[key]
                        break

            if mat is not None:
                mat = mat.astype(np.float32)
                # pad or trim rows to match n_items
                if mat.shape[0] < self.n_items:
                    pad = np.zeros(
                        (self.n_items - mat.shape[0], mat.shape[1]),
                        dtype=np.float32)
                    mat = np.vstack([mat, pad])
                else:
                    mat = mat[:self.n_items]
                row_max = mat.max(axis=1, keepdims=True)
                row_max[row_max == 0] = 1.0
                self.item_text_features = mat / row_max
                self.n_vocab = mat.shape[1]
                return   # ← fast path done

        # ── build from JSON ───────────────────────────────────────────────────
        with open(self._p(json_file), 'r', encoding='utf-8') as f:
            recipes = json.load(f)

        texts = [''] * self.n_items
        for rec in recipes:
            orig_id = rec.get('id')
            if orig_id in self.recipe_id_map:
                mid = self.recipe_id_map[orig_id]
                if mid < self.n_items:
                    texts[mid] = self._recipe_to_text(rec)

        vectorizer = CountVectorizer(
            analyzer='char', max_features=max_vocab, min_df=1)
        vectorizer.fit(texts)
        self.n_vocab = len(vectorizer.vocabulary_)

        mat = vectorizer.transform(texts).toarray().astype(np.float32)
        row_max = mat.max(axis=1, keepdims=True)
        row_max[row_max == 0] = 1.0
        self.item_text_features = mat / row_max

        # persist for next run
        with open(cache_path, 'wb') as f:
            pickle.dump({'text_features': self.item_text_features}, f)

        # ─────────────────────────────────────────── image helpers ──────────────

    def _build_image_cache(self):
        """
        利用多线程，在初始化时一次性将所有图片读取并存入 Numpy 数组中。
        形状为 (n_items, 3, H, W)。
        """
        self.item_visual_features = np.zeros(
            (self.n_items, 3, self.image_height, self.image_width),
            dtype=np.float32
        )

        def load_single_img(mapped_id):
            orig_id = self._mapped_to_orig_recipe.get(mapped_id, mapped_id)
            path = os.path.join(self.image_dir, f'{int(orig_id)}.jpg')

            if os.path.isfile(path):
                try:
                    img = Image.open(path).convert('RGB')
                    img = img.resize((self.image_width, self.image_height), Image.BILINEAR)
                    # 归一化到 [0, 1] 并转换维度为 (3, H, W)
                    arr = np.asarray(img, dtype=np.float32) / 255.0
                    return mapped_id, arr.transpose(2, 0, 1)
                except Exception:
                    pass
            # 失败或文件不存在返回纯黑图片
            return mapped_id, np.zeros((3, self.image_height, self.image_width), dtype=np.float32)

        from concurrent.futures import ThreadPoolExecutor, as_completed
        with ThreadPoolExecutor(max_workers=self.num_img_workers) as executor:
            futures = [executor.submit(load_single_img, i) for i in range(self.n_items)]

            for future in as_completed(futures):
                idx, arr = future.result()
                self.item_visual_features[idx] = arr

    def get_item_visual_features(self, mapped_item_ids):
        """
        【极速切片版】直接从内存中的矩阵提取图片特征，0 I/O 延迟。
        mapped_item_ids : array-like of ints (mapped recipe IDs)
        Returns         : np.float32  (n, 3, H, W)
        """
        # 利用 numpy 高级索引直接切片
        return self.item_visual_features[mapped_item_ids]

    # ─────────────────────────────────────────── batch generators ───────────

    def generate_cf_batch(self, user_dict, batch_size):
        """
        Sample a CF mini-batch with uniform negative sampling.

        Returns (user_ids, pos_item_ids, neg_item_ids) — LongTensor each.
        """
        exist_users = list(user_dict.keys())
        batch_users = (random.sample(exist_users, batch_size)
                       if batch_size <= len(exist_users)
                       else random.choices(exist_users, k=batch_size))

        pos_items, neg_items = [], []
        for u in batch_users:
            pos_set = set(user_dict[u])
            pos_items.append(random.choice(list(pos_set)))
            while True:
                neg = random.randint(0, self.n_items - 1)
                if neg not in pos_set:
                    break
            neg_items.append(neg)

        return (torch.LongTensor(batch_users),
                torch.LongTensor(pos_items),
                torch.LongTensor(neg_items))

    def generate_kg_batch(self, kg_dict, batch_size, n_entities):
        """
        Sample a KG mini-batch (h, r, pos_t, neg_t).

        Returns four LongTensors.
        """
        exist_heads = list(kg_dict.keys())
        batch_heads = (random.sample(exist_heads, batch_size)
                       if batch_size <= len(exist_heads)
                       else random.choices(exist_heads, k=batch_size))

        relations, pos_tails, neg_tails = [], [], []
        for h in batch_heads:
            t, r    = random.choice(kg_dict[h])
            pos_set = {tt for tt, _ in kg_dict[h]}
            relations.append(r)
            pos_tails.append(t)
            while True:
                neg_t = random.randint(0, n_entities - 1)
                if neg_t not in pos_set:
                    break
            neg_tails.append(neg_t)

        return (torch.LongTensor(batch_heads),
                torch.LongTensor(relations),
                torch.LongTensor(pos_tails),
                torch.LongTensor(neg_tails))

    def generate_sdae_batch(self, batch_size, dropout=0.2):
        """
        Sample a (masked, clean) text batch for SDAE training.

        Returns (masked_textual, textual) — FloatTensor (B, n_vocab).
        """
        idx   = np.random.choice(self.n_items, batch_size, replace=True)
        clean = self.item_text_features[idx]
        mask  = (np.random.rand(*clean.shape) >= dropout).astype(np.float32)
        return (torch.FloatTensor(clean * mask),
                torch.FloatTensor(clean))

    def generate_scae_batch(self, batch_size, dropout=0.2):
        """
        Sample a (masked, clean) image batch for SCAE training.

        Returns (masked_visual, visual) — FloatTensor (B, 3, H, W).
        """
        idx   = np.random.choice(self.n_items, batch_size, replace=True)
        clean = self.get_item_visual_features(idx)
        mask  = (np.random.rand(*clean.shape) >= dropout).astype(np.float32)
        return (torch.FloatTensor(clean * mask),
                torch.FloatTensor(clean))

    def get_cf_item_embeddings(self, pos_ids_tensor, neg_ids_tensor):
        """
        Fetch text + visual features for a CF batch.

        Returns
        -------
        pos_text, neg_text     FloatTensor (B, n_vocab)
        pos_visual, neg_visual FloatTensor (B, 3, H, W)
        """
        pos_ids = pos_ids_tensor.numpy()
        neg_ids = neg_ids_tensor.numpy()
        return (
            torch.FloatTensor(self.item_text_features[pos_ids]),
            torch.FloatTensor(self.item_text_features[neg_ids]),
            torch.FloatTensor(self.get_item_visual_features(pos_ids)),
            torch.FloatTensor(self.get_item_visual_features(neg_ids)),
        )

    def get_all_item_embeddings(self):
        """
        Return text + visual features for ALL items (used at evaluation time).

        Returns
        -------
        text_embed   FloatTensor (n_items, n_vocab)
        visual_embed FloatTensor (n_items, 3, H, W)
        """
        all_ids = np.arange(self.n_items)
        return (
            torch.FloatTensor(self.item_text_features),
            torch.FloatTensor(self.get_item_visual_features(all_ids)),
        )

    # ─────────────────────────────────────────── logging ────────────────────

    def print_info(self, logging):
        logging.info('=' * 55)
        logging.info('DataLoaderCKEFull  —  %s' % self.data_dir)
        logging.info('  n_users      : %d' % self.n_users)
        logging.info('  n_items      : %d' % self.n_items)
        logging.info('  n_entities   : %d' % self.n_entities)
        logging.info('  n_relations  : %d' % self.n_relations)
        logging.info('  n_vocab      : %d' % self.n_vocab)
        logging.info('  n_cf_train   : %d' % self.n_cf_train)
        logging.info('  n_cf_test    : %d' % self.n_cf_test)
        logging.info('  n_kg_data    : %d' % self.n_kg_data)
        logging.info('  image_dir    : %s' % self.image_dir)
        logging.info('=' * 55)