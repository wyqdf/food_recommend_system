<template>
  <div class="recommend-page container page-shell">
    <div class="page-header">
      <h2>为你推荐</h2>
      <p>基于你的浏览历史和喜好，为你精选以下菜谱</p>
    </div>

    <div class="tabs-wrapper">
      <el-tabs v-model="activeTab" @tab-change="fetchRecommend">
        <el-tab-pane name="personal">
          <template #label>
            <span class="tab-label">
              <el-icon>
                <Star />
              </el-icon>
              猜你喜欢
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="hot">
          <template #label>
            <span class="tab-label">
              <el-icon>
                <TrendCharts />
              </el-icon>
              热门推荐
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="new">
          <template #label>
            <span class="tab-label">
              <el-icon>
                <Clock />
              </el-icon>
              最新发布
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <RecipeGrid :recipes="recipeList" :loading="loading" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { recipeApi } from '@/api'
import RecipeGrid from '@/components/RecipeGrid.vue'
import { Star, TrendCharts, Clock } from '@element-plus/icons-vue'

const activeTab = ref('personal')
const recipeList = ref([])
const loading = ref(false)
const cache = new Map()

const fetchRecommend = async () => {
  const cacheKey = `${activeTab.value}_12`
  if (cache.has(cacheKey)) {
    recipeList.value = cache.get(cacheKey)
    return
  }

  loading.value = true
  try {
    const res = await recipeApi.getRecommend({ type: activeTab.value, limit: 12 })
    recipeList.value = res.data.list || []
    cache.set(cacheKey, recipeList.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchRecommend()
})
</script>

<style scoped>
.recommend-page {
  padding-top: 0;
}

.page-header {
  margin-bottom: 22px;
  padding: 22px 24px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #eefaf8 0%, #ffffff 70%);
  border: 1px solid #dceeea;
}

.page-header h2 {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.page-header p {
  font-size: 16px;
  color: var(--text-secondary);
}

.tabs-wrapper {
  margin-bottom: 24px;
  background: #fff;
  padding: 8px 24px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  padding: 0 24px;
  height: 48px;
  line-height: 48px;
}
</style>
