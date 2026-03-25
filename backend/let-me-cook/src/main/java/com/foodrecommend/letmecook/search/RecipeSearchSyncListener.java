package com.foodrecommend.letmecook.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeSearchSyncListener {

    private final RecipeSearchService recipeSearchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRecipeSearchSync(RecipeSearchSyncEvent event) {
        if (event == null || event.getRecipeIds() == null || event.getRecipeIds().isEmpty()) {
            return;
        }

        if (event.getAction() == RecipeSearchSyncEvent.SyncAction.DELETE) {
            recipeSearchService.deleteRecipes(event.getRecipeIds());
        } else {
            recipeSearchService.upsertRecipes(event.getRecipeIds());
        }
        log.debug("Recipe search sync completed. action={}, size={}", event.getAction(), event.getRecipeIds().size());
    }
}
