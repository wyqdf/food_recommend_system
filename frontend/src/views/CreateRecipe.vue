<template>
  <div class="create-recipe-page">
    <div class="page-container">
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">
            <el-icon class="title-icon">
              <Notebook />
            </el-icon>
            创建我的菜谱
          </h1>
          <p class="page-subtitle">分享你的独家美味，让更多人品尝你的手艺</p>
        </div>
      </div>

      <div class="form-container">
        <el-form :model="formData" :rules="rules" ref="formRef" label-position="top">
          <div class="form-section">
            <div class="section-title">
              <span class="section-number">1</span>
              <span>基本信息</span>
            </div>

            <div class="form-grid">
              <div class="form-item full-width">
                <el-form-item label="菜谱标题" prop="title">
                  <el-input v-model="formData.title" placeholder="给你的菜谱起个响亮的名字" size="large" />
                </el-form-item>
              </div>

              <div class="form-item">
                <el-form-item label="封面图片" prop="image">
                  <div class="image-input-wrapper">
                    <div class="image-upload-row">
                      <el-input v-model="formData.image" placeholder="输入图片URL" size="large" class="flex-grow-input">
                        <template #prefix>
                          <el-icon>
                            <Picture />
                          </el-icon>
                        </template>
                      </el-input>
                      <el-upload
                        class="image-uploader"
                        :show-file-list="false"
                        :before-upload="handleImageUpload"
                        accept="image/*"
                      >
                        <el-button type="primary" size="large">
                          <el-icon><Upload /></el-icon>
                          上传图片
                        </el-button>
                      </el-upload>
                    </div>
                    <div v-if="formData.image" class="image-preview">
                      <el-image :src="formData.image" fit="cover">
                        <template #error>
                          <div class="image-error-small">
                            <el-icon><Picture /></el-icon>
                            <span>加载失败</span>
                          </div>
                        </template>
                      </el-image>
                    </div>
                  </div>
                </el-form-item>
              </div>

              <div class="form-item">
                <el-form-item label="所需厨具" prop="cookware">
                  <el-select 
                    v-model="formData.cookware" 
                    placeholder="选择或输入厨具" 
                    size="large"
                    filterable
                    allow-create
                    default-first-option
                    :reserve-keyword="false"
                    class="full-width-select"
                  >
                    <el-option v-for="item in cookwares" :key="item.id" :label="item.name" :value="item.name" />
                  </el-select>
                </el-form-item>
              </div>
            </div>
          </div>

          <div class="form-section">
            <div class="section-title">
              <span class="section-number">2</span>
              <span>菜谱属性</span>
            </div>

            <div class="attributes-grid">
              <div class="attribute-card">
                <div class="attribute-icon difficulty">
                  <el-icon>
                    <TrendCharts />
                  </el-icon>
                </div>
                <div class="attribute-content">
                  <label>难度</label>
                  <el-select 
                    v-model="formData.difficultyId" 
                    placeholder="选择或输入难度" 
                    size="large"
                    filterable
                    allow-create
                    default-first-option
                    :reserve-keyword="false"
                  >
                    <el-option v-for="item in difficulties" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </div>
              </div>

              <div class="attribute-card">
                <div class="attribute-icon taste">
                  <el-icon>
                    <Grape />
                  </el-icon>
                </div>
                <div class="attribute-content">
                  <label>口味</label>
                  <el-select 
                    v-model="formData.tasteId" 
                    placeholder="选择或输入口味" 
                    size="large"
                    filterable
                    allow-create
                    default-first-option
                    :reserve-keyword="false"
                  >
                    <el-option v-for="item in tastes" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </div>
              </div>

              <div class="attribute-card">
                <div class="attribute-icon technique">
                  <el-icon>
                    <MagicStick />
                  </el-icon>
                </div>
                <div class="attribute-content">
                  <label>工艺</label>
                  <el-select 
                    v-model="formData.techniqueId" 
                    placeholder="选择或输入工艺" 
                    size="large"
                    filterable
                    allow-create
                    default-first-option
                    :reserve-keyword="false"
                  >
                    <el-option v-for="item in techniques" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </div>
              </div>

              <div class="attribute-card">
                <div class="attribute-icon time">
                  <el-icon>
                    <Timer />
                  </el-icon>
                </div>
                <div class="attribute-content">
                  <label>耗时</label>
                  <el-select 
                    v-model="formData.timeCostId" 
                    placeholder="选择或输入耗时" 
                    size="large"
                    filterable
                    allow-create
                    default-first-option
                    :reserve-keyword="false"
                  >
                    <el-option v-for="item in timeCosts" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </div>
              </div>
            </div>
          </div>

          <div class="form-section">
            <div class="section-title">
              <span class="section-number">3</span>
              <span>菜谱描述</span>
            </div>

            <div class="description-area">
              <el-form-item label="简介" prop="description">
                <el-input v-model="formData.description" type="textarea" :rows="4"
                  placeholder="描述一下这道菜的特色、故事或者适合的场景..." />
              </el-form-item>

              <el-form-item label="小贴士" prop="tips">
                <el-input v-model="formData.tips" type="textarea" :rows="3" placeholder="分享一些烹饪技巧或注意事项..." />
              </el-form-item>
            </div>
          </div>

          <div class="form-section">
            <div class="section-title">
              <span class="section-number">4</span>
              <span>食材清单</span>
            </div>

            <div class="ingredients-container">
              <div class="ingredients-list">
                <div v-for="(ingredient, index) in formData.ingredients" :key="index" class="ingredient-item">
                  <div class="ingredient-index">{{ index + 1 }}</div>
                  <el-select v-model="ingredient.ingredientId" placeholder="输入或选择食材" filterable allow-create
                    size="large" class="ingredient-name" @change="(val) => handleIngredientChange(val, ingredient)">
                    <el-option v-for="item in filteredIngredients(ingredient.ingredientId)" :key="item.id"
                      :label="item.name" :value="item.id" />
                  </el-select>
                  <el-select v-model="ingredient.type" placeholder="类型" size="large" class="ingredient-type">
                    <el-option label="主料" value="main" />
                    <el-option label="辅料" value="sub" />
                    <el-option label="调料" value="seasoning" />
                  </el-select>
                  <el-input v-model="ingredient.quantity" placeholder="用量" size="large" class="ingredient-quantity" />
                  <el-button type="danger" :icon="Delete" circle @click="removeIngredient(index)" class="remove-btn" />
                </div>
              </div>

              <el-button type="primary" plain @click="addIngredient" class="add-ingredient-btn">
                <el-icon>
                  <Plus />
                </el-icon>
                添加食材
              </el-button>
            </div>
          </div>

          <div class="form-section">
            <div class="section-title">
              <span class="section-number">5</span>
              <span>烹饪步骤</span>
            </div>

            <div class="steps-container">
              <transition-group name="step-list">
                <div v-for="(step, index) in formData.steps" :key="index" class="step-card">
                  <div class="step-left">
                    <div class="step-number-circle">{{ index + 1 }}</div>
                  </div>
                  <div class="step-content">
                    <el-input v-model="step.description" type="textarea" :rows="3" placeholder="详细描述这一步的操作..." />
                    <div class="step-image-row">
                      <el-input v-model="step.image" placeholder="步骤图片URL（可选）" class="step-image-input">
                        <template #prefix>
                          <el-icon>
                            <Picture />
                          </el-icon>
                        </template>
                      </el-input>
                      <el-upload
                        class="step-image-uploader"
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
                    <el-image v-if="step.image" :src="step.image" class="step-preview-image" fit="cover" />
                  </div>
                  <div class="step-actions">
                    <el-button type="danger" text @click="removeStep(index)">
                      <el-icon>
                        <Delete />
                      </el-icon>
                      删除
                    </el-button>
                  </div>
                </div>
              </transition-group>

              <el-button type="primary" plain @click="addStep" class="add-step-btn">
                <el-icon>
                  <Plus />
                </el-icon>
                添加步骤
              </el-button>
            </div>
          </div>

          <div class="form-section">
            <div class="section-title">
              <span class="section-number">6</span>
              <span>菜谱分类</span>
            </div>

            <div class="category-container">
              <div class="category-input-wrapper">
                <el-autocomplete v-model="categorySearch" :fetch-suggestions="searchCategories"
                  placeholder="输入分类名称，如：家常菜、川菜..." size="large" clearable @select="handleCategorySelect"
                  @keyup.enter="handleCategoryAdd" class="category-input">
                  <template #prefix>
                    <el-icon>
                      <Search />
                    </el-icon>
                  </template>
                  <template #suffix>
                    <el-button type="primary" @click="handleCategoryAdd" size="small">添加</el-button>
                  </template>
                </el-autocomplete>
                <div class="category-hints">
                  <span class="hint-text">热门分类：</span>
                  <el-tag v-for="cat in hotCategories" :key="cat.id" @click="addCategoryById(cat.id)" class="hint-tag">
                    {{ cat.name }}
                  </el-tag>
                </div>
              </div>

              <div class="selected-categories">
                <transition-group name="tag-list">
                  <el-tag v-for="id in formData.categoryIds" :key="id" closable size="large" @close="removeCategory(id)"
                    class="category-tag">
                    {{ getCategoryName(id) }}
                  </el-tag>
                </transition-group>
                <div v-if="formData.categoryIds.length === 0" class="empty-categories">
                  <el-icon>
                    <FolderOpened />
                  </el-icon>
                  <span>请选择或输入分类</span>
                </div>
              </div>
            </div>
          </div>
        </el-form>

        <div class="form-actions">
          <el-button size="large" @click="handleCancel">取消</el-button>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitLoading" class="submit-btn">
            <el-icon>
              <Check />
            </el-icon>
            发布菜谱
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus, Delete, Notebook, Picture, TrendCharts, Grape, MagicStick, Timer,
  Search, FolderOpened, Check, Upload
} from '@element-plus/icons-vue'
import { recipeApi, attributeApi, uploadApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const categorySearch = ref('')
const imageUploadLoading = ref(false)

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

const formData = reactive({
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
  title: [{ required: true, message: '请输入菜谱标题', trigger: 'blur' }],
  difficultyId: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

const difficulties = ref([])
const tastes = ref([])
const techniques = ref([])
const timeCosts = ref([])
const ingredients = ref([])
const categories = ref([])
const cookwares = ref([])

const hotCategories = computed(() => categories.value.slice(0, 8))

const filteredIngredients = (currentId) => {
  return ingredients.value
}

const getCategoryName = (id) => {
  const cat = categories.value.find(c => c.id === id)
  return cat ? cat.name : '未知分类'
}

const searchCategories = (queryString, cb) => {
  const results = queryString
    ? categories.value
      .filter(c => c.name.toLowerCase().includes(queryString.toLowerCase()))
      .map(c => ({ value: c.name, id: c.id }))
    : categories.value.slice(0, 10).map(c => ({ value: c.name, id: c.id }))
  cb(results)
}

const handleCategorySelect = (item) => {
  if (!formData.categoryIds.includes(item.id)) {
    formData.categoryIds.push(item.id)
  }
  categorySearch.value = ''
}

const handleCategoryAdd = () => {
  if (!categorySearch.value.trim()) return

  const existingCategory = categories.value.find(
    c => c.name.toLowerCase() === categorySearch.value.trim().toLowerCase()
  )

  if (existingCategory) {
    if (!formData.categoryIds.includes(existingCategory.id)) {
      formData.categoryIds.push(existingCategory.id)
    }
  } else {
    const newId = Date.now()
    categories.value.push({ id: newId, name: categorySearch.value.trim() })
    formData.categoryIds.push(newId)
  }

  categorySearch.value = ''
}

const addCategoryById = (id) => {
  if (!formData.categoryIds.includes(id)) {
    formData.categoryIds.push(id)
  }
}

const removeCategory = (id) => {
  const index = formData.categoryIds.indexOf(id)
  if (index > -1) {
    formData.categoryIds.splice(index, 1)
  }
}

const addIngredient = () => {
  formData.ingredients.push({ ingredientId: null, ingredientName: '', type: 'main', quantity: '' })
}

const handleIngredientChange = (val, ingredient) => {
  if (typeof val === 'string') {
    ingredient.ingredientName = val
    ingredient.ingredientId = Date.now()
  } else if (typeof val === 'number') {
    ingredient.ingredientId = val
    ingredient.ingredientName = ''
  }
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

const handleCancel = () => {
  router.back()
}

const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate().catch(err => {
      const firstError = Object.values(err)[0]
      if (firstError && firstError[0]) {
        ElMessage.error(firstError[0].message)
      }
      return false
    })
    if (!valid) return

    submitLoading.value = true

    const user = userStore.user
    formData.author = user?.nickname || user?.username || ''
    formData.authorUid = user?.id?.toString() || ''

    const validCategoryIds = []
    const categoryNames = []

    formData.categoryIds.forEach(id => {
      const cat = categories.value.find(c => c.id === id)
      if (cat) {
        if (id < 10000) {
          validCategoryIds.push(id)
        } else {
          categoryNames.push(cat.name)
        }
      }
    })

    const processedIngredients = formData.ingredients
      .filter(i => i.ingredientId || i.ingredientName)
      .map(i => {
        if (i.ingredientId && i.ingredientId < 10000) {
          return { ingredientId: i.ingredientId, type: i.type, quantity: i.quantity }
        } else {
          const name = i.ingredientName || ingredients.value.find(ing => ing.id === i.ingredientId)?.name
          return { ingredientName: name, type: i.type, quantity: i.quantity }
        }
      })

    const data = {
      title: formData.title,
      author: formData.author,
      authorUid: formData.authorUid,
      image: formData.image,
      description: formData.description,
      tips: formData.tips,
      cookware: formData.cookware,
      difficultyId: typeof formData.difficultyId === 'number' ? formData.difficultyId : null,
      difficultyName: typeof formData.difficultyId === 'string' ? formData.difficultyId : null,
      tasteId: typeof formData.tasteId === 'number' ? formData.tasteId : null,
      tasteName: typeof formData.tasteId === 'string' ? formData.tasteId : null,
      techniqueId: typeof formData.techniqueId === 'number' ? formData.techniqueId : null,
      techniqueName: typeof formData.techniqueId === 'string' ? formData.techniqueId : null,
      timeCostId: typeof formData.timeCostId === 'number' ? formData.timeCostId : null,
      timeCostName: typeof formData.timeCostId === 'string' ? formData.timeCostId : null,
      categoryIds: validCategoryIds,
      categoryNames: categoryNames,
      ingredients: processedIngredients,
      steps: formData.steps.filter(s => s.description.trim())
    }

    const res = await recipeApi.create(data)
    if (res.code === 200) {
      ElMessage.success('菜谱创建成功！')
      router.push('/')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('创建失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

const loadOptions = async () => {
  loading.value = true
  try {
    const [diffRes, tasteRes, techRes, timeRes, ingRes, catRes, cookRes] = await Promise.all([
      attributeApi.getDifficulties(),
      attributeApi.getTastes(),
      attributeApi.getTechniques(),
      attributeApi.getTimeCosts(),
      attributeApi.getIngredients(),
      recipeApi.getCategories(),
      attributeApi.getCookwares()
    ])

    difficulties.value = diffRes.data || []
    tastes.value = tasteRes.data || []
    techniques.value = techRes.data || []
    timeCosts.value = timeRes.data || []
    ingredients.value = ingRes.data || []
    categories.value = catRes.data || []
    cookwares.value = cookRes.data || []
  } catch (error) {
    console.error('加载选项失败:', error)
    ElMessage.error('加载选项失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOptions()
  formData.ingredients = [{ ingredientId: null, ingredientName: '', type: 'main', quantity: '' }]
  formData.steps = [{ stepNumber: 1, description: '', image: '' }]
})
</script>

<style scoped>
.create-recipe-page {
  min-height: 100vh;
  background: radial-gradient(circle at 8% 8%, #fff3ea 0%, #f6f8fb 52%);
  padding: 40px 20px;
}

.page-container {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.header-content {
  color: var(--text-primary);
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.title-icon {
  font-size: 40px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
}

.form-container {
  background: white;
  border-radius: 24px;
  padding: 40px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
}

.form-section {
  margin-bottom: 40px;
  padding-bottom: 30px;
  border-bottom: 1px solid #f0f0f0;
}

.form-section:last-of-type {
  border-bottom: none;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24px;
}

.section-number {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-item {
  margin-bottom: 0;
}

.form-item.full-width {
  grid-column: 1 / -1;
}

.image-input-wrapper {
  position: relative;
}

.image-upload-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.image-uploader {
  flex-shrink: 0;
}

.image-preview {
  margin-top: 12px;
  border-radius: 12px;
  overflow: hidden;
  max-width: 200px;
}

.image-preview :deep(.el-image) {
  width: 100%;
  height: 120px;
}

.image-error-small {
  width: 100%;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa, #e9ecef);
  color: var(--text-secondary);
  gap: 4px;
  font-size: 12px;
}

.attributes-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.attribute-card {
  background: linear-gradient(180deg, #f8fbff 0%, #f6f9ff 100%);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e6edf6;
  transition: all 0.3s ease;
}

.attribute-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.attribute-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.attribute-icon.difficulty {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.attribute-icon.taste {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.attribute-icon.technique {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.attribute-icon.time {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.attribute-content {
  flex: 1;
}

.attribute-content label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.description-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ingredients-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ingredients-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ingredient-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8fbff;
  border-radius: 12px;
  border: 1px solid #e7eef8;
  transition: all 0.3s ease;
}

.ingredient-item:hover {
  background: #f3f8ff;
}

.ingredient-index {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.ingredient-name {
  flex: 2;
}

.ingredient-type {
  width: 100px;
}

.ingredient-quantity {
  width: 100px;
}

.remove-btn {
  flex-shrink: 0;
}

.add-ingredient-btn {
  align-self: flex-start;
  border-radius: 12px;
  padding: 12px 24px;
}

.steps-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-card {
  display: flex;
  gap: 20px;
  padding: 24px;
  background: #f8fbff;
  border-radius: 16px;
  border: 1px solid #e7eef8;
  transition: all 0.3s ease;
}

.step-card:hover {
  background: #f3f8ff;
}

.step-left {
  flex-shrink: 0;
}

.step-number-circle {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

.step-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-image-input {
  margin-top: 8px;
}

.step-image-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 8px;
}

.step-image-row .step-image-input {
  margin-top: 0;
  flex: 1;
}

.step-image-uploader {
  flex-shrink: 0;
}

.step-actions {
  display: flex;
  align-items: flex-start;
}

.add-step-btn {
  align-self: flex-start;
  border-radius: 12px;
  padding: 12px 24px;
}

.category-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-input {
  width: 100%;
}

.category-hints {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hint-text {
  font-size: 14px;
  color: #666;
}

.hint-tag {
  cursor: pointer;
  transition: all 0.3s ease;
}

.hint-tag:hover {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  color: white;
  border-color: transparent;
}

.selected-categories {
  min-height: 60px;
  padding: 16px;
  background: #f8fbff;
  border-radius: 12px;
  border: 1px solid #e7eef8;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.category-tag {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
}

.empty-categories {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #f0f0f0;
}

.submit-btn {
  padding: 12px 32px;
  border-radius: 12px;
  font-size: 16px;
}

.flex-grow-input {
  flex: 1;
}

.full-width-select {
  width: 100%;
}

.step-preview-image {
  width: 100px;
  height: 100px;
  margin-top: 10px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
}

.step-list-enter-active,
.step-list-leave-active {
  transition: all 0.3s ease;
}

.step-list-enter-from,
.step-list-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

.tag-list-enter-active,
.tag-list-leave-active {
  transition: all 0.3s ease;
}

.tag-list-enter-from,
.tag-list-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

@media (max-width: 768px) {
  .create-recipe-page {
    padding: 20px 10px;
  }

  .page-title {
    font-size: 28px;
  }

  .form-container {
    padding: 24px;
    border-radius: 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .attributes-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .ingredient-item {
    flex-wrap: wrap;
  }

  .ingredient-name {
    flex: 1 1 100%;
  }

  .ingredient-type,
  .ingredient-quantity {
    flex: 1;
  }

  .step-card {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
