<template>
  <div class="category-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <span>分类管理</span>
            <small>管理食谱分类和关联数量</small>
          </div>
          <el-button type="primary" @click="handleCreate">
            <el-icon>
              <Plus />
            </el-icon>
            新增分类
          </el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="searchText" placeholder="搜索分类名称" clearable class="search-input" @input="handleSearch">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>
        <el-tag effect="plain" type="info">共 {{ filteredData.length }} 个分类</el-tag>
      </div>

      <el-table :data="filteredData" v-loading="loading" class="table-full">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" />
        <el-table-column label="食谱数量" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.recipeCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :disabled="(row.recipeCount || 0) > 0" @click="handleDelete(row)">
              删除{{ (row.recipeCount || 0) > 0 ? '（已关联）' : '' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px" @close="handleDialogClose">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="分类名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入分类名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { adminCategoryApi } from '@/api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const tableData = ref([])
const filteredData = ref([])
const searchText = ref('')

const formData = reactive({
  id: null,
  name: ''
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const dialogTitle = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await adminCategoryApi.getList()
    if (res.code === 200) {
      tableData.value = res.data
      handleSearch(searchText.value)
    }
  } catch (error) {
    ElMessage.error('加载失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleSearch = (value) => {
  if (!value) {
    filteredData.value = tableData.value
    return
  }
  const keyword = value.toLowerCase()
  filteredData.value = tableData.value.filter(item =>
    item.name.toLowerCase().includes(keyword)
  )
}

const handleCreate = () => {
  isEdit.value = false
  dialogTitle.value = '新增分类'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑分类'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  if (row.recipeCount > 0) {
    ElMessage.warning(`无法删除：该分类已被 ${row.recipeCount} 个食谱使用`)
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '警告', { type: 'warning' })
    await adminCategoryApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          await adminCategoryApi.update(formData.id, formData)
          ElMessage.success('更新成功')
        } else {
          await adminCategoryApi.create(formData)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        ElMessage.error('操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  formData.id = null
  formData.name = ''
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.category-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-title {
  display: flex;
  flex-direction: column;
}

.header-title span {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
}

.header-title small {
  margin-top: 2px;
  font-size: 12px;
  color: #8a97a8;
}

.search-bar {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.search-input {
  width: 240px;
}

.table-full {
  width: 100%;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input {
    width: 100%;
  }
}
</style>
