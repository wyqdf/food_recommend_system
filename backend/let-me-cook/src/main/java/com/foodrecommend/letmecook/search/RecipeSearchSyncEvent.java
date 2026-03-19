package com.foodrecommend.letmecook.search;

import lombok.Getter;

import java.util.List;

@Getter
public class RecipeSearchSyncEvent {

    private final SyncAction action;
    private final List<Integer> recipeIds;

    private RecipeSearchSyncEvent(SyncAction action, List<Integer> recipeIds) {
        this.action = action;
        this.recipeIds = recipeIds;
    }

    public static RecipeSearchSyncEvent upsert(List<Integer> recipeIds) {
        return new RecipeSearchSyncEvent(SyncAction.UPSERT, recipeIds);
    }

    public static RecipeSearchSyncEvent delete(List<Integer> recipeIds) {
        return new RecipeSearchSyncEvent(SyncAction.DELETE, recipeIds);
    }

    public enum SyncAction {
        UPSERT,
        DELETE
    }
}
