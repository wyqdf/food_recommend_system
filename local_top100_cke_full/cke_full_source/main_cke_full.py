"""
main_cke_full.py
================
Training entry-point for CKEFull (KG + Text-SDAE + Visual-SCAE + CF-BPR).

Requires model/CKE.py to be the FULL multimodal version with:
  __init__(args, n_users, n_items, n_entities, n_relations, n_vocab,
           user_pre_embed=None, item_pre_embed=None)
  calc_loss(h, r, pos_t, neg_t,
            masked_textual_embed, textual_embed,
            masked_visual_embed, visual_embed,
            user_ids, item_pos_ids, item_neg_ids,
            item_pos_textual_embed, item_neg_textual_embed,
            item_pos_visual_embed, item_neg_visual_embed)
  calc_score(user_ids, item_ids, item_textual_embed, item_visual_embed)

Usage
-----
    python main_cke_full.py --data_name meishitianxia_v1
"""

import os
import sys
import random
import logging
from time import time

import numpy as np
import pandas as pd
import torch
import torch.optim as optim
from tqdm import tqdm

from model.CKE_full import CKE
from data_loader.loader_cke_full import DataLoaderCKEFull
from parser.paser_all import parse_cke_full_args


# ──────────────────────────────────────────────────── helpers ────────────────

def _setup_logging(save_dir):
    os.makedirs(save_dir, exist_ok=True)
    log_path = os.path.join(save_dir, 'train.log')
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s  %(message)s',
        handlers=[
            logging.FileHandler(log_path, encoding='utf-8'),
            logging.StreamHandler(sys.stdout),
        ])


def early_stopping(recall_list, stopping_steps):
    best = max(recall_list)
    steps_since_best = len(recall_list) - 1 - recall_list[::-1].index(best)
    return best, steps_since_best >= stopping_steps


def save_model(model, save_dir, epoch, best_epoch):
    if best_epoch >= 0:
        old = os.path.join(save_dir, f'model_epoch{best_epoch:04d}.pt')
        if os.path.exists(old):
            os.remove(old)
    path = os.path.join(save_dir, f'model_epoch{epoch:04d}.pt')
    torch.save(model.state_dict(), path)
    logging.info(f'Model saved → {path}')


def load_model(model, path):
    model.load_state_dict(torch.load(path, map_location='cpu'))
    logging.info(f'Model loaded from {path}')
    return model


# ──────────────────────────────────────────────────── metrics ────────────────

def calc_metrics_at_k(scores_tensor, train_user_dict, test_user_dict,
                      batch_user_ids_tensor, Ks):
    k_max   = max(Ks)
    scores  = scores_tensor.cpu().numpy()
    uids    = batch_user_ids_tensor.cpu().numpy()
    metrics = {k: {'precision': [], 'recall': [], 'ndcg': []} for k in Ks}

    for i, uid in enumerate(uids):
        row      = scores[i].copy()
        row[train_user_dict.get(int(uid), [])] = -np.inf
        top_k    = np.argsort(row)[::-1][:k_max]
        test_set = set(test_user_dict.get(int(uid), []))
        if not test_set:
            continue
        hits = np.array([int(it in test_set) for it in top_k], dtype=np.float32)

        for k in Ks:
            h    = hits[:k]
            p    = h.sum() / k
            r    = h.sum() / len(test_set)
            dcg  = sum(hh / np.log2(idx + 2) for idx, hh in enumerate(h))
            idcg = sum(1.0 / np.log2(idx + 2)
                       for idx in range(min(k, len(test_set))))
            ndcg = dcg / idcg if idcg > 0 else 0.0
            metrics[k]['precision'].append(p)
            metrics[k]['recall'].append(r)
            metrics[k]['ndcg'].append(ndcg)

    return metrics


# ──────────────────────────────────────────────────── evaluation ─────────────

@torch.no_grad()
def evaluate(model, dataloader, Ks, device):
    model.eval()

    logging.info('[Eval] Pre-loading all item embeddings …')
    # get text (n_items, n_vocab) and visual (n_items, 3, H, W)
    item_text_all, item_vis_all = dataloader.get_all_item_embeddings()
    item_text_all = item_text_all.to(device)
    item_vis_all  = item_vis_all.to(device)
    item_ids_all  = torch.arange(
        dataloader.n_items, dtype=torch.long, device=device)

    test_users = list(dataloader.test_user_dict.keys())
    bsz        = dataloader.test_batch_size
    batches    = [test_users[i:i+bsz] for i in range(0, len(test_users), bsz)]

    all_scores   = []
    metric_names = ['precision', 'recall', 'ndcg']
    agg = {k: {m: [] for m in metric_names} for k in Ks}

    with tqdm(total=len(batches), desc='Evaluating') as pbar:
        for batch_uids in batches:
            uid_tensor = torch.LongTensor(batch_uids).to(device)

            # calc_score(user_ids, item_ids, item_textual_embed, item_visual_embed)
            batch_scores = model(
                uid_tensor, item_ids_all,
                item_text_all, item_vis_all,
                is_train=False,
            )   # (B, n_items)

            bm = calc_metrics_at_k(
                batch_scores,
                dataloader.train_user_dict,
                dataloader.test_user_dict,
                uid_tensor, Ks)

            all_scores.append(batch_scores.cpu().numpy())
            for k in Ks:
                for m in metric_names:
                    agg[k][m].extend(bm[k][m])
            pbar.update(1)

    for k in Ks:
        for m in metric_names:
            agg[k][m] = float(np.mean(agg[k][m])) if agg[k][m] else 0.0

    return np.concatenate(all_scores, axis=0), agg


