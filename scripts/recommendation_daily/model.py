from __future__ import annotations

import torch
import torch.nn as nn
import torch.nn.functional as F


class CkeDailyModel(nn.Module):
    def __init__(self, n_users: int, n_items: int, n_entities: int, n_relations: int, hidden_dim: int, backbone_dim: int) -> None:
        super().__init__()
        self.user_embedding = nn.Embedding(max(n_users, 1), hidden_dim)
        self.item_id_embedding = nn.Embedding(max(n_items, 1), hidden_dim)
        self.entity_embedding = nn.Embedding(max(n_entities, 1), hidden_dim)
        self.relation_embedding = nn.Embedding(max(n_relations, 1), hidden_dim)
        self.text_projection = nn.Linear(backbone_dim, hidden_dim)
        self.image_projection = nn.Linear(backbone_dim, hidden_dim)
        self.fusion_layer = nn.Sequential(
            nn.Linear(hidden_dim * 3, hidden_dim),
            nn.GELU(),
            nn.Linear(hidden_dim, hidden_dim),
        )
        self._reset_parameters()

    def _reset_parameters(self) -> None:
        for module in self.modules():
            if isinstance(module, nn.Embedding):
                nn.init.xavier_uniform_(module.weight)
            elif isinstance(module, nn.Linear):
                nn.init.xavier_uniform_(module.weight)
                nn.init.zeros_(module.bias)

    def item_representation(self, item_ids: torch.Tensor, text_features: torch.Tensor, image_features: torch.Tensor) -> torch.Tensor:
        item_base = self.item_id_embedding(item_ids)
        text_projected = self.text_projection(text_features)
        image_projected = self.image_projection(image_features)
        fused = self.fusion_layer(torch.cat([item_base, text_projected, image_projected], dim=-1))
        return F.normalize(item_base + text_projected + image_projected + fused, dim=-1)

    def user_representation(self, user_ids: torch.Tensor) -> torch.Tensor:
        return F.normalize(self.user_embedding(user_ids), dim=-1)

    def interaction_loss(
        self,
        user_ids: torch.Tensor,
        pos_item_ids: torch.Tensor,
        neg_item_ids: torch.Tensor,
        pos_text_features: torch.Tensor,
        pos_image_features: torch.Tensor,
        neg_text_features: torch.Tensor,
        neg_image_features: torch.Tensor,
        weights: torch.Tensor,
    ) -> torch.Tensor:
        user_vec = self.user_representation(user_ids)
        pos_vec = self.item_representation(pos_item_ids, pos_text_features, pos_image_features)
        neg_vec = self.item_representation(neg_item_ids, neg_text_features, neg_image_features)
        pos_scores = (user_vec * pos_vec).sum(dim=-1)
        neg_scores = (user_vec * neg_vec).sum(dim=-1)
        return (-F.logsigmoid(pos_scores - neg_scores) * weights).mean()

    def kg_loss(
        self,
        head_item_ids: torch.Tensor,
        relation_ids: torch.Tensor,
        tail_entity_ids: torch.Tensor,
        head_text_features: torch.Tensor,
        head_image_features: torch.Tensor,
    ) -> torch.Tensor:
        head_vec = self.item_representation(head_item_ids, head_text_features, head_image_features)
        relation_vec = F.normalize(self.relation_embedding(relation_ids), dim=-1)
        tail_vec = F.normalize(self.entity_embedding(tail_entity_ids), dim=-1)
        return torch.norm(head_vec + relation_vec - tail_vec, p=2, dim=-1).mean()
