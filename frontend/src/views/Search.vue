<template>
  <div class="search-page container page-shell">
    <section class="search-hero">
      <div class="search-hero__content">
        <span class="search-eyebrow">搜索工作台</span>
        <h1>找菜谱、食材、作者与灵感</h1>
        <p>支持按菜名、食材、分类、作者、口味和做法混合搜索。</p>
        <SearchEntry
          v-model="searchInput"
          source-page="search_page"
          size="large"
          placeholder="试试搜索：鸡胸肉、家常菜、下饭菜、张三..."
          @submit="handleSearchSubmit"
        />
        <div class="search-hero__quick">
          <div v-if="recentSearches.length" class="quick-row">
            <span class="quick-row__label">最近搜索</span>
            <div class="quick-row__tags">
              <el-tag
                v-for="item in recentSearches"
                :key="item"
                class="quick-tag history-tag"
                @click="handleHistoryClick(item)"
              >
                {{ item }}
              </el-tag>
            </div>
            <el-button link class="quick-row__action" @click="clearHistoryList">清空</el-button>
          </div>
          <div class="quick-row">
            <span class="quick-row__label">推荐搜索</span>
            <div class="quick-row__tags">
              <el-tag
                v-for="item in hotKeywords"
                :key="item"
                class="quick-tag"
                @click="handleQuickSearch(item, 'search_page_hot_tag')"
              >
                {{ item }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="search-panel">
      <div v-if="hasKeyword" class="search-toolbar">
        <div class="search-summary">
          <h2>“{{ keyword }}” 的搜索结果</h2>
          <p>{{ loading ? '正在搜索中...' : `共找到 ${total} 个菜谱` }}</p>
        </div>
        <el-radio-group v-model="sort" size="default" class="search-sort" @change="handleSortChange">
          <el-radio-button :value="'relevance'">综合相关</el-radio-button>
          <el-radio-button :value="'hot'">热门优先</el-radio-button>
          <el-radio-button :value="'new'">最新发布</el-radio-button>
        </el-radio-group>
      </div>

      <div v-if="!hasKeyword" class="search-state search-state--welcome">
        <div class="welcome-card">
          <h3>想吃什么，直接搜就行</h3>
          <p>你可以搜索菜名、食材、作者、分类、口味和做法，我们会按相关度帮你整理结果。</p>
          <div class="scope-tags">
            <span v-for="item in searchableScopes" :key="item" class="scope-tag">{{ item }}</span>
          </div>
        </div>
      </div>

      <div v-else-if="errorMessage" class="search-state">
        <el-result icon="error" title="搜索失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="loadSearchResults">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <template v-else>
        <RecipeGrid v-if="loading || recipeList.length" :recipes="recipeList" :loading="loading" />

        <div v-else class="search-state search-state--empty">
          <el-empty description="没有找到匹配的菜谱">
            <template #image>
              <div class="empty-illustration">?</div>
            </template>
            <div class="empty-actions">
              <span>换个词试试：</span>
              <div class="empty-actions__tags">
                <el-tag
                  v-for="item in emptySuggestionKeywords"
                  :key="item"
                  class="quick-tag"
                  @click="handleQuickSearch(item, 'search_page_empty_retry')"
                >
                  {{ item }}
                </el-tag>
              </div>
            </div>
          </el-empty>
        </div>

        <div v-if="total > pageSize && !loading" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recipeApi } from '@/api'
import RecipeGrid from '@/components/RecipeGrid.vue'
import SearchEntry from '@/components/SearchEntry.vue'
import { trackBehavior } from '@/utils/tracker'
import { createLatestRequestGuard } from '@/utils/latestRequest'
import { SEARCH_HOT_KEYWORDS, addSearchHistory, clearSearchHistory, getSearchHistory } from '@/utils/search'

const route = useRoute()
const router = useRouter()

const searchInput = ref('')
const keyword = ref('')
const sort = ref('relevance')
const recipeList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)
const errorMessage = ref('')
const recentSearches = ref([])
const latestSearchGuard = createLatestRequestGuard()

const searchableScopes = ['菜名', '食材', '作者', '分类', '口味', '做法']
const hotKeywords = SEARCH_HOT_KEYWORDS

