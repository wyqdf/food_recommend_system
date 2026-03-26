package com.foodrecommend.letmecook.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private String engine = "auto";
    private final Es es = new Es();

    public boolean useElasticsearch() {
        return !"mysql".equalsIgnoreCase(engine);
    }

    public boolean isAuto() {
        return "auto".equalsIgnoreCase(engine);
    }

    public boolean isMysql() {
        return "mysql".equalsIgnoreCase(engine);
    }

    public boolean isElasticsearch() {
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
