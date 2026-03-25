package com.foodrecommend.letmecook.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private String engine = "mysql";
    private final Es es = new Es();

    public boolean useElasticsearch() {
        return "elasticsearch".equalsIgnoreCase(engine);
    }

    @Data
    public static class Es {
        private String indexAlias = "recipes_search";
        private String indexName = "recipes_search_v2";
        private int batchSize = 500;
        private boolean autoCreateIndex = true;
    }
}
