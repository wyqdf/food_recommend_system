<template>
  <div class="favorites-page page-shell">
    <el-card class="favorites-card">
      <template #header>
        <div class="card-header">
          <div>
            <h3 class="title">我的收藏</h3>
            <p class="subtitle">收藏的菜谱会统一保存在这里</p>
          </div>
          <el-tag type="warning" effect="light">共 {{ total }} 条</el-tag>
        </div>
      </template>
      <RecipeGrid :recipes="favoriteList" :loading="loading" />
      <el-empty v-if="!loading && !favoriteList.length" description="暂无收藏" />
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchFavorites"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { favoriteApi } from '@/api'
import RecipeGrid from '@/components/RecipeGrid.vue'

const favoriteList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchFavorites = async () => {
  loading.value = true
  try {
    const res = await favoriteApi.getList({ page: page.value, pageSize: pageSize.value })
    favoriteList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.favorites-page {
  width: 100%;
}

.favorites-card {
  border-radius: var(--radius-md);
}

.favorites-card :deep(.el-card__body) {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.title {
  font-size: 20px;
  line-height: 1.2;
  color: var(--text-primary);
}

.subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

@media (max-width: 768px) {
  .favorites-card :deep(.el-card__header) {
    padding: 14px;
  }

  .favorites-card :deep(.el-card__body) {
    padding: 14px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .pagination-wrapper {
    margin-top: 14px;
  }
}

@media (max-width: 430px) {
  .title {
    font-size: 18px;
  }

  .subtitle {
    font-size: 12px;
  }
}
</style>
