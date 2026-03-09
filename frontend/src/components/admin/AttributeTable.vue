<template>
  <div class="attribute-table">
    <div class="table-header">
      <div class="search-wrapper">
        <el-input v-model="searchText" placeholder="搜索" clearable class="search-input" @input="handleSearch">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>
      </div>
      <el-button type="primary" size="small" @click="$emit('add')">
        新增{{ title }}
      </el-button>
    </div>
    <el-table :data="filteredData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" :label="title + '名称'" />
      <el-table-column prop="recipeCount" label="关联食谱数" width="120">
        <template #default="{ row }">
          <el-tag :type="row.recipeCount > 0 ? 'warning' : 'info'">
            {{ row.recipeCount || 0 }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="$emit('edit', row)">编辑</el-button>
          <el-button type="danger" link :disabled="row.recipeCount > 0" @click="$emit('delete', row)">
            删除{{ row.recipeCount > 0 ? '（已关联）' : '' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  data: { type: Array, required: true },
  loading: { type: Boolean, default: false },
  title: { type: String, required: true }
})

defineEmits(['add', 'edit', 'delete'])

const searchText = ref('')
const filteredData = ref([])

watch(() => props.data, (newData) => {
  filteredData.value = newData
  if (searchText.value) {
    handleSearch(searchText.value)
  }
}, { immediate: true })

const handleSearch = (value) => {
  if (!value) {
    filteredData.value = props.data
    return
  }
  const keyword = value.toLowerCase()
  filteredData.value = props.data.filter(item =>
    item.name.toLowerCase().includes(keyword)
  )
}
</script>

<style scoped>
.attribute-table {
  margin-top: 10px;
}

.table-header {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 220px;
}

@media (max-width: 768px) {
  .table-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input {
    width: 100%;
  }
}
</style>
