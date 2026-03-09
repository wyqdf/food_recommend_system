<template>
  <div class="detail-page container page-shell">
    <el-skeleton v-if="loading" :rows="10" animated />
    <template v-else-if="recipe">
      <div class="recipe-header">
        <h1>{{ recipe.title || recipe.name }}</h1>
        <div class="recipe-meta">
          <span><el-icon>
              <Star />
            </el-icon> {{ recipe.likeCount || 0 }} 点赞</span>
          <span><el-icon>
              <CollectionTag />
            </el-icon> {{ recipe.favoriteCount || 0 }} 收藏</span>
          <span><el-icon>
              <ChatDotRound />
            </el-icon> {{ recipe.replyCount || 0 }} 评论</span>
          <span><el-icon>
              <Timer />
            </el-icon> {{ recipe.timeCostName || recipe.time || '未知' }}</span>
          <span><el-icon>
              <Dish />
            </el-icon> {{ recipe.difficultyName || recipe.difficulty || '未知' }}</span>
        </div>
      </div>

      <el-row :gutter="24">
        <el-col :xs="24" :lg="16">
          <el-card class="main-card">
            <div class="recipe-image">
              <el-image :src="recipe.image || defaultImage" fit="cover">
                <template #error>
                  <div class="image-error-placeholder">
                    <el-icon :size="60"><Picture /></el-icon>
                    <span>图片加载失败</span>
                  </div>
                </template>
              </el-image>
            </div>

            <div class="recipe-properties">
              <el-tag v-if="recipe.tasteName">{{ recipe.tasteName }}</el-tag>
              <el-tag v-if="recipe.techniqueName" type="success">{{ recipe.techniqueName }}</el-tag>
              <el-tag v-if="recipe.timeCostName" type="warning">{{ recipe.timeCostName }}</el-tag>
              <el-tag v-if="recipe.difficultyName" type="info">{{ recipe.difficultyName }}</el-tag>
            </div>

            <div v-if="recipe.description" class="section">
              <h3>简介</h3>
              <div class="description-list">
                <p v-for="(para, idx) in descriptionParagraphs" :key="idx">{{ para }}</p>
              </div>
            </div>

            <div class="section">
              <h3>食材清单</h3>
              <div v-if="mainIngredients.length" class="ingredient-section">
                <div class="ingredient-type">主料</div>
                <el-card class="ingredient-card">
                  <div class="ingredient-grid">
                    <div v-for="ing in mainIngredients" :key="ing.id" class="ingredient-item">
                      <div class="ingredient-name">{{ ing.name }}</div>
                      <div class="ingredient-quantity">{{ ing.quantity }}</div>
                    </div>
                  </div>
                </el-card>
              </div>
              <div v-if="subIngredients.length" class="ingredient-section">
                <div class="ingredient-type">辅料</div>
                <el-card class="ingredient-card">
                  <div class="ingredient-grid">
                    <div v-for="ing in subIngredients" :key="ing.id" class="ingredient-item">
                      <div class="ingredient-name">{{ ing.name }}</div>
                      <div class="ingredient-quantity">{{ ing.quantity }}</div>
                    </div>
                  </div>
                </el-card>
              </div>
              <div v-if="seasoningIngredients.length" class="ingredient-section">
                <div class="ingredient-type">调料</div>
                <el-card class="ingredient-card">
                  <div class="ingredient-grid">
                    <div v-for="ing in seasoningIngredients" :key="ing.id" class="ingredient-item">
                      <div class="ingredient-name">{{ ing.name }}</div>
                      <div class="ingredient-quantity">{{ ing.quantity }}</div>
                    </div>
                  </div>
                </el-card>
              </div>
            </div>

            <div class="section">
              <h3>烹饪步骤</h3>
              <div class="steps-list">
                <div v-for="(step, index) in steps" :key="index" class="step-item">
                  <div class="step-number">{{ index + 1 }}</div>
                  <div class="step-content">
                    <div class="step-description">{{ step.description }}</div>
                    <el-image v-if="step.image" :src="step.image" class="step-image" fit="cover" :preview-src-list="[step.image]" />
                  </div>
                </div>
              </div>
            </div>

            <div v-if="recipe.tips" class="section">
              <h3>小贴士</h3>
              <p class="tips">{{ recipe.tips }}</p>
            </div>
          </el-card>

          <el-card class="comment-card">
            <template #header>
              <span>评论区 ({{ commentTotal }})</span>
            </template>
            <div class="comment-form">
              <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="写下你的评论..." />
              <el-button type="primary" @click="submitComment" :loading="submitting">发表评论</el-button>
            </div>
            <div class="comment-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <el-avatar :size="40" :src="comment.avatar">{{ comment.username?.charAt(0) || 'U' }}</el-avatar>
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="username">{{ comment.username || '匿名用户' }}</span>
                    <span class="time">{{ comment.publishTime }}</span>
                  </div>
                  <p>{{ comment.content }}</p>
                </div>
              </div>
              <el-empty v-if="!comments.length" description="暂无评论" />
            </div>
            <div class="comment-pagination" v-if="commentTotal > commentPageSize">
              <el-pagination v-model:current-page="commentPage" :page-size="commentPageSize" :total="commentTotal"
                layout="prev, pager, next" @current-change="handleCommentPageChange" />
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card class="side-card">
            <el-button type="primary" :icon="Star" @click="toggleFavorite" :loading="favoriting">
              {{ isFavorited ? '取消收藏' : '收藏菜谱' }}
            </el-button>
            <el-button :icon="Share">分享</el-button>
          </el-card>

          <el-card class="side-card">
            <template #header>相关推荐</template>
            <div v-if="recommendList.length === 0" class="no-recommend">
              <el-empty description="暂无相关推荐" :image-size="80" />
            </div>
            <div v-else class="recommend-list">
              <div v-for="rec in recommendList" :key="rec.id" class="recommend-item" @click="goRecipe(rec.id)">
                <el-avatar :size="40" :src="rec.image" shape="square">{{ (rec.title || rec.name)?.charAt(0)
                  }}</el-avatar>
                <div class="recommend-info">
                  <div class="recommend-title">{{ rec.title || rec.name }}</div>
                  <div class="recommend-meta">
                    <span class="recommend-difficulty">{{ rec.difficulty }}</span>
                    <span class="recommend-time">{{ rec.time }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, View, Star, ChatDotRound, Share, Timer, Dish, CollectionTag, Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { recipeApi, favoriteApi, commentApi } from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const recipe = ref(null)
const steps = ref([])
const ingredients = ref([])
const comments = ref([])
const recommendList = ref([])
const loading = ref(true)
const isFavorited = ref(false)
const favoriting = ref(false)
const commentContent = ref('')
const submitting = ref(false)
const defaultImage = '/images/food-placeholder.svg'
const commentPage = ref(1)
const commentPageSize = ref(10)
const commentTotal = ref(0)

const descriptionParagraphs = computed(() => {
  if (!recipe.value?.description) return []
  return recipe.value.description.split('|').map(p => p.trim()).filter(p => p)
})

const mainIngredients = computed(() => {
  const unique = Array.from(new Map(
    ingredients.value.filter(i => i.type === 'main').map(i => [i.name, i])
  ).values())
  return unique
})
const subIngredients = computed(() => {
  const unique = Array.from(new Map(
    ingredients.value.filter(i => i.type === 'sub').map(i => [i.name, i])
  ).values())
  return unique
})
const seasoningIngredients = computed(() => {
  const unique = Array.from(new Map(
    ingredients.value.filter(i => i.type === 'seasoning').map(i => [i.name, i])
  ).values())
  return unique
})

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await recipeApi.getDetail(route.params.id)
    recipe.value = res.data
    steps.value = res.data.steps || []
    ingredients.value = res.data.ingredients || []
  } catch (error) {
    ElMessage.error('加载菜谱详情失败，请检查网络后重试')
    console.error('Recipe detail fetch error:', error)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchDetail()
  if (userStore.isLoggedIn) {
    const favRes = await favoriteApi.check(route.params.id)
    isFavorited.value = favRes.data.isFavorite
  }

  await fetchComments()

  const recommendRes = await recipeApi.getSimilar(route.params.id, { limit: 5 })
  recommendList.value = Array.isArray(recommendRes.data) ? recommendRes.data : (recommendRes.data.list || [])
})

