package com.foodrecommend.letmecook.search;

import com.foodrecommend.letmecook.config.SearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeSearchIndexInitializer {

    private final SearchProperties searchProperties;
    private final RecipeSearchService recipeSearchService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSearchIndex() {
        if (searchProperties.isMysql() || !searchProperties.getEs().isAutoCreateIndex()) {
            return;
        }
        try {
            recipeSearchService.ensureIndexReady();
        } catch (Exception e) {
            log.warn("应用启动时初始化搜索索引失败: {}", e.getMessage());
        }
    }
}