# ──────────────────────────────────────────────────── training ───────────────

def train(args):
    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    torch.cuda.manual_seed_all(args.seed)

    _setup_logging(args.save_dir)
    logging.info(args)

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    logging.info(f'Device: {device}')

    # ── data ─────────────────────────────────────────────────────────────────
    data = DataLoaderCKEFull(args, logging)

    # sync n_vocab / image dims from loader into args so CKE.__init__ can read them
    args.n_vocab      = data.n_vocab
    args.image_height = data.image_height
    args.image_width  = data.image_width

    # ── model ─────────────────────────────────────────────────────────────────
    user_pre_embed = item_pre_embed = None
    if args.use_pretrain == 1:
        if hasattr(data, 'user_pre_embed'):
            user_pre_embed = torch.tensor(data.user_pre_embed)
        if hasattr(data, 'item_pre_embed'):
            item_pre_embed = torch.tensor(data.item_pre_embed)

    # Full CKE signature:
    # __init__(args, n_users, n_items, n_entities, n_relations, n_vocab,
    #          user_pre_embed=None, item_pre_embed=None)
    model = CKE(
        args,
        data.n_users,
        data.n_items,
        data.n_entities,
        data.n_relations,
        data.n_vocab,
        user_pre_embed,
        item_pre_embed,
    )

    if args.use_pretrain == 2 and args.pretrain_model_path:
        model = load_model(model, args.pretrain_model_path)

    model.to(device)
    logging.info(model)

    optimizer = optim.Adam(model.parameters(), lr=args.lr)

    # ── training state ────────────────────────────────────────────────────────
    Ks           = eval(args.Ks)
    k_min, k_max = min(Ks), max(Ks)
    best_epoch   = -1
    best_recall  = 0.0
    epoch_list   = []
    metrics_list = {k: {'precision': [], 'recall': [], 'ndcg': []} for k in Ks}

    n_batch = data.n_cf_train // data.cf_batch_size + 1

    # ── epoch loop ────────────────────────────────────────────────────────────
    for epoch in range(1, args.n_epoch + 1):
        model.train()
        t0         = time()
        total_loss = 0.0

        for it in range(1, n_batch + 1):
            t_it = time()

            # ── CF batch ──────────────────────────────────────────────────────
            cf_u, cf_pos, cf_neg = data.generate_cf_batch(
                data.train_user_dict, data.cf_batch_size)

            # text (B, n_vocab) and visual (B, 3, H, W) for pos/neg items
            pos_txt, neg_txt, pos_vis, neg_vis = data.get_cf_item_embeddings(
                cf_pos, cf_neg)

            cf_u    = cf_u.to(device)
            cf_pos  = cf_pos.to(device)
            cf_neg  = cf_neg.to(device)
            pos_txt = pos_txt.to(device)
            neg_txt = neg_txt.to(device)
            pos_vis = pos_vis.to(device)
            neg_vis = neg_vis.to(device)

            # ── KG batch ──────────────────────────────────────────────────────
            kg_h, kg_r, kg_pt, kg_nt = data.generate_kg_batch(
                data.kg_dict, data.kg_batch_size, data.n_entities)
            kg_h  = kg_h.to(device)
            kg_r  = kg_r.to(device)
            kg_pt = kg_pt.to(device)
            kg_nt = kg_nt.to(device)

            # ── SDAE batch  (masked, clean) text ──────────────────────────────
            sdae_masked, sdae_clean = data.generate_sdae_batch(
                data.sdae_batch_size, dropout=args.sdae_dropout)
            sdae_masked = sdae_masked.to(device)
            sdae_clean  = sdae_clean.to(device)

            # ── SCAE batch  (masked, clean) image ─────────────────────────────
            scae_masked, scae_clean = data.generate_scae_batch(
                data.scae_batch_size, dropout=args.scae_dropout)
            scae_masked = scae_masked.to(device)
            scae_clean  = scae_clean.to(device)

            # ── forward ───────────────────────────────────────────────────────
            # calc_loss(h, r, pos_t, neg_t,
            #           masked_textual_embed, textual_embed,
            #           masked_visual_embed,  visual_embed,
            #           user_ids, item_pos_ids, item_neg_ids,
            #           item_pos_textual_embed, item_neg_textual_embed,
            #           item_pos_visual_embed,  item_neg_visual_embed)
            loss = model(
                kg_h, kg_r, kg_pt, kg_nt,
                sdae_masked, sdae_clean,
                scae_masked, scae_clean,
                cf_u, cf_pos, cf_neg,
                pos_txt, neg_txt,
                pos_vis, neg_vis,
                is_train=True,
            )

            if torch.isnan(loss):
                logging.error(
                    f'NaN at Epoch {epoch:04d} Iter {it:04d}. Aborting.')
                sys.exit(1)

            loss.backward()
            optimizer.step()
            optimizer.zero_grad()
            total_loss += loss.item()

            if it % args.print_every == 0:
                logging.info(
                    f'Epoch {epoch:04d} | Iter {it:04d}/{n_batch} '
                    f'| t={time()-t_it:.1f}s '
                    f'| loss={loss.item():.4f} '
                    f'| mean={total_loss/it:.4f}')

        logging.info(
            f'Epoch {epoch:04d} done | t={time()-t0:.1f}s '
            f'| mean_loss={total_loss/n_batch:.4f}')

        # ── evaluation ────────────────────────────────────────────────────────
        if (epoch % args.evaluate_every == 0) or (epoch == args.n_epoch):
            t_eval = time()
            _, metrics = evaluate(model, data, Ks, device)
            logging.info(
                f'Eval {epoch:04d} | t={time()-t_eval:.1f}s | '
                f'P@{k_min}={metrics[k_min]["precision"]:.4f}  '
                f'R@{k_min}={metrics[k_min]["recall"]:.4f}  '
                f'NDCG@{k_min}={metrics[k_min]["ndcg"]:.4f} || '
                f'P@{k_max}={metrics[k_max]["precision"]:.4f}  '
                f'R@{k_max}={metrics[k_max]["recall"]:.4f}  '
                f'NDCG@{k_max}={metrics[k_max]["ndcg"]:.4f}')

            epoch_list.append(epoch)
            for k in Ks:
                for m in ('precision', 'recall', 'ndcg'):
                    metrics_list[k][m].append(metrics[k][m])

            best_recall, should_stop = early_stopping(
                metrics_list[k_min]['recall'], args.stopping_steps)

            if should_stop:
                logging.info('Early stopping triggered.')
                break

            if metrics_list[k_min]['recall'].index(best_recall) == len(epoch_list) - 1:
                save_model(model, args.save_dir, epoch, best_epoch)
                best_epoch = epoch

            # ── metrics CSV ───────────────────────────────────────────────────
            rows = {'epoch': epoch_list}
            cols = ['epoch']
            for k in Ks:
                for m in ('precision', 'recall', 'ndcg'):
                    key = f'{m}@{k}'
                    rows[key] = metrics_list[k][m]
                    cols.append(key)
            df = pd.DataFrame(rows)[cols]
            df.to_csv(os.path.join(args.save_dir, 'metrics.tsv'),
                      sep='\t', index=False)

            if best_epoch >= 0:
                best_row = df[df['epoch'] == best_epoch].iloc[0]
                logging.info(
                    'Best (epoch {:04d}): '.format(best_epoch) +
                    '  '.join(f'{c}={best_row[c]:.4f}'
                               for c in cols if c != 'epoch'))