watch(() => route.params.id, async (newId) => {
  if (newId) {
    loading.value = true
    await fetchDetail()
    if (userStore.isLoggedIn) {
      const favRes = await favoriteApi.check(newId)
      isFavorited.value = favRes.data.isFavorite
    }
    commentPage.value = 1
    await fetchComments()
    const recommendRes = await recipeApi.getSimilar(newId, { limit: 5 })
    recommendList.value = Array.isArray(recommendRes.data) ? recommendRes.data : (recommendRes.data.list || [])
    loading.value = false
  }
})

const toggleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  favoriting.value = true
  try {
    if (isFavorited.value) {
      await favoriteApi.remove(recipe.value.id)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteApi.add(recipe.value.id)
      isFavorited.value = true
      ElMessage.success('收藏成功')
    }
  } finally {
    favoriting.value = false
  }
}

const submitComment = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    await commentApi.add({ recipeId: recipe.value.id, content: commentContent.value })
    ElMessage.success('评论成功')
    commentContent.value = ''
    fetchComments()
  } finally {
    submitting.value = false
  }
}

const fetchComments = async () => {
  const res = await commentApi.getList(route.params.id, { page: commentPage.value, pageSize: commentPageSize.value })
  comments.value = res.data.list || []
  commentTotal.value = res.data.total || 0
}

const handleCommentPageChange = (page) => {
  commentPage.value = page
  fetchComments()
}

const goRecipe = (id) => {
  router.push(`/recipe/${id}`)
}


</script>

<style scoped>
.detail-page {
  padding-top: 0;
}

.recipe-header {
  margin-bottom: 24px;
}

