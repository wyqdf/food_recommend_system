package com.foodrecommend.letmecook.search;

import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.entity.RecipeIngredient;
import com.foodrecommend.letmecook.mapper.CategoryMapper;
import com.foodrecommend.letmecook.mapper.RecipeIngredientMapper;
import com.foodrecommend.letmecook.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeSearchDataLoader {

    private final RecipeMapper recipeMapper;
    private final CategoryMapper categoryMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;

    public RecipeSearchDocument loadPublicRecipe(Integer recipeId) {
        if (recipeId == null || recipeId <= 0) {
            return null;
        }
        Recipe recipe = recipeMapper.findPublicById(recipeId);
        if (recipe == null) {
            return null;
        }
        return buildDocuments(List.of(recipe)).stream().findFirst().orElse(null);
    }

    public List<RecipeSearchDocument> loadPublicRecipes(List<Integer> recipeIds) {
        List<Integer> validIds = normalizeIds(recipeIds);
        if (validIds.isEmpty()) {
            return List.of();
        }
        List<Recipe> recipes = recipeMapper.findByIds(validIds);
        return buildDocuments(recipes);
    }

    public List<RecipeSearchDocument> loadPublicRecipesBatch(int offset, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<Recipe> recipes = recipeMapper.findPublicForSearchBatch(Math.max(offset, 0), limit);
        return buildDocuments(recipes);
    }

    public long countPublicRecipes() {
        return recipeMapper.countPublicRecipes();
    }

    private List<RecipeSearchDocument> buildDocuments(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return List.of();
        }

        List<Integer> recipeIds = recipes.stream()
                .map(Recipe::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, List<String>> categoryMap = groupCategories(recipeIds);
        Map<Integer, List<String>> ingredientMap = groupIngredients(recipeIds);

        List<RecipeSearchDocument> documents = new ArrayList<>(recipes.size());
        for (Recipe recipe : recipes) {
            RecipeSearchDocument document = new RecipeSearchDocument();
            document.setId(String.valueOf(recipe.getId()));
            document.setRecipeId(recipe.getId());
            document.setTitle(trimToNull(recipe.getTitle()));
            document.setAuthor(trimToNull(recipe.getAuthor()));
            document.setAuthorUid(trimToNull(recipe.getAuthorUid()));
            document.setCategories(categoryMap.getOrDefault(recipe.getId(), List.of()));
            document.setIngredients(ingredientMap.getOrDefault(recipe.getId(), List.of()));
            document.setTasteName(trimToNull(recipe.getTasteName()));
            document.setTechniqueName(trimToNull(recipe.getTechniqueName()));
            document.setTimeCostName(trimToNull(recipe.getTimeCostName()));
            document.setDifficultyName(trimToNull(recipe.getDifficultyName()));
            document.setSearchText(buildSearchText(document));
            document.setTitleSuggestInputs(singleValueSuggestionInputs(document.getTitle()));
            document.setIngredientSuggestInputs(document.getIngredients());
            document.setCategorySuggestInputs(document.getCategories());
            document.setAuthorSuggestInputs(singleValueSuggestionInputs(document.getAuthor()));
            document.setLikeCount(recipe.getLikeCount() == null ? 0 : recipe.getLikeCount());
            document.setStatus(recipe.getStatus() == null ? 0 : recipe.getStatus());
            document.setCreateTime(recipe.getCreateTime());
            document.setUpdateTime(recipe.getUpdateTime());
            documents.add(document);
        }
        return documents;
    }

    private Map<Integer, List<String>> groupCategories(List<Integer> recipeIds) {
        return categoryMapper.findByRecipeIds(recipeIds).stream()
                .collect(Collectors.groupingBy(
                        CategoryMapper.CategoryRecipeDTO::getRecipeId,
                        LinkedHashMap::new,
                        Collectors.mapping(CategoryMapper.CategoryRecipeDTO::getName,
                                Collectors.collectingAndThen(Collectors.toList(), RecipeSearchDataLoader::distinctNonBlankValues))
                ));
    }

    private Map<Integer, List<String>> groupIngredients(List<Integer> recipeIds) {
        return recipeIngredientMapper.findByRecipeIds(recipeIds).stream()
                .collect(Collectors.groupingBy(
                        RecipeIngredient::getRecipeId,
                        LinkedHashMap::new,
                        Collectors.mapping(RecipeIngredient::getIngredientName,
                                Collectors.collectingAndThen(Collectors.toList(), RecipeSearchDataLoader::distinctNonBlankValues))
                ));
    }

    private String buildSearchText(RecipeSearchDocument document) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        addIfPresent(parts, document.getTitle());
        addIfPresent(parts, document.getAuthor());
        addAllIfPresent(parts, document.getCategories());
        addAllIfPresent(parts, document.getIngredients());
        addIfPresent(parts, document.getTasteName());
        addIfPresent(parts, document.getTechniqueName());
        addIfPresent(parts, document.getTimeCostName());
        addIfPresent(parts, document.getDifficultyName());
        return String.join(" ", parts);
    }

    private static List<Integer> normalizeIds(Collection<Integer> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    private static List<String> distinctNonBlankValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                normalized.add(trimmed);
            }
        }
        return List.copyOf(normalized);
    }

    private static List<String> singleValueSuggestionInputs(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? List.of() : List.of(trimmed);
    }

    private static void addIfPresent(Set<String> values, String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            values.add(trimmed);
        }
    }

    private static void addAllIfPresent(Set<String> target, Collection<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addIfPresent(target, value);
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