# ──────────────────────────────────────────────────── predict only ───────────

def predict(args):
    _setup_logging(args.save_dir)
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

    data = DataLoaderCKEFull(args, logging)
    args.n_vocab      = data.n_vocab
    args.image_height = data.image_height
    args.image_width  = data.image_width

    model = CKE(args, data.n_users, data.n_items,
                 data.n_entities, data.n_relations, data.n_vocab)
    model = load_model(model, args.pretrain_model_path)
    model.to(device)

    Ks = eval(args.Ks)
    k_min, k_max = min(Ks), max(Ks)

    cf_scores, metrics = evaluate(model, data, Ks, device)
    np.save(os.path.join(args.save_dir, 'cf_scores.npy'), cf_scores)
    print(
        f'Eval: '
        f'P@{k_min}={metrics[k_min]["precision"]:.4f}  '
        f'R@{k_min}={metrics[k_min]["recall"]:.4f}  '
        f'NDCG@{k_min}={metrics[k_min]["ndcg"]:.4f} || '
        f'P@{k_max}={metrics[k_max]["precision"]:.4f}  '
        f'R@{k_max}={metrics[k_max]["recall"]:.4f}  '
        f'NDCG@{k_max}={metrics[k_max]["ndcg"]:.4f}')


# ──────────────────────────────────────────────────── entry ──────────────────

if __name__ == '__main__':
    args = parse_cke_full_args()
    train(args)
    # predict(args)