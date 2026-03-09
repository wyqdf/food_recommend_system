<template>
  <div class="recipe-management">
    <el-card class="recipe-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <span>食谱管理</span>
            <small>支持筛选、批量删除与快速维护</small>
          </div>
          <div class="header-actions">
            <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
              <el-icon>
                <Delete />
              </el-icon>
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" @click="handleCreate">
              <el-icon>
                <Plus />
              </el-icon>
              新增食谱
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="食谱名称/作者" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="searchForm.difficultyId" placeholder="全部" clearable>
            <el-option v-for="item in difficulties" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="口味">
          <el-select v-model="searchForm.tasteId" placeholder="全部" clearable>
            <el-option v-for="item in tastes" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <div class="toolbar-tags">
          <el-tag effect="plain" type="info">总数 {{ pagination.total }}</el-tag>
          <el-tag effect="light" type="warning">已选 {{ selectedIds.length }}</el-tag>
        </div>
        <div class="toolbar-actions">
          <el-button link type="primary" @click="loadData">刷新</el-button>
          <el-button link :disabled="selectedIds.length === 0" @click="clearSelection">清空选择</el-button>
        </div>
      </div>

      <el-table
        ref="recipeTableRef"
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="封面图" width="100">
          <template #default="{ row }">
            <el-image :src="row.image || '/images/food-placeholder.svg'" :preview-src-list="row.image ? [row.image] : []" class="recipe-cover" fit="cover">
              <template #error>
                <div class="table-image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)">{{ row.difficulty }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taste" label="口味" width="80" />
        <el-table-column prop="technique" label="技法" width="80" />
        <el-table-column prop="timeCost" label="耗时" width="100" />
        <el-table-column prop="viewCount" label="浏览量" sortable width="90" />
        <el-table-column prop="likeCount" label="点赞数" sortable width="90" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.pageSize"
        :total="pagination.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData" @current-change="loadData" style="margin-top: 20px; justify-content: flex-end" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" @close="handleDialogClose">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入食谱标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="formData.author" placeholder="请输入作者" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="封面图 URL" prop="image">
              <div style="display: flex; gap: 10px; align-items: center;">
                <el-input v-model="formData.image" placeholder="请输入图片 URL" style="flex: 1;" />
                <el-upload
                  class="image-uploader"
                  :show-file-list="false"
                  :before-upload="handleImageUpload"
                  accept="image/*"
                >
                  <el-button type="primary">
                    <el-icon><Upload /></el-icon>
                    上传
                  </el-button>
                </el-upload>
              </div>
              <div v-if="formData.image" style="margin-top: 10px;">
                <el-image :src="formData.image" style="width: 100px; height: 100px;" fit="cover" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者 ID" prop="authorUid">
              <el-input v-model="formData.authorUid" placeholder="请输入作者 UID" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="难度" prop="difficultyId">
              <el-select v-model="formData.difficultyId" placeholder="请选择难度" style="width: 100%">
                <el-option v-for="item in difficulties" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="口味" prop="tasteId">
              <el-select v-model="formData.tasteId" placeholder="请选择口味" style="width: 100%">
                <el-option v-for="item in tastes" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="技法" prop="techniqueId">
              <el-select v-model="formData.techniqueId" placeholder="请选择技法" style="width: 100%">
                <el-option v-for="item in techniques" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="耗时" prop="timeCostId">
              <el-select v-model="formData.timeCostId" placeholder="请选择耗时" style="width: 100%">
                <el-option v-for="item in timeCosts" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="厨具" prop="cookware">
              <el-input v-model="formData.cookware" placeholder="所需厨具" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入食谱描述" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="小贴士" prop="tips">
              <el-input v-model="formData.tips" type="textarea" :rows="2" placeholder="请输入烹饪小贴士" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">食材清单</el-divider>

        <el-table :data="formData.ingredients" style="margin-bottom: 10px">
          <el-table-column label="食材" width="200">
            <template #default="{ row, $index }">
              <el-select v-model="row.ingredientId" placeholder="选择食材" filterable allow-create default-first-option
                style="width: 100%">
                <el-option v-for="item in ingredients" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="150">
            <template #default="{ row }">
              <el-select v-model="row.type" placeholder="食材类型" style="width: 100%">
                <el-option label="主料" value="main" />
                <el-option label="辅料" value="sub" />
                <el-option label="调料" value="seasoning" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="用量" width="150">
            <template #default="{ row }">
              <el-input v-model="row.quantity" placeholder="例如：500g" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeIngredient($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-button type="primary" size="small" @click="addIngredient">
          <el-icon>
            <Plus />
          </el-icon>
          添加食材
        </el-button>

        <el-divider content-position="left">烹饪步骤</el-divider>

        <div v-for="(step, index) in formData.steps" :key="index" style="margin-bottom: 10px">
          <el-input v-model="step.description" type="textarea" :rows="2" placeholder="请输入烹饪步骤"
            style="margin-bottom: 5px" />
          <div style="display: flex; gap: 10px; align-items: center; margin-bottom: 5px;">
            <el-input v-model="step.image" placeholder="步骤图片 URL（可选）" style="flex: 1;" />
            <el-upload
              :show-file-list="false"
              :before-upload="(file) => handleStepImageUpload(file, index)"
              accept="image/*"
            >
              <el-button type="primary" size="small">
                <el-icon><Upload /></el-icon>
                上传
              </el-button>
            </el-upload>
          </div>
          <div v-if="step.image" style="margin-bottom: 5px;">
            <el-image :src="step.image" style="width: 80px; height: 80px;" fit="cover" />
          </div>
          <el-button type="danger" link @click="removeStep(index)">删除此步骤</el-button>
        </div>

        <el-button type="primary" size="small" @click="addStep">
          <el-icon>
            <Plus />
          </el-icon>
          添加步骤
        </el-button>

        <el-divider content-position="left">分类</el-divider>

        <el-checkbox-group v-model="formData.categoryIds">
          <el-checkbox v-for="cat in categories" :key="cat.id" :label="cat.id">
            {{ cat.name }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="食谱详情" width="700px">
      <div v-if="currentRecipe" class="recipe-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题">{{ currentRecipe.title }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ currentRecipe.author }}</el-descriptions-item>
          <el-descriptions-item label="难度">{{ currentRecipe.difficulty?.name }}</el-descriptions-item>
          <el-descriptions-item label="口味">{{ currentRecipe.taste?.name }}</el-descriptions-item>
          <el-descriptions-item label="技法">{{ currentRecipe.technique?.name }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ currentRecipe.timeCost?.name }}</el-descriptions-item>
          <el-descriptions-item label="厨具">{{ currentRecipe.cookware }}</el-descriptions-item>
          <el-descriptions-item label="浏览量">{{ currentRecipe.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="点赞数">{{ currentRecipe.likeCount }}</el-descriptions-item>
          <el-descriptions-item label="评论数">{{ currentRecipe.replyCount }}</el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag v-for="(cat, index) in currentRecipe.categories" :key="index" size="small">
              {{ cat.name }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 20px">
          <h4>描述</h4>
          <p>{{ currentRecipe.description }}</p>
        </div>
        <div style="margin-top: 20px" v-if="currentRecipe.tips">
          <h4>小贴士</h4>
          <p>{{ currentRecipe.tips }}</p>
        </div>
        <div style="margin-top: 20px" v-if="currentRecipe.ingredients && currentRecipe.ingredients.length > 0">
          <h4>食材</h4>
          <el-table :data="currentRecipe.ingredients" size="small">
            <el-table-column prop="name" label="食材" />
            <el-table-column prop="type" label="类型">
              <template #default="{ row }">
                {{ row.type === 'main' ? '主料' : row.type === 'sub' ? '辅料' : '调料' }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="用量" />
          </el-table>
        </div>
        <div style="margin-top: 20px" v-if="currentRecipe.steps && currentRecipe.steps.length > 0">
          <h4>烹饪步骤</h4>
          <el-timeline>
            <el-timeline-item v-for="(step, index) in currentRecipe.steps" :key="index" :timestamp="'步骤 ' + (index + 1)"
              placement="top">
              <p>{{ step.description }}</p>
              <el-image v-if="step.image" :src="step.image" style="width: 200px; margin-top: 10px" fit="cover" />
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, Upload, Delete } from '@element-plus/icons-vue'
import { adminRecipeApi, adminTasteApi, adminTechniqueApi, adminTimeCostApi, adminDifficultyApi, adminIngredientApi, adminCategoryApi } from '@/api/admin'
import { uploadApi } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const imageUploadLoading = ref(false)
const recipeTableRef = ref(null)

const handleImageUpload = async (file) => {
  if (!file) return false

  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }

  imageUploadLoading.value = true

  try {
    const res = await uploadApi.uploadImage(file)
    if (res.code === 200) {
      formData.image = res.data.url
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败，请重试')
  } finally {
    imageUploadLoading.value = false
  }

  return false
}

const handleStepImageUpload = async (file, index) => {
  if (!file) return false

  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }

  try {
    const res = await uploadApi.uploadImage(file)
    if (res.code === 200) {
      formData.steps[index].image = res.data.url
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败，请重试')
  }

  return false
}

const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({
  keyword: '',
  difficultyId: null,
  tasteId: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const currentRecipe = ref(null)
const selectedIds = ref([])

const formData = reactive({
  id: null,
  title: '',
  author: '',
  authorUid: '',
  image: '',
  description: '',
  tips: '',
  cookware: '',
  difficultyId: null,
  tasteId: null,
  techniqueId: null,
  timeCostId: null,
  categoryIds: [],
  ingredients: [],
  steps: []
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  difficultyId: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

const dialogTitle = ref('')

const difficulties = ref([])
const tastes = ref([])
const techniques = ref([])
const timeCosts = ref([])
const ingredients = ref([])
const categories = ref([])

const getDifficultyType = (difficulty) => {
  const map = {
    '简单': 'success',
    '中等': 'warning',
    '困难': 'danger'
  }
  return map[difficulty] || 'info'
}

const loadOptions = async () => {
  try {
    const [diffRes, tasteRes, techRes, timeRes, ingRes, catRes] = await Promise.all([
      adminDifficultyApi.getList(),
      adminTasteApi.getList(),
      adminTechniqueApi.getList(),
      adminTimeCostApi.getList(),
      adminIngredientApi.getList(),
      adminCategoryApi.getList()
    ])

    difficulties.value = diffRes.data || []
    tastes.value = tasteRes.data || []
    techniques.value = techRes.data || []
    timeCosts.value = timeRes.data || []
    ingredients.value = ingRes.data || []
    categories.value = catRes.data || []
  } catch (error) {
    console.error('加载选项失败:', error)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await adminRecipeApi.getList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      difficultyId: searchForm.difficultyId,
      tasteId: searchForm.tasteId
    })
    if (res.code === 200) {
      tableData.value = res.data.list
      pagination.total = res.data.total
      selectedIds.value = []
    }
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.difficultyId = null
  searchForm.tasteId = null
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  isEdit.value = false
  dialogTitle.value = '新增食谱'
  Object.keys(formData).forEach(key => {
    if (Array.isArray(formData[key])) {
      formData[key] = []
    } else {
      formData[key] = null
    }
  })
  formData.ingredients = [{ ingredientId: null, type: 'main', quantity: '' }]
  formData.steps = [{ description: '', image: '' }]
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑食谱'
  try {
    const res = await adminRecipeApi.getById(row.id)
    if (res.code === 200) {
      const recipe = res.data
      Object.assign(formData, {
        id: recipe.id,
        title: recipe.title,
        author: recipe.author,
        authorUid: recipe.authorUid || '',
        image: recipe.image,
        description: recipe.description,
        tips: recipe.tips || '',
        cookware: recipe.cookware || '',
        difficultyId: recipe.difficulty?.id,
        tasteId: recipe.taste?.id,
        techniqueId: recipe.technique?.id,
        timeCostId: recipe.timeCost?.id,
        categoryIds: recipe.categories?.map(c => c.id) || [],
        ingredients: recipe.ingredients?.map(i => ({
          ingredientId: i.ingredientId,
          type: i.type,
          quantity: i.quantity
        })) || [],
        steps: recipe.steps?.map(s => ({
          stepNumber: s.stepNumber,
          description: s.description,
          image: s.image
        })) || []
      })
    }
  } catch (error) {
    console.error('加载食谱详情失败:', error)
    ElMessage.error('加载食谱详情失败')
  }
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await adminRecipeApi.getById(row.id)
    if (res.code === 200) {
      currentRecipe.value = res.data
      viewDialogVisible.value = true
    }
  } catch (error) {
    console.error('加载食谱详情失败:', error)
    ElMessage.error('加载食谱详情失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个食谱吗？', '提示', {
      type: 'warning'
    })
    await adminRecipeApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const clearSelection = () => {
  recipeTableRef.value?.clearSelection()
  selectedIds.value = []
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个食谱吗？此操作不可恢复！`, '警告', {
      type: 'warning'
    })

    const res = await adminRecipeApi.batchDelete(selectedIds.value)
    const successCount = res?.data?.successCount ?? selectedIds.value.length
    const failCount = res?.data?.failCount ?? 0

    if (failCount > 0) {
      ElMessage.warning(`批量删除完成：成功 ${successCount} 个，失败 ${failCount} 个`)
    } else {
      ElMessage.success(`批量删除成功，共删除 ${successCount} 个食谱`)
    }

    selectedIds.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const addIngredient = () => {
  formData.ingredients.push({ ingredientId: null, type: 'main', quantity: '' })
}

const removeIngredient = (index) => {
  formData.ingredients.splice(index, 1)
}

const addStep = () => {
  formData.steps.push({ stepNumber: formData.steps.length + 1, description: '', image: '' })
}

const removeStep = (index) => {
  formData.steps.splice(index, 1)
  // 重新排列步骤序号
  formData.steps.forEach((step, idx) => {
    step.stepNumber = idx + 1
  })
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true

    const data = {
      ...formData,
      ingredients: formData.ingredients.filter(i => i.ingredientId),
      steps: formData.steps.filter(s => s.description.trim())
    }

    if (isEdit.value) {
      await adminRecipeApi.update(formData.id, data)
      ElMessage.success('更新成功')
    } else {
      await adminRecipeApi.create(data)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadData()
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
  loadOptions()
  loadData()
})
</script>

<style scoped>
.recipe-management {
  padding: 0;
}

.recipe-card {
  border-radius: 14px;
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
  font-size: 12px;
  color: #8a97a8;
  margin-top: 2px;
}

.table-image-error {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa, #e9ecef);
  color: #adb5bd;
}

.recipe-cover {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  border: 1px solid #eef1f4;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-form {
  margin-bottom: 12px;
  padding: 14px 14px 2px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f7fbff 0%, #f8f9fc 100%);
  border: 1px solid #e8edf3;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.table-toolbar {
  margin: 8px 0 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #edf0f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-tags,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recipe-detail {
  max-height: 600px;
  overflow-y: auto;
}

@media (max-width: 900px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
