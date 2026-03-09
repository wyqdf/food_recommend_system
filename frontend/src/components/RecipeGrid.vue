<template>
  <div class="recipe-grid">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="6" animated class="loading-skeleton" />
    </div>
    <template v-else>
      <div v-if="recipes.length" class="grid-container">
        <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" />
      </div>
      <el-empty v-else description="暂无数据" />
    </template>
  </div>
</template>

<script setup>
import RecipeCard from './RecipeCard.vue'

defineProps({
  recipes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
</script>

<style scoped>
.recipe-grid {
  min-height: 180px;
}

.loading-container {
  padding: 12px 0;
}

.loading-skeleton {
  border-radius: var(--radius-md);
  background: #fff;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

@media (max-width: 1280px) {
  .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }
}

@media (max-width: 1024px) {
  .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
    gap: 14px;
  }
}

@media (max-width: 768px) {
  .grid-container {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}

@media (max-width: 430px) {
  .grid-container {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}

@media (max-width: 360px) {
  .grid-container {
    grid-template-columns: 1fr;
  }
}
</style>
