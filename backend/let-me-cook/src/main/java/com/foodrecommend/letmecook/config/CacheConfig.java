package com.foodrecommend.letmecook.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("categories", Duration.ofMinutes(30), 16),
                buildCache("recommend_categories", Duration.ofMinutes(30), 16),
                buildCache("recipe_list", Duration.ofMinutes(5), 256),
                buildCache("recommendations", Duration.ofMinutes(5), 256),
                buildCache("recipe_detail", Duration.ofMinutes(10), 1024),
                buildCache("similar_recipes", Duration.ofMinutes(10), 512),
                buildCache("search_suggestions", Duration.ofMinutes(3), 256),
                buildCache("search_results", Duration.ofMinutes(2), 512)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, Duration ttl, long maximumSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maximumSize)
                .initialCapacity((int) Math.min(maximumSize, 32))
                .build());
    }
}
