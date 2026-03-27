<template>
  <div class="recipe-card" @click="goDetail">
    <div class="recipe-image">
      <el-image :src="recipe.image || defaultImage" fit="cover" lazy>
        <template #error>
          <div class="image-placeholder">
            <el-icon :size="40">
              <Picture />
            </el-icon>
          </div>
        </template>
      </el-image>
      <div class="image-overlay">
        <div v-if="displayDifficulty" class="difficulty-tag" :class="getDifficultyClass(displayDifficulty)">
          {{ displayDifficulty }}
        </div>
        <div class="time-tag">
          <el-icon>
            <Timer />
          </el-icon>
          {{ displayTime }}
        </div>
      </div>
    </div>
    <div class="recipe-info">
      <h3 class="recipe-title">{{ recipe.title || recipe.name }}</h3>
      <p v-if="displayReason" class="recommend-reason">{{ displayReason }}</p>
      <p class="recipe-author">
        <el-icon>
          <User />
        </el-icon>
        {{ recipe.author || '佚名' }}
      </p>
      <div v-if="recipe.sceneTags?.length" class="scene-tags">
        <span v-for="tag in recipe.sceneTags.slice(0, 2)" :key="tag" class="scene-tag">{{ tag }}</span>
      </div>
      <div class="recipe-meta">
        <span class="meta-item">
          <el-icon>
            <Star />
          </el-icon>
          {{ recipe.likeCount || 0 }}
        </span>
        <span class="meta-item">
          <el-icon>
            <CollectionTag />
          </el-icon>
          {{ recipe.favoriteCount || 0 }}
        </span>
        <span class="meta-item">
          <el-icon>
            <ChatDotRound />
          </el-icon>
          {{ recipe.replyCount || 0 }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Picture, Star, Timer, User, ChatDotRound, CollectionTag } from '@element-plus/icons-vue'
import { trackBehavior } from '@/utils/tracker'

const props = defineProps({
  recipe: { type: Object, required: true }
})

const router = useRouter()
const route = useRoute()
const defaultImage = '/images/food-placeholder.svg'
const displayTime = computed(() => props.recipe.timeCostName || props.recipe.time || '未知')
const displayDifficulty = computed(() => props.recipe.difficultyName || props.recipe.difficulty || '')
const displayReason = computed(() => {
  const reasons = Array.isArray(props.recipe.reasons)
    ? props.recipe.reasons.filter(reason => typeof reason === 'string' && reason.trim())
    : []
  if (reasons.length === 0) {
    return ''
  }
  if (reasons.length === 1) {
    return reasons[0]
  }
  return reasons[Math.floor(Math.random() * reasons.length)]
})

const goDetail = () => {
  trackBehavior('recipe_click', {
    recipeId: props.recipe.id,
    sourcePage: route.path
  })
  router.push(`/recipe/${props.recipe.id}`)
}

const getDifficultyClass = (difficulty) => {
  const map = {
    '简单': 'easy',
    '普通': 'normal',
    '高级': 'hard'
  }
  return map[difficulty] || 'normal'
}
</script>

<style scoped>
.recipe-card {
  background: #fff;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: var(--transition);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs);
}

.recipe-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: #d2dbe8;
}

.recipe-image {
  position: relative;
  height: clamp(160px, 17vw, 208px);
  overflow: hidden;
}

.recipe-image .el-image {
  width: 100%;
  height: 100%;
  transition: transform 0.5s ease;
}

.recipe-card:hover .recipe-image .el-image {
  transform: scale(1.1);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa, #e9ecef);
  color: var(--text-secondary);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 10px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.difficulty-tag {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
  backdrop-filter: blur(10px);
}

.difficulty-tag.easy {
  background: rgba(81, 207, 102, 0.9);
  color: #fff;
}

.difficulty-tag.normal {
  background: rgba(254, 196, 25, 0.9);
  color: #fff;
}

.difficulty-tag.hard {
  background: rgba(255, 107, 107, 0.9);
  color: #fff;
}

.time-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  font-size: 11px;
  color: #fff;
}

.recipe-info {
  padding: 14px 14px 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.recipe-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.35;
  min-height: 40px;
  transition: var(--transition);
}

.recipe-card:hover .recipe-title {
  color: var(--primary-color);
}

.recipe-author {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-regular);
  margin-bottom: 10px;
}

.recommend-reason {
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--primary-color);
  line-height: 1.4;
}

.scene-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.scene-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  color: #0f766e;
  background: #ecfeff;
  border: 1px solid #bbf7d0;
}

.recipe-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--border-color);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-item .el-icon {
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .recipe-image {
    height: 160px;
  }

  .recipe-info {
    padding: 12px;
  }

  .recipe-meta {
    gap: 6px;
  }
}

@media (max-width: 430px) {
  .recipe-image {
    height: 150px;
  }

  .recipe-title {
    font-size: 14px;
    min-height: 38px;
  }

  .meta-item {
    font-size: 12px;
  }
}
</style>
