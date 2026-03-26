package com.foodrecommend.letmecook.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.config.SearchProperties;
import com.foodrecommend.letmecook.dto.RecipeListDTO;
import com.foodrecommend.letmecook.dto.SearchSuggestionDTO;
import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.mapper.RecipeMapper;
import com.foodrecommend.letmecook.service.RecipeListDTOAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeSearchService {

    private static final List<String> PRIMARY_SEARCH_FIELDS = List.of(
            "title^4.0",
            "ingredients^3.0",
            "categories^2.0",
            "author^1.8"
    );
    private static final int TITLE_SUGGEST_QUOTA = 3;
    private static final int INGREDIENT_SUGGEST_QUOTA = 3;
    private static final int CATEGORY_SUGGEST_QUOTA = 1;
    private static final int AUTHOR_SUGGEST_QUOTA = 1;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SearchProperties searchProperties;
    private final RecipeSearchDataLoader recipeSearchDataLoader;
    private final RecipeMapper recipeMapper;
    private final RecipeListDTOAssembler recipeListDTOAssembler;

    private final ReentrantLock indexLock = new ReentrantLock();
    private volatile boolean indexReady;
    private volatile String lastSyncError;

    public PageResult<RecipeListDTO> searchRecipes(String keyword, String sort, int page, int pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        ensureIndexReady();
        ensureV2ReadReady();

        int offset = (safePage - 1) * safePageSize;
        String normalizedSort = normalizeSort(sort);
        JsonNode root = executeSearch(buildPrimarySearchPayload(normalizedKeyword, normalizedSort, offset, safePageSize));
        JsonNode hitsNode = root.path("hits");
        long total = extractTotal(hitsNode.path("total"));
        List<Integer> recipeIds = extractRecipeIds(hitsNode.path("hits"));
        if (total == 0 || recipeIds.isEmpty()) {
            root = executeSearch(buildFallbackSearchPayload(normalizedKeyword, normalizedSort, offset, safePageSize));
            hitsNode = root.path("hits");
            total = extractTotal(hitsNode.path("total"));
            recipeIds = extractRecipeIds(hitsNode.path("hits"));
        }
        if (recipeIds.isEmpty()) {
            return new PageResult<>(List.of(), total, safePage, safePageSize);
        }

        List<Recipe> recipes = recipeMapper.findByIds(recipeIds);
        Map<Integer, Recipe> recipeMap = new HashMap<>(recipes.size());
        for (Recipe recipe : recipes) {
            recipeMap.put(recipe.getId(), recipe);
        }

        List<Recipe> orderedRecipes = new ArrayList<>(recipeIds.size());
        for (Integer recipeId : recipeIds) {
            Recipe recipe = recipeMap.get(recipeId);
            if (recipe != null) {
                orderedRecipes.add(recipe);
            }
        }

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(orderedRecipes);
        return new PageResult<>(list, total, safePage, safePageSize);
    }

    public boolean shouldUseElasticsearch() {
        if (searchProperties.isMysql()) {
            return false;
        }
        try {
            ensureIndexReady();
            ensureV2ReadReady();
            return true;
        } catch (Exception e) {
            log.debug("ES 当前不可读，回退 MySQL：{}", e.getMessage());
            return false;
        }
    }

    public String getActiveEngine() {
        return shouldUseElasticsearch() ? "elasticsearch" : "mysql";
    }

    public List<SearchSuggestionDTO> getSearchSuggestions(String keyword, int limit) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return List.of();
        }

        int safeLimit = Math.min(Math.max(limit, 1), 10);
        ensureIndexReady();
        ensureV2ReadReady();

        JsonNode root = executeSearch(buildSuggestionPayload(normalizedKeyword, safeLimit));
        JsonNode suggestRoot = root.path("suggest");
        if (suggestRoot.isMissingNode() || suggestRoot.isNull()) {
            return List.of();
        }

        LinkedHashMap<String, SearchSuggestionDTO> merged = new LinkedHashMap<>();
        addCompletionSuggestions(merged, suggestRoot, "titleSuggest", "title", "菜谱", TITLE_SUGGEST_QUOTA, safeLimit);
        addCompletionSuggestions(merged, suggestRoot, "ingredientSuggest", "ingredient", "食材", INGREDIENT_SUGGEST_QUOTA, safeLimit);
        addCompletionSuggestions(merged, suggestRoot, "categorySuggest", "category", "分类", CATEGORY_SUGGEST_QUOTA, safeLimit);
        addCompletionSuggestions(merged, suggestRoot, "authorSuggest", "author", "作者", AUTHOR_SUGGEST_QUOTA, safeLimit);
        return merged.values().stream().limit(safeLimit).toList();
    }

    public void upsertRecipes(Collection<Integer> recipeIds) {
        List<Integer> validIds = normalizeIds(recipeIds);
        if (validIds.isEmpty()) {
            return;
        }

        ensureIndexReady();
        if (!indexReady) {
            return;
        }

        String writeTarget = resolveWriteTarget();
        if (!StringUtils.hasText(writeTarget)) {
            return;
        }

        for (Integer recipeId : validIds) {
            try {
                RecipeSearchDocument document = recipeSearchDataLoader.loadPublicRecipe(recipeId);
                if (document == null) {
                    deleteDocument(writeTarget, recipeId);
                } else {
                    indexDocument(writeTarget, document);
                }
                clearLastSyncError();
            } catch (Exception e) {
                markSyncError("同步食谱到 ES 失败，recipeId=" + recipeId, e);
            }
        }
    }

    public void deleteRecipes(Collection<Integer> recipeIds) {
        List<Integer> validIds = normalizeIds(recipeIds);
        if (validIds.isEmpty()) {
            return;
        }

        ensureIndexReady();
        if (!indexReady) {
            return;
        }

        String writeTarget = resolveWriteTarget();
        if (!StringUtils.hasText(writeTarget)) {
            return;
        }

        for (Integer recipeId : validIds) {
            try {
                deleteDocument(writeTarget, recipeId);
                clearLastSyncError();
            } catch (Exception e) {
                markSyncError("从 ES 删除食谱失败，recipeId=" + recipeId, e);
            }
        }
    }

    public BulkSyncResult bulkIndexDocuments(List<RecipeSearchDocument> documents) {
        return bulkIndexDocuments(documents, resolveWriteTarget());
    }

    public BulkSyncResult bulkIndexDocuments(List<RecipeSearchDocument> documents, String indexName) {
        if (documents == null || documents.isEmpty()) {
            return new BulkSyncResult(0, 0, null);
        }

        ensureIndexReady();
        if (!indexReady || !StringUtils.hasText(indexName)) {
            return new BulkSyncResult(0, documents.size(), lastSyncError);
        }

        StringBuilder body = new StringBuilder(documents.size() * 384);
        for (RecipeSearchDocument document : documents) {
            body.append("{\"index\":{\"_index\":\"")
                    .append(indexName)
                    .append("\",\"_id\":\"")
                    .append(document.getId())
                    .append("\"}}\n");
            body.append(serializeDocument(document)).append('\n');
        }

        try {
            JsonNode root = performRequest("POST", "/_bulk?refresh=false", body.toString());
            long failed = 0;
            String firstError = null;
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode indexNode = item.path("index");
                    if (indexNode.has("error")) {
                        failed++;
                        if (firstError == null) {
                            firstError = indexNode.path("error").path("reason").asText(indexNode.path("error").toString());
                        }
                    }
                }
            }
            long success = documents.size() - failed;
            if (failed > 0) {
                lastSyncError = formatErrorMessage("批量写入 ES 时部分失败: " + firstError);
            } else {
                clearLastSyncError();
            }
            return new BulkSyncResult(success, failed, firstError);
        } catch (Exception e) {
            markSyncError("批量写入 ES 失败", e);
            return new BulkSyncResult(0, documents.size(), e.getMessage());
        }
    }

    public void ensureIndexReady() {
        if (indexReady || !searchProperties.getEs().isAutoCreateIndex()) {
            return;
        }
        if (!indexLock.tryLock()) {
            return;
        }
        try {
            if (indexReady) {
                return;
            }
            createIndexIfMissing(targetIndex());
            bindAliasToTargetIfMissingAndPopulated();
            indexReady = true;
            clearLastSyncError();
        } catch (Exception e) {
            indexReady = false;
            markSyncError("初始化 ES V2 索引失败", e);
        } finally {
            indexLock.unlock();
        }
    }

    public String getLastSyncError() {
        return lastSyncError;
    }

    public boolean isIndexReady() {
        return indexReady;
    }

    public String getCurrentIndexName() {
        try {
            JsonNode aliasRoot = getAliasRoot();
            if (aliasRoot == null) {
                return null;
            }

            String fallback = null;
            var fields = aliasRoot.fieldNames();
            while (fields.hasNext()) {
                String indexName = fields.next();
                if (fallback == null) {
                    fallback = indexName;
                }
                JsonNode aliasInfo = aliasRoot.path(indexName).path("aliases").path(indexAlias());
                if (aliasInfo.path("is_write_index").asBoolean(false)) {
                    return indexName;
                }
            }
            return fallback;
        } catch (Exception e) {
            log.debug("读取当前搜索别名绑定失败: {}", e.getMessage());
            return null;
        }
    }

    public String getTargetIndexName() {
        return targetIndex();
    }

    public void recreateTargetIndex() {
        String targetIndex = targetIndex();
        String currentIndex = getCurrentIndexName();
        try {
            if (targetIndex.equals(currentIndex)) {
                clearIndexDocuments(targetIndex);
            } else {
                deleteIndexIfExists(targetIndex);
                createIndex(targetIndex);
            }
            indexReady = true;
            clearLastSyncError();
        } catch (Exception e) {
            indexReady = false;
            markSyncError("重建 V2 目标索引失败", e);
            throw new IllegalStateException("重建 V2 目标索引失败: " + e.getMessage(), e);
        }
    }

    public void swapAliasToTargetIndex() {
        try {
            createIndexIfMissing(targetIndex());
            JsonNode aliasRoot = getAliasRoot();
            List<Map<String, Object>> actions = new ArrayList<>();

            if (aliasRoot != null) {
                aliasRoot.fieldNames().forEachRemaining(indexName ->
                        actions.add(Map.of("remove", Map.of("index", indexName, "alias", indexAlias()))));
            }

            actions.add(Map.of("add", Map.of(
                    "index", targetIndex(),
                    "alias", indexAlias(),
                    "is_write_index", true
            )));
            performRequest("POST", "/_aliases", objectMapper.writeValueAsString(Map.of("actions", actions)));
            clearLastSyncError();
        } catch (Exception e) {
            markSyncError("切换搜索别名到 V2 失败", e);
            throw new IllegalStateException("切换搜索别名到 V2 失败: " + e.getMessage(), e);
        }
    }

    private void ensureV2ReadReady() {
        String currentIndex = getCurrentIndexName();
        if (!StringUtils.hasText(currentIndex)) {
            throw new IllegalStateException("搜索别名尚未绑定到任何索引");
        }
        if (!targetIndex().equals(currentIndex)) {
            throw new IllegalStateException("搜索索引尚未切换到 V2");
        }
    }

    private void createIndexIfMissing(String indexName) throws IOException {
        if (resourceExists("GET", "/" + indexName)) {
            return;
        }
        createIndex(indexName);
    }

    private void createIndex(String indexName) throws IOException {
        String createIndexPayload = """
                {
                  "mappings": {
                    "dynamic": "false",
                    "properties": {
                      "recipeId": { "type": "integer" },
                      "title": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 }
                        }
                      },
                      "author": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 }
                        }
                      },
                      "authorUid": { "type": "keyword" },
                      "categories": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 }
                        }
                      },
                      "ingredients": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 }
                        }
                      },
                      "tasteName": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 }
                        }
                      },
                      "techniqueName": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 }
                        }
                      },
                      "timeCostName": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 }
                        }
                      },
                      "difficultyName": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 }
                        }
                      },
                      "searchText": {
                        "type": "text",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn"
                      },
                      "titleSuggest": {
                        "type": "completion",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "preserve_separators": true,
                        "preserve_position_increments": true,
                        "max_input_length": 100
                      },
                      "ingredientSuggest": {
                        "type": "completion",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "preserve_separators": true,
                        "preserve_position_increments": true,
                        "max_input_length": 100
                      },
                      "categorySuggest": {
                        "type": "completion",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "preserve_separators": true,
                        "preserve_position_increments": true,
                        "max_input_length": 100
                      },
                      "authorSuggest": {
                        "type": "completion",
                        "analyzer": "smartcn",
                        "search_analyzer": "smartcn",
                        "preserve_separators": true,
                        "preserve_position_increments": true,
                        "max_input_length": 100
                      },
                      "likeCount": { "type": "integer" },
                      "status": { "type": "integer" },
                      "createTime": { "type": "date" },
                      "updateTime": { "type": "date" }
                    }
                  }
                }
                """;
        performRequest("PUT", "/" + indexName, createIndexPayload);
    }

    private void deleteIndexIfExists(String indexName) throws IOException {
        if (!resourceExists("GET", "/" + indexName)) {
            return;
        }
        performRequest("DELETE", "/" + indexName, null);
    }

    private void clearIndexDocuments(String indexName) throws IOException {
        if (!resourceExists("GET", "/" + indexName)) {
            createIndex(indexName);
            return;
        }
        performRequest("POST", "/" + indexName + "/_delete_by_query?refresh=true&conflicts=proceed&wait_for_completion=true",
                objectMapper.writeValueAsString(Map.of("query", Map.of("match_all", Map.of()))));
    }

    private void indexDocument(String indexName, RecipeSearchDocument document) throws IOException {
        performRequest("PUT", "/" + indexName + "/_doc/" + document.getId() + "?refresh=false", serializeDocument(document));
    }

    private void deleteDocument(String indexName, Integer recipeId) throws IOException {
        if (recipeId == null) {
            return;
        }
        try {
            performRequest("DELETE", "/" + indexName + "/_doc/" + recipeId, null);
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() != 404) {
                throw e;
            }
        }
    }

    private String resolveWriteTarget() {
        String currentIndex = getCurrentIndexName();
        if (StringUtils.hasText(currentIndex)) {
            return indexAlias();
        }
        ensureIndexReady();
        return indexReady ? targetIndex() : null;
    }

    private JsonNode getAliasRoot() throws IOException {
        if (!resourceExists("GET", "/_alias/" + indexAlias())) {
            return null;
        }
        return performRequest("GET", "/_alias/" + indexAlias(), null);
    }

    private void bindAliasToTargetIfMissingAndPopulated() throws IOException {
        JsonNode aliasRoot = getAliasRoot();
        if (aliasRoot != null) {
            return;
        }
        if (getIndexDocumentCount(targetIndex()) <= 0) {
            return;
        }
        log.info("检测到 ES 目标索引 {} 已有数据且别名 {} 缺失，自动绑定别名。", targetIndex(), indexAlias());
        swapAliasToTargetIndex();
    }

    private long getIndexDocumentCount(String indexName) throws IOException {
        JsonNode root = performRequest("GET", "/" + indexName + "/_count", null);
        return root.path("count").asLong(0L);
    }

    private JsonNode executeSearch(Map<String, Object> payload) {
        try {
            return performRequest("POST", "/" + indexAlias() + "/_search", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new IllegalStateException("执行 ES 搜索失败: " + e.getMessage(), e);
        }
    }

    private JsonNode performRequest(String method, String endpoint, String jsonBody) throws IOException {
        Request request = new Request(method, endpoint);
        if (jsonBody != null) {
            request.setJsonEntity(jsonBody);
        }
        Response response = restClient.performRequest(request);
        try (var content = response.getEntity().getContent()) {
            return objectMapper.readTree(content);
        }
    }

    private boolean resourceExists(String method, String endpoint) throws IOException {
        try {
            restClient.performRequest(new Request(method, endpoint));
            return true;
        } catch (ResponseException e) {
            int status = e.getResponse().getStatusLine().getStatusCode();
            if (status == 404) {
                return false;
            }
            throw e;
        }
    }

    private Map<String, Object> buildPrimarySearchPayload(String keyword, String sort, int offset, int size) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("track_total_hits", true);
        payload.put("from", offset);
        payload.put("size", size);
        payload.put("_source", List.of("recipeId"));
        payload.put("query", Map.of(
                "bool", Map.of(
                        "filter", List.of(Map.of("term", Map.of("status", 1))),
                        "must", List.of(Map.of(
                                "combined_fields", Map.of(
                                        "query", keyword,
                                        "fields", PRIMARY_SEARCH_FIELDS,
                                        "operator", "and"
                                )
                        )),
                        "should", buildPrimaryRerankClauses(keyword)
                )
        ));
        payload.put("sort", buildSortClauses(sort));
        return payload;
    }

    private Map<String, Object> buildFallbackSearchPayload(String keyword, String sort, int offset, int size) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("track_total_hits", true);
        payload.put("from", offset);
        payload.put("size", size);
        payload.put("_source", List.of("recipeId"));
        payload.put("query", Map.of(
                "bool", Map.of(
                        "filter", List.of(Map.of("term", Map.of("status", 1))),
                        "must", List.of(Map.of(
                                "bool", Map.of(
                                        "should", buildFallbackClauses(keyword),
                                        "minimum_should_match", 1
                                )
                        ))
                )
        ));
        payload.put("sort", buildSortClauses(sort));
        return payload;
    }

    private Map<String, Object> buildSuggestionPayload(String keyword, int limit) {
        int titleSize = Math.min(Math.max(limit, TITLE_SUGGEST_QUOTA), 6);
        int ingredientSize = Math.min(Math.max(limit, INGREDIENT_SUGGEST_QUOTA), 6);
        int categorySize = Math.min(Math.max(limit, CATEGORY_SUGGEST_QUOTA), 3);
        int authorSize = Math.min(Math.max(limit, AUTHOR_SUGGEST_QUOTA), 3);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("size", 0);
        payload.put("suggest", Map.of(
                "titleSuggest", Map.of(
                        "prefix", keyword,
                        "completion", Map.of("field", "titleSuggest", "size", titleSize, "skip_duplicates", true)
                ),
                "ingredientSuggest", Map.of(
                        "prefix", keyword,
                        "completion", Map.of("field", "ingredientSuggest", "size", ingredientSize, "skip_duplicates", true)
                ),
                "categorySuggest", Map.of(
                        "prefix", keyword,
                        "completion", Map.of("field", "categorySuggest", "size", categorySize, "skip_duplicates", true)
                ),
                "authorSuggest", Map.of(
                        "prefix", keyword,
                        "completion", Map.of("field", "authorSuggest", "size", authorSize, "skip_duplicates", true)
                )
        ));
        return payload;
    }

    private List<Map<String, Object>> buildPrimaryRerankClauses(String keyword) {
        return List.of(
                termQuery("title.keyword", keyword, 60),
                termQuery("ingredients.keyword", keyword, 48),
                termQuery("categories.keyword", keyword, 24),
                termQuery("author.keyword", keyword, 18),
                matchPhraseQuery("title", keyword, 42),
                matchPhraseQuery("ingredients", keyword, 34),
                matchPhraseQuery("categories", keyword, 20),
                matchPhraseQuery("author", keyword, 16)
        );
    }

    private List<Map<String, Object>> buildFallbackClauses(String keyword) {
        return List.of(
                termQuery("tasteName.keyword", keyword, 30),
                termQuery("techniqueName.keyword", keyword, 26),
                termQuery("timeCostName.keyword", keyword, 22),
                termQuery("difficultyName.keyword", keyword, 22),
                matchPhraseQuery("tasteName", keyword, 20),
                matchPhraseQuery("techniqueName", keyword, 18),
                matchPhraseQuery("timeCostName", keyword, 16),
                matchPhraseQuery("difficultyName", keyword, 16),
                matchQuery("tasteName", keyword, 12, "and"),
                matchQuery("techniqueName", keyword, 10, "and"),
                matchQuery("timeCostName", keyword, 8, "and"),
                matchQuery("difficultyName", keyword, 8, "and")
        );
    }

    private List<Map<String, Object>> buildSortClauses(String sort) {
        return switch (sort) {
            case "hot" -> List.of(sortClause("likeCount"), sortClause("createTime"));
            case "new" -> List.of(sortClause("createTime"));
            default -> List.of(sortClause("_score"), sortClause("likeCount"), sortClause("createTime"));
        };
    }

    private Map<String, Object> termQuery(String field, String keyword, int boost) {
        return Map.of("term", Map.of(field, Map.of("value", keyword, "boost", boost)));
    }

    private Map<String, Object> matchQuery(String field, String keyword, int boost, String operator) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", keyword);
        body.put("boost", boost);
        body.put("operator", operator);
        return Map.of("match", Map.of(field, body));
    }

    private Map<String, Object> matchPhraseQuery(String field, String keyword, int boost) {
        return Map.of("match_phrase", Map.of(field, Map.of("query", keyword, "boost", boost)));
    }

    private Map<String, Object> sortClause(String field) {
        return Map.of(field, Map.of("order", "desc"));
    }

    private long extractTotal(JsonNode totalNode) {
        if (totalNode == null || totalNode.isMissingNode() || totalNode.isNull()) {
            return 0;
        }
        if (totalNode.isNumber()) {
            return totalNode.asLong(0);
        }
        return totalNode.path("value").asLong(0);
    }

    private List<Integer> extractRecipeIds(JsonNode hitsNode) {
        if (hitsNode == null || !hitsNode.isArray()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (JsonNode hit : hitsNode) {
            JsonNode source = hit.path("_source");
            if (source.hasNonNull("recipeId")) {
                ids.add(source.path("recipeId").asInt());
            }
        }
        return ids;
    }

    private void addCompletionSuggestions(LinkedHashMap<String, SearchSuggestionDTO> merged,
                                          JsonNode suggestRoot,
                                          String suggestName,
                                          String type,
                                          String typeLabel,
                                          int typeQuota,
                                          int totalLimit) {
        if (merged.size() >= totalLimit || typeQuota <= 0) {
            return;
        }

        JsonNode entries = suggestRoot.path(suggestName);
        if (!entries.isArray() || entries.isEmpty()) {
            return;
        }

        int addedForType = 0;
        for (JsonNode entry : entries) {
            JsonNode options = entry.path("options");
            if (!options.isArray()) {
                continue;
            }
            for (JsonNode option : options) {
                if (merged.size() >= totalLimit || addedForType >= typeQuota) {
                    return;
                }
                String text = normalizeSuggestionValue(option.path("text").asText(null));
                if (!StringUtils.hasText(text) || merged.containsKey(text)) {
                    continue;
                }
                merged.put(text, new SearchSuggestionDTO(text, type, typeLabel));
                addedForType++;
            }
        }
    }

    private String serializeDocument(RecipeSearchDocument document) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("recipeId", document.getRecipeId());
            body.put("title", document.getTitle());
            body.put("author", document.getAuthor());
            body.put("authorUid", document.getAuthorUid());
            body.put("categories", document.getCategories());
            body.put("ingredients", document.getIngredients());
            body.put("tasteName", document.getTasteName());
            body.put("techniqueName", document.getTechniqueName());
            body.put("timeCostName", document.getTimeCostName());
            body.put("difficultyName", document.getDifficultyName());
            body.put("searchText", document.getSearchText());
            putCompletionField(body, "titleSuggest", document.getTitleSuggestInputs());
            putCompletionField(body, "ingredientSuggest", document.getIngredientSuggestInputs());
            putCompletionField(body, "categorySuggest", document.getCategorySuggestInputs());
            putCompletionField(body, "authorSuggest", document.getAuthorSuggestInputs());
            body.put("likeCount", document.getLikeCount());
            body.put("status", document.getStatus());
            body.put("createTime", document.getCreateTime());
            body.put("updateTime", document.getUpdateTime());
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("序列化搜索文档失败", e);
        }
    }

    private void putCompletionField(Map<String, Object> body, String fieldName, List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        body.put(fieldName, Map.of("input", inputs));
    }

    private void markSyncError(String message, Exception e) {
        String fullMessage = formatErrorMessage(message + ": " + e.getMessage());
        lastSyncError = fullMessage;
        if (searchProperties.useElasticsearch()) {
            log.warn(fullMessage, e);
        } else {
            log.warn(fullMessage);
        }
    }

    private String formatErrorMessage(String message) {
        return "[" + LocalDateTime.now() + "] " + message;
    }

    private void clearLastSyncError() {
        lastSyncError = null;
    }

    private List<Integer> normalizeIds(Collection<Integer> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim().replaceAll("\\s+", " ");
    }

    private String normalizeSort(String sort) {
        if ("hot".equalsIgnoreCase(sort) || "new".equalsIgnoreCase(sort)) {
            return sort.toLowerCase(Locale.ROOT);
        }
        return "relevance";
    }

    private String normalizeSuggestionValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String targetIndex() {
        return searchProperties.getEs().getIndexName();
    }

    private String indexAlias() {
        return searchProperties.getEs().getIndexAlias();
    }

    public record BulkSyncResult(long success, long failed, String firstError) {
    }
}
