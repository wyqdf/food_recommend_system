package com.foodrecommend.letmecook.search;

import com.foodrecommend.letmecook.config.SearchProperties;
import com.foodrecommend.letmecook.dto.admin.SearchReindexStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeSearchReindexService {

    private final SearchProperties searchProperties;
    private final RecipeSearchDataLoader recipeSearchDataLoader;
    private final RecipeSearchService recipeSearchService;

    private final ReentrantLock rebuildLock = new ReentrantLock();
    private final Object statusMonitor = new Object();
    private final SearchReindexStatusDTO status = new SearchReindexStatusDTO();

    public String startRebuild() {
        if (!rebuildLock.tryLock()) {
            return "搜索索引重建任务正在执行中";
        }

        synchronized (statusMonitor) {
            status.setRunning(true);
            status.setTotal(0);
            status.setProcessed(0);
            status.setSuccess(0);
            status.setFailed(0);
            status.setStartedAt(LocalDateTime.now());
            status.setFinishedAt(null);
            status.setLastError(null);
            status.setPhase("building");
            fillBaseStatus(status);
        }

        CompletableFuture.runAsync(() -> {
            try {
                runRebuild();
            } catch (Exception e) {
                log.error("重建搜索索引失败", e);
                synchronized (statusMonitor) {
                    status.setLastError(resolveErrorMessage(e));
                    status.setPhase("failed");
                }
            } finally {
                synchronized (statusMonitor) {
                    status.setRunning(false);
                    status.setFinishedAt(LocalDateTime.now());
                    if (status.getLastError() == null && recipeSearchService.getLastSyncError() != null) {
                        status.setLastError(recipeSearchService.getLastSyncError());
                    }
                    if (status.getLastError() == null && !"completed".equals(status.getPhase())) {
                        status.setPhase("completed");
                    }
                }
                rebuildLock.unlock();
            }
        });

        return "搜索索引重建任务已开始";
    }

    public SearchReindexStatusDTO getStatus() {
        synchronized (statusMonitor) {
            SearchReindexStatusDTO snapshot = copyStatus();
            fillBaseStatus(snapshot);
            if (snapshot.getLastError() == null) {
                snapshot.setLastError(recipeSearchService.getLastSyncError());
            }
            return snapshot;
        }
    }

    private void runRebuild() {
        recipeSearchService.ensureIndexReady();
        if (!recipeSearchService.isIndexReady()) {
            synchronized (statusMonitor) {
                status.setLastError(recipeSearchService.getLastSyncError());
                status.setPhase("failed");
            }
            return;
        }

        String targetIndex = recipeSearchService.getTargetIndexName();
        recipeSearchService.recreateTargetIndex();

        long total = recipeSearchDataLoader.countPublicRecipes();
        synchronized (statusMonitor) {
            status.setTotal(total);
        }

        int batchSize = Math.max(searchProperties.getEs().getBatchSize(), 1);
        int offset = 0;
        while (true) {
            var documents = recipeSearchDataLoader.loadPublicRecipesBatch(offset, batchSize);
            if (documents.isEmpty()) {
                break;
            }

            RecipeSearchService.BulkSyncResult result = recipeSearchService.bulkIndexDocuments(documents, targetIndex);
            synchronized (statusMonitor) {
                status.setProcessed(status.getProcessed() + documents.size());
                status.setSuccess(status.getSuccess() + result.success());
                status.setFailed(status.getFailed() + result.failed());
                if (result.firstError() != null && status.getLastError() == null) {
                    status.setLastError(result.firstError());
                }
            }
            offset += documents.size();
        }

        synchronized (statusMonitor) {
            if (status.getFailed() > 0 || status.getLastError() != null) {
                status.setPhase("failed");
                return;
            }
        }

        synchronized (statusMonitor) {
            status.setPhase("swapping");
        }
        recipeSearchService.swapAliasToTargetIndex();
        synchronized (statusMonitor) {
            status.setPhase("completed");
            status.setCurrentIndex(recipeSearchService.getCurrentIndexName());
        }
    }

    private SearchReindexStatusDTO copyStatus() {
        SearchReindexStatusDTO snapshot = new SearchReindexStatusDTO();
        snapshot.setEngine(status.getEngine());
        snapshot.setIndexAlias(status.getIndexAlias());
        snapshot.setIndexName(status.getIndexName());
        snapshot.setCurrentIndex(status.getCurrentIndex());
        snapshot.setTargetIndex(status.getTargetIndex());
        snapshot.setPhase(status.getPhase());
        snapshot.setRunning(status.isRunning());
        snapshot.setTotal(status.getTotal());
        snapshot.setProcessed(status.getProcessed());
        snapshot.setSuccess(status.getSuccess());
        snapshot.setFailed(status.getFailed());
        snapshot.setStartedAt(status.getStartedAt());
        snapshot.setFinishedAt(status.getFinishedAt());
        snapshot.setLastError(status.getLastError());
        return snapshot;
    }

    private void fillBaseStatus(SearchReindexStatusDTO target) {
        target.setEngine(searchProperties.getEngine());
        target.setIndexAlias(searchProperties.getEs().getIndexAlias());
        target.setIndexName(searchProperties.getEs().getIndexName());
        target.setCurrentIndex(recipeSearchService.getCurrentIndexName());
        target.setTargetIndex(recipeSearchService.getTargetIndexName());
        if (target.getPhase() == null) {
            target.setPhase("idle");
        }
    }

    private String resolveErrorMessage(Exception e) {
        if (e == null) {
            return "Unknown error";
        }
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }
}