const hasKeyword = computed(() => Boolean(keyword.value))
const emptySuggestionKeywords = computed(() => {
  const recent = recentSearches.value.filter((item) => item !== keyword.value)
  const hot = hotKeywords.filter((item) => item !== keyword.value)
  return [...recent, ...hot].slice(0, 6)
})

const normalizeKeyword = (value) => {
  if (typeof value !== 'string') return ''
  return value.trim().replace(/\s+/g, ' ')
}

const normalizeSort = (value) => {
  if (value === 'hot' || value === 'new') return value
  return 'relevance'
}

const normalizePage = (value) => {
  const pageNumber = Number.parseInt(value, 10)
  return Number.isFinite(pageNumber) && pageNumber > 0 ? pageNumber : 1
}

const refreshRecentSearches = () => {
  recentSearches.value = getSearchHistory()
}

const buildRouteQuery = ({ nextKeyword = keyword.value, nextSort = sort.value, nextPage = page.value } = {}) => {
  const query = {}
  const normalizedKeyword = normalizeKeyword(nextKeyword)
  const normalizedSortValue = normalizeSort(nextSort)
  const normalizedPageValue = normalizePage(nextPage)

  if (normalizedKeyword) {
    query.keyword = normalizedKeyword
  }
  if (normalizedSortValue !== 'relevance') {
    query.sort = normalizedSortValue
  }
  if (normalizedPageValue > 1) {
    query.page = String(normalizedPageValue)
  }

  return query
}

const isSameRouteState = (query) => {
  return normalizeKeyword(route.query.keyword) === (query.keyword || '')
    && normalizeSort(route.query.sort) === normalizeSort(query.sort)
    && normalizePage(route.query.page) === normalizePage(query.page)
}

const applyRouteQuery = async (payload) => {
  const nextQuery = buildRouteQuery(payload)
  if (isSameRouteState(nextQuery)) {
    keyword.value = nextQuery.keyword || ''
    searchInput.value = nextQuery.keyword || ''
    sort.value = normalizeSort(nextQuery.sort)
    page.value = normalizePage(nextQuery.page)
    if (keyword.value) {
      await loadSearchResults()
    } else {
      recipeList.value = []
      total.value = 0
      errorMessage.value = ''
    }
    return
  }

  await router.push({ path: '/search', query: nextQuery })
}

const loadSearchResults = async () => {
  if (!keyword.value) {
    recipeList.value = []
    total.value = 0
    errorMessage.value = ''
    return
  }

  loading.value = true
  errorMessage.value = ''
  const requestId = latestSearchGuard.begin()

  try {
    const res = await recipeApi.search({
      keyword: keyword.value,
      sort: sort.value,
      page: page.value,
      pageSize
    }, {
      timeout: 20000,
      silentError: true
    })
    if (!latestSearchGuard.isLatest(requestId)) {
      return
    }

    recipeList.value = Array.isArray(res.data.list) ? res.data.list : []
    total.value = Number(res.data.total || 0)
  } catch (error) {
    if (!latestSearchGuard.isLatest(requestId)) {
      return
    }
    recipeList.value = []
    total.value = 0
    errorMessage.value = error.businessResponse?.message || error.response?.data?.message || error.message || '请稍后重试'
  } finally {
    if (latestSearchGuard.isLatest(requestId)) {
      loading.value = false
    }
  }
}

const syncFromRoute = async () => {
  keyword.value = normalizeKeyword(route.query.keyword)
  searchInput.value = keyword.value
  sort.value = normalizeSort(route.query.sort)
  page.value = normalizePage(route.query.page)
  refreshRecentSearches()

  if (!keyword.value) {
    recipeList.value = []
    total.value = 0
    errorMessage.value = ''
    loading.value = false
    return
  }

  await loadSearchResults()
}

const handleSearchSubmit = async ({ keyword: nextKeyword }) => {
  refreshRecentSearches()
  await applyRouteQuery({
    nextKeyword,
    nextSort: sort.value,
    nextPage: 1
  })
}

const handleSortChange = async (value) => {
  trackBehavior('search_sort_change', {
    sourcePage: 'search_page',
    extra: {
      keyword: keyword.value,
      sort: value
    }
  })

  await applyRouteQuery({
    nextKeyword: keyword.value,
    nextSort: value,
    nextPage: 1
  })
}

