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
        <div v-if="recipe.difficultyName" class="difficulty-tag" :class="getDifficultyClass(recipe.difficultyName)">
          {{ recipe.difficultyName }}
        </div>
        <div class="time-tag">
          <el-icon>
            <Timer />
          </el-icon>
          {{ recipe.timeCostName || '未知' }}
        </div>
      </div>
    </div>
    <div class="recipe-info">
      <h3 class="recipe-title">{{ recipe.title || recipe.name }}</h3>
      <p class="recipe-author">
        <el-icon>
          <User />
        </el-icon>
        {{ recipe.author || '佚名' }}
      </p>
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
import { useRouter } from 'vue-router'
import { Picture, Star, Timer, User, ChatDotRound, CollectionTag } from '@element-plus/icons-vue'

const props = defineProps({
  recipe: { type: Object, required: true }
})

const router = useRouter()
const defaultImage = '/images/food-placeholder.svg'

const goDetail = () => {
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
  transition: var(--transition);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs);
}

.recipe-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
  border-color: #d2dbe8;
}

.recipe-image {
  position: relative;
  height: 180px;
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
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.difficulty-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
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
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  font-size: 12px;
  color: #fff;
}

.recipe-info {
  padding: 16px 16px 14px;
}

.recipe-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  margin-bottom: 12px;
}

.recipe-meta {
  display: flex;
  gap: 16px;
  padding-top: 12px;
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
</style>
