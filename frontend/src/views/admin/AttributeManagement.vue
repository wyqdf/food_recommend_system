<template>
  <div class="attribute-management">
    <div class="page-heading">
      <h2 class="title">属性管理</h2>
      <p class="subtitle">统一维护口味、技法、耗时、难度和食材属性</p>
    </div>

    <el-tabs v-model="activeTab" type="border-card" class="attribute-tabs">
      <el-tab-pane label="口味管理" name="taste">
        <attribute-table 
          :data="tasteData"
          :loading="loading"
          title="口味"
          @add="handleAdd('taste')"
          @edit="handleEdit('taste', $event)"
          @delete="handleDelete('taste', $event)"
        />
      </el-tab-pane>
      
      <el-tab-pane label="烹饪技法管理" name="technique">
        <attribute-table 
          :data="techniqueData"
          :loading="loading"
          title="技法"
          @add="handleAdd('technique')"
          @edit="handleEdit('technique', $event)"
          @delete="handleDelete('technique', $event)"
        />
      </el-tab-pane>
      
      <el-tab-pane label="耗时管理" name="timeCost">
        <attribute-table 
          :data="timeCostData"
          :loading="loading"
          title="耗时"
          @add="handleAdd('timeCost')"
          @edit="handleEdit('timeCost', $event)"
          @delete="handleDelete('timeCost', $event)"
        />
      </el-tab-pane>
      
      <el-tab-pane label="难度管理" name="difficulty">
        <attribute-table 
          :data="difficultyData"
          :loading="loading"
          title="难度"
          @add="handleAdd('difficulty')"
          @edit="handleEdit('difficulty', $event)"
          @delete="handleDelete('difficulty', $event)"
        />
      </el-tab-pane>
      
      <el-tab-pane label="食材管理" name="ingredient">
        <attribute-table 
          :data="ingredientData"
          :loading="loading"
          title="食材"
          @add="handleAdd('ingredient')"
          @edit="handleEdit('ingredient', $event)"
          @delete="handleDelete('ingredient', $event)"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="400px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入名称" />
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
import { adminTasteApi, adminTechniqueApi, adminTimeCostApi, adminDifficultyApi, adminIngredientApi } from '@/api/admin'
import AttributeTable from '@/components/admin/AttributeTable.vue'

const activeTab = ref('taste')
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const currentType = ref('')
const currentId = ref(null)

const tasteData = ref([])
const techniqueData = ref([])
const timeCostData = ref([])
const difficultyData = ref([])
const ingredientData = ref([])

const formData = reactive({
  name: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => currentId.value ? '编辑' : '新增')

const getApi = (type) => {
  const apiMap = {
    taste: adminTasteApi,
    technique: adminTechniqueApi,
    timeCost: adminTimeCostApi,
    difficulty: adminDifficultyApi,
    ingredient: adminIngredientApi
  }
  return apiMap[type]
}

const getData = (type) => {
  const dataMap = {
    taste: tasteData,
    technique: techniqueData,
    timeCost: timeCostData,
    difficulty: difficultyData,
    ingredient: ingredientData
  }
  return dataMap[type]
}

const loadData = async (type) => {
  loading.value = true
  try {
    const api = getApi(type)
    const res = await api.getList()
    if (res.code === 200) {
      getData(type).value = res.data
    }
  } catch (error) {
    console.error(`加载${type}数据失败:`, error)
  } finally {
    loading.value = false
  }
}

const handleAdd = (type) => {
  currentType.value = type
  currentId.value = null
  formData.name = ''
  dialogVisible.value = true
}

const handleEdit = (type, row) => {
  currentType.value = type
  currentId.value = row.id
  formData.name = row.name
  dialogVisible.value = true
}

const handleDelete = async (type, row) => {
  try {
    const hasRelations = row.recipeCount > 0
    
    if (hasRelations) {
      ElMessage.warning(`无法删除：该${row.title || '属性'}已被 ${row.recipeCount} 个食谱使用`)
      return
    }
    
    await ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '警告', {
      type: 'warning'
    })
    
    const api = getApi(type)
    await api.delete(row.id)
    
    ElMessage.success('删除成功')
    loadData(type)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    const api = getApi(currentType.value)
    
    if (currentId.value) {
      await api.update(currentId.value, { name: formData.name })
      ElMessage.success('更新成功')
    } else {
      await api.create({ name: formData.name })
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadData(currentType.value)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
      ElMessage.error('操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData('taste')
  loadData('technique')
  loadData('timeCost')
  loadData('difficulty')
  loadData('ingredient')
})
</script>

<style scoped>
.attribute-management {
  padding: 0;
}

.page-heading {
  margin-bottom: 12px;
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

.attribute-tabs {
  border-radius: 12px;
  overflow: hidden;
}

.attribute-table {
  margin-top: 10px;
}

.table-header {
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
