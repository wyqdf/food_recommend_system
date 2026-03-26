<template>
  <div class="recommend-page container page-shell">
    <div class="page-header">
      <h2>为你推荐</h2>
      <p>结合你的偏好与当前场景模式，为你智能挑选更合适的菜谱</p>
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

    <div v-if="activeTab === 'personal' && categoryOptions.length" class="scene-filter">
      <span class="scene-label">分类筛选：</span>
      <el-segmented v-model="selectedCategoryId" :options="categorySegmentOptions" @change="fetchRecommend" />
    </div>

    <transition name="mode-fade" mode="out-in">
      <RecipeGrid :key="gridKey" :recipes="recipeList" :loading="loading" />
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { recipeApi } from '@/api'
import RecipeGrid from '@/components/RecipeGrid.vue'
import { Star, TrendCharts, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { trackBehavior } from '@/utils/tracker'
import { useSceneModeStore } from '@/stores/sceneMode'

const sceneModeStore = useSceneModeStore()
const { currentMode } = storeToRefs(sceneModeStore)
const activeTab = ref('personal')
const recipeList = ref([])
const loading = ref(false)
const cache = new Map()
const selectedCategoryId = ref('')
const categoryOptions = ref([])

const gridKey = computed(() => `${currentMode.value}_${activeTab.value}_${selectedCategoryId.value || 'all'}`)

const categorySegmentOptions = computed(() => {
  const options = [{ label: '全部', value: '' }]
  categoryOptions.value.forEach(category => {
    options.push({ label: category.name, value: category.id })
  })
  return options
})

const loadCategories = async () => {
  try {
    const res = await recipeApi.getRecommendCategories(10)
    categoryOptions.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    categoryOptions.value = []
  }
}

const fetchRecommend = async () => {
  const categoryKey = activeTab.value === 'personal' ? selectedCategoryId.value || 'all' : 'all'
  const cacheKey = `${activeTab.value}_${categoryKey}_${currentMode.value}_12`
  if (cache.has(cacheKey)) {
    recipeList.value = cache.get(cacheKey)
    return
  }

  loading.value = true
  try {
    const params = { type: activeTab.value, limit: 12, mode: currentMode.value }
    if (activeTab.value === 'personal' && selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }
    const res = await recipeApi.getRecommend(params)
    recipeList.value = res.data.list || []
    cache.set(cacheKey, recipeList.value)
    const selectedCategoryName = categoryOptions.value.find(item => String(item.id) === String(selectedCategoryId.value))?.name || null
    trackBehavior('recommend_request', {
      sourcePage: 'recommend',
      sceneCode: null,
      extra: {
        type: activeTab.value,
        mode: currentMode.value,
        size: recipeList.value.length,
        categoryId: params.categoryId || null,
        categoryName: selectedCategoryName
      }
    })
  } catch (error) {
    recipeList.value = []
    ElMessage.warning('推荐加载稍慢，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  trackBehavior('page_view', { sourcePage: 'recommend', extra: { mode: currentMode.value } })
  await fetchRecommend()
})

watch(currentMode, async () => {
  cache.clear()
  await fetchRecommend()
})
</script>

<style scoped>
.recommend-page {
  padding-top: 0;
}

.page-header {
  margin-bottom: 18px;
  padding: 20px 22px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #eefaf8 0%, #ffffff 70%);
  border: 1px solid #dceeea;
}

.page-header h2 {
  font-size: 30px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.page-header p {
  font-size: 15px;
  color: var(--text-secondary);
}

.tabs-wrapper {
  margin-bottom: 20px;
  background: #fff;
  padding: 8px 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  padding: 0 20px;
  height: 46px;
  line-height: 46px;
}

.scene-filter {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.scene-label {
  color: var(--text-secondary);
  font-size: 14px;
  flex-shrink: 0;
}

.mode-fade-enter-active,
.mode-fade-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.mode-fade-enter-from,
.mode-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (max-width: 768px) {
  .page-header {
    padding: 14px;
    margin-bottom: 12px;
  }

  .page-header h2 {
    font-size: 24px;
  }

  .page-header p {
    font-size: 14px;
  }

  .tabs-wrapper {
    padding: 6px 10px;
    margin-bottom: 14px;
  }

  .tab-label {
    font-size: 13px;
    gap: 4px;
  }

  :deep(.el-tabs__item) {
    padding: 0 12px;
    height: 42px;
    line-height: 42px;
  }

  :deep(.el-tabs__header) {
    margin-bottom: 2px;
  }
}

@media (max-width: 430px) {
  .page-header h2 {
    font-size: 22px;
  }

  .page-header p {
    line-height: 1.5;
  }

  .tab-label .el-icon {
    display: none;
  }

  :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
    scrollbar-width: none;
  }

  :deep(.el-tabs__nav-wrap::-webkit-scrollbar) {
    display: none;
  }

  :deep(.el-tabs__nav) {
    min-width: max-content;
  }

  :deep(.el-tabs__item) {
    padding: 0 12px;
  }
}
</style>
