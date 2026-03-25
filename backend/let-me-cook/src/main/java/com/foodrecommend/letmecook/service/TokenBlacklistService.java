package com.foodrecommend.letmecook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistService {

    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public void addToBlacklist(String token) {
        blacklist.add(token);
        log.info("Token 已加入黑名单：{}", token.substring(0, Math.min(20, token.length())));
    }

    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }

    public void removeFromBlacklist(String token) {
        blacklist.remove(token);
    }

    public void clear() {
        blacklist.clear();
    }

    public int size() {
        return blacklist.size();
    }
}