const handlePageChange = async (nextPage) => {
  await applyRouteQuery({
    nextKeyword: keyword.value,
    nextSort: sort.value,
    nextPage
  })
}

const handleHistoryClick = async (value) => {
  addSearchHistory(value)
  refreshRecentSearches()
  trackBehavior('search_history_click', {
    sourcePage: 'search_page_history',
    extra: { keyword: value }
  })

  await applyRouteQuery({
    nextKeyword: value,
    nextSort: 'relevance',
    nextPage: 1
  })
}

const handleQuickSearch = async (value, sourcePage) => {
  addSearchHistory(value)
  refreshRecentSearches()
  trackBehavior('search_submit', {
    sourcePage,
    extra: { keyword: value }
  })

  await applyRouteQuery({
    nextKeyword: value,
    nextSort: 'relevance',
    nextPage: 1
  })
}

const clearHistoryList = () => {
  clearSearchHistory()
  refreshRecentSearches()
}

watch(() => route.fullPath, () => {
  syncFromRoute()
}, { immediate: true })
</script>

<style scoped>
.search-page {
  padding-top: 0;
}

.search-hero {
  margin-bottom: 22px;
  padding: 28px;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at top right, rgba(255, 214, 102, 0.28), transparent 36%),
    linear-gradient(135deg, #fff5ef 0%, #fffdf7 100%);
  border: 1px solid #f3dfd0;
}

.search-hero__content {
  max-width: 880px;
}

.search-eyebrow {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 107, 107, 0.12);
  color: #b45309;
  font-size: 12px;
  font-weight: 600;
}

.search-hero h1 {
  margin-bottom: 10px;
  font-size: 32px;
  line-height: 1.2;
  color: var(--text-primary);
}

.search-hero p {
  margin-bottom: 18px;
  color: var(--text-secondary);
}

.search-hero__quick {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-row__label {
  padding-top: 3px;
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.quick-row__tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}

.quick-row__action {
  padding: 0;
}

.quick-tag {
  cursor: pointer;
  border-radius: 999px;
  transition: var(--transition);
}

.quick-tag:hover {
  transform: translateY(-1px);
}

.history-tag {
  background: #fff;
}

.search-panel {
  padding: 20px;
  border-radius: var(--radius-lg);
  background: #fff;
  border: 1px solid var(--border-color);
}

.search-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.search-summary h2 {
  margin-bottom: 6px;
  font-size: 24px;
  color: var(--text-primary);
  word-break: break-word;
}

.search-summary p {
  color: var(--text-secondary);
}

.search-state {
  padding: 24px 0 10px;
}

.search-state--welcome {
  padding-top: 0;
}

.welcome-card {
  padding: 24px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #f8fafc 0%, #fff 100%);
  border: 1px solid #e8edf5;
}

.welcome-card h3 {
  margin-bottom: 10px;
  font-size: 22px;
  color: var(--text-primary);
}

.welcome-card p {
  margin-bottom: 16px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.scope-tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 13px;
  border: 1px solid #fed7aa;
}

.search-state--empty {
  padding-top: 8px;
}

.empty-illustration {
  width: 92px;
  height: 92px;
  margin: 0 auto;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff1f2, #fffbeb);
  color: #fb7185;
  font-size: 42px;
  font-weight: 700;
}

.empty-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.empty-actions__tags {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 900px) {
  .search-hero {
    padding: 22px 18px;
  }

  .search-hero h1 {
    font-size: 28px;
  }

  .search-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .search-panel {
    padding: 16px 14px;
  }

  .search-hero :deep(.search-entry) {
    flex-direction: column;
  }

  .search-hero :deep(.search-entry__button) {
    width: 100%;
  }

  .search-summary h2 {
    font-size: 21px;
  }
}

@media (max-width: 430px) {
  .search-hero {
    margin-bottom: 16px;
    padding: 18px 14px;
  }

  .search-hero h1 {
    font-size: 24px;
  }

  .welcome-card {
    padding: 18px 16px;
  }

  .quick-row {
    gap: 8px;
  }
}
</style>
