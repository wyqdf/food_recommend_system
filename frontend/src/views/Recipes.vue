<template>
  <div class="recipes-page container page-shell">
    <div class="page-header">
      <h2>菜谱大全</h2>
      <p>探索美味，发现精彩</p>
    </div>

    <div class="filter-bar">
      <div class="filter-item">
        <label>分类</label>
        <el-select v-model="filters.categoryId" placeholder="全部分类" clearable filterable @change="handleCategoryChange">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="String(cat.id)" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>难度</label>
        <el-select v-model="filters.difficultyId" placeholder="全部难度" clearable @change="fetchRecipes">
          <el-option label="简单" value="简单" />
          <el-option label="普通" value="普通" />
          <el-option label="高级" value="高级" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>排序</label>
        <el-select v-model="filters.sort" placeholder="默认排序" @change="fetchRecipes">
          <el-option label="最新发布" value="new" />
          <el-option label="最多点赞" value="hot" />
          <el-option label="最多收藏" value="collect" />
        </el-select>
      </div>
    </div>

    <RecipeGrid :recipes="recipeList" :loading="loading" />

    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next"
        background @current-change="fetchRecipes" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recipeApi } from '@/api'
import RecipeGrid from '@/components/RecipeGrid.vue'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const recipeList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

const filters = ref({
  categoryId: '',
  difficultyId: '',
  sort: 'new'
})

const fetchRecipes = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      sort: filters.value.sort
    }

    if (filters.value.categoryId) {
      params.category = parseInt(filters.value.categoryId)
    }
    if (filters.value.difficultyId) {
      params.difficulty = filters.value.difficultyId
    }

    const res = await recipeApi.getList(params)
    recipeList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleCategoryChange = (value) => {
  filters.value.categoryId = value || ''
  if (value) {
    router.push({ query: { ...route.query, category: value } })
  } else {
    const { category, ...query } = route.query
    router.push({ query })
  }
  fetchRecipes()
}

watch(() => route.query.category, (newCategory, oldCategory) => {
  if (newCategory !== oldCategory) {
    page.value = 1
    if (newCategory) {
      filters.value.categoryId = newCategory
    } else {
      filters.value.categoryId = ''
    }
    fetchRecipes()
  }
})

onMounted(async () => {
  const catRes = await recipeApi.getCategories()
  categories.value = catRes.data

  if (route.query.category) {
    filters.value.categoryId = route.query.category
  }
  fetchRecipes()
})
</script>

<style scoped>
.recipes-page {
  padding-top: 0;
}

.page-header {
  margin-bottom: 18px;
  padding: 20px 22px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #fff7f1 0%, #fff 70%);
  border: 1px solid #f0e3d7;
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

.filter-bar {
  display: flex;
  gap: 18px;
  margin-bottom: 20px;
  padding: 18px 20px;
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs);
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-item label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-regular);
  white-space: nowrap;
}

.filter-item .el-select {
  width: 168px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 26px;
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

  .filter-bar {
    flex-direction: column;
    gap: 10px;
    padding: 14px;
    margin-bottom: 14px;
  }

  .filter-item {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .filter-item .el-select {
    width: 100%;
  }
}

@media (max-width: 430px) {
  .page-header h2 {
    font-size: 22px;
  }

  .pagination-wrapper {
    margin-top: 18px;
  }
}
</style>
