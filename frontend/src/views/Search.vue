<template>
  <div class="search-page container page-shell">
    <div class="search-header">
      <h2>搜索结果: "{{ keyword }}"</h2>
      <span>共找到 {{ total }} 个菜谱</span>
    </div>
    
    <RecipeGrid :recipes="recipeList" :loading="loading" />
    
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="search"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { recipeApi } from '@/api'
import RecipeGrid from '@/components/RecipeGrid.vue'

const route = useRoute()
const keyword = ref('')
const recipeList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

const search = async () => {
  if (!keyword.value) return
  loading.value = true
  try {
    const res = await recipeApi.search({
      keyword: keyword.value,
      page: page.value,
      pageSize: pageSize.value
    })
    recipeList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

watch(() => route.query.keyword, (val) => {
  keyword.value = val
  page.value = 1
  search()
})

onMounted(() => {
  keyword.value = route.query.keyword
  search()
})
</script>

<style scoped>
.search-page {
  padding-top: 0;
}

.search-header {
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  background: #fff;
}

.search-header h2 {
  font-size: 24px;
  margin-bottom: 6px;
  color: var(--text-primary);
  word-break: break-word;
}

.search-header span {
  color: var(--text-secondary);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .search-header {
    margin-bottom: 12px;
    padding: 14px;
  }

  .search-header h2 {
    font-size: 20px;
    line-height: 1.4;
  }

  .search-header span {
    display: block;
    font-size: 13px;
  }

  .pagination-wrapper {
    margin-top: 14px;
  }
}

@media (max-width: 430px) {
  .search-header h2 {
    font-size: 19px;
  }
}
</style>