.recipe-header h1 {
  font-size: 30px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.recipe-meta {
  display: flex;
  gap: 18px;
  color: var(--text-secondary);
  font-size: 14px;
  flex-wrap: wrap;
}

.recipe-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.recipe-meta .el-icon {
  color: var(--primary-color);
}

.main-card {
  margin-bottom: 20px;
  border-radius: var(--radius-md);
}

.recipe-image {
  margin-bottom: 20px;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.recipe-image .el-image {
  width: 100%;
  height: 320px;
}

.image-error-placeholder {
  width: 100%;
  height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa, #e9ecef);
  color: var(--text-secondary);
  gap: 12px;
}

.recipe-properties {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.recipe-properties .el-tag {
  border-radius: 20px;
  padding: 6px 16px;
}

.section {
  margin-bottom: 26px;
}

.section h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--primary-color);
  display: inline-block;
}

.description-list p {
  text-indent: 2em;
  line-height: 1.8;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.ingredient-section {
  margin-bottom: 20px;
}

.ingredient-type {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-left: 12px;
  position: relative;
}

.ingredient-type::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  border-radius: 2px;
}

.ingredient-card {
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

.ingredient-card :deep(.el-card__body) {
  padding: 16px;
}

.ingredient-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.ingredient-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: var(--bg-color);
  border-radius: var(--radius-sm);
}

.ingredient-name {
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.ingredient-quantity {
  font-size: 13px;
  color: var(--text-secondary);
}

.steps-list {
  padding-left: 0;
}

.step-item {
  display: flex;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  transition: var(--transition);
}

.step-item:hover {
  background: #fff;
  box-shadow: var(--shadow-sm);
}

.step-number {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 600;
  font-size: 16px;
}

.step-content {
  flex: 1;
  line-height: 1.8;
  color: var(--text-primary);
}

.step-description {
  margin-bottom: 12px;
}

.step-image {
  max-width: 300px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: transform 0.3s ease;
}

.step-image:hover {
  transform: scale(1.02);
}

.tips {
  background: linear-gradient(135deg, #fff8e6, #fffbe8);
  padding: 20px;
  border-radius: var(--radius-md);
  color: #8b6914;
  border-left: 4px solid var(--warning-color);
}

.comment-card {
  margin-bottom: 18px;
  border-radius: var(--radius-md);
}

.comment-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  font-weight: 600;
  font-size: 16px;
}

.comment-form {
  margin-bottom: 24px;
}

.comment-form .el-button {
  margin-top: 12px;
  border-radius: 20px;
  padding: 10px 24px;
  min-height: 40px;
}

.comment-list {
  max-height: 440px;
  overflow-y: auto;
}

.comment-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-header .username {
  font-weight: 600;
  color: var(--text-primary);
}

.comment-header .time {
  font-size: 12px;
  color: var(--text-secondary);
}

.comment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.side-card {
  margin-bottom: 16px;
  border-radius: var(--radius-md);
}

.side-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-color);
  font-weight: 600;
}

.side-card :deep(.el-card__body) {
  padding: 16px 18px;
}

.side-card .el-button {
  width: 100%;
  min-height: 42px;
}

.side-card .el-button + .el-button {
  margin-top: 10px;
  margin-left: 0;
}

.no-recommend {
  padding: 20px 0;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
}

.recommend-item:hover {
  background: var(--bg-color);
}

.recommend-item .el-image {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.recommend-info {
  flex: 1;
  min-width: 0;
}

.recommend-title {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-meta {
  font-size: 12px;
  color: var(--text-secondary);
}

.favorite-btn {
  width: 100%;
  border-radius: var(--radius-sm);
}

.favorite-btn.is-favorite {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
}

@media (max-width: 1024px) {
  .recipe-header {
    margin-bottom: 20px;
  }

  .recipe-header h1 {
    font-size: 28px;
    margin-bottom: 12px;
  }
}

@media (max-width: 768px) {
  .recipe-header h1 {
    font-size: 24px;
  }

  .recipe-meta {
    gap: 10px 16px;
  }

  .recipe-image .el-image,
  .image-error-placeholder {
    height: 230px;
  }

  .main-card,
  .comment-card,
  .side-card {
    margin-bottom: 14px;
  }

  .section {
    margin-bottom: 24px;
  }

  .section h3 {
    font-size: 18px;
    margin-bottom: 12px;
  }

  .ingredient-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .step-item {
    gap: 12px;
    margin-bottom: 14px;
    padding: 12px;
  }

  .step-number {
    width: 34px;
    height: 34px;
    font-size: 14px;
  }

  .step-image {
    width: 100%;
    max-width: 100%;
  }

  .comment-item {
    gap: 10px;
    padding: 14px 0;
  }

  .comment-header {
    flex-direction: column;
    gap: 4px;
  }

  .side-card :deep(.el-card__body) {
    padding: 14px;
  }
}

@media (max-width: 430px) {
  .recipe-header h1 {
    font-size: 22px;
  }

  .recipe-meta {
    font-size: 13px;
  }

  .ingredient-grid {
    grid-template-columns: 1fr;
  }

  .step-item {
    flex-direction: column;
  }

  .comment-form .el-button {
    width: 100%;
  }
}
</style>
