<template>
  <div class="cooking-mode-page container page-shell">
    <el-skeleton v-if="loading" :rows="10" animated />
    <template v-else-if="recipe">
      <div class="header-row">
        <div>
          <h2 class="title">{{ recipe.title || recipe.name }}</h2>
          <p class="subtitle">烹饪模式 · 跟做导航</p>
        </div>
        <div class="header-right">
          <el-tag type="success">步骤 {{ currentStep }} / {{ totalSteps }}</el-tag>
          <el-tag type="info">进度 {{ progressPercent }}%</el-tag>
        </div>
      </div>

      <el-alert
        v-if="session?.resumed"
        title="已为你恢复上次烹饪进度"
        type="success"
        show-icon
        :closable="false"
        class="resume-alert"
      />

      <el-row :gutter="18">
        <el-col :xs="24" :lg="16">
          <el-card class="step-card">
            <template #header>
              <div class="step-header">
                <span>当前步骤</span>
                <el-tag type="warning" effect="light">{{ currentStepData?.step || currentStep }}</el-tag>
              </div>
            </template>
            <div v-if="currentStepData" class="step-main">
              <p class="step-text">{{ currentStepData.description }}</p>
              <el-image
                v-if="currentStepData.image"
                :src="currentStepData.image"
                fit="cover"
                class="step-image"
                :preview-src-list="[currentStepData.image]"
              />
            </div>
            <el-empty v-else description="暂无步骤数据" />
            <div class="action-row">
              <el-button @click="goPrev" :disabled="currentStep <= 1">上一步</el-button>
              <el-button type="primary" @click="goNext">
                {{ currentStep >= totalSteps ? '完成本次烹饪' : '下一步' }}
              </el-button>
              <el-button type="success" plain @click="finishSession" :loading="finishing">提前完成</el-button>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card class="timeline-card">
            <template #header>
              <div class="step-header">
                <span>步骤导航</span>
                <el-tag type="info" effect="light">{{ saving ? '保存中' : '自动保存' }}</el-tag>
              </div>
            </template>
            <div class="timeline-list">
              <button
                v-for="item in stepItems"
                :key="item.step"
                class="timeline-item"
                :class="{ active: item.step === currentStep, done: item.step < currentStep }"
                @click="jumpStep(item.step)"
              >
                <span class="num">{{ item.step }}</span>
                <span class="txt">{{ item.description }}</span>
              </button>
            </div>
          </el-card>

          <el-card class="timeline-card">
            <template #header>烹饪信息</template>
            <div class="meta-line">
              <span>会话 ID</span>
              <strong>{{ session?.sessionId || '-' }}</strong>
            </div>
            <div class="meta-line">
              <span>累计时长</span>
              <strong>{{ durationText }}</strong>
            </div>
            <div class="meta-line">
              <span>状态</span>
              <strong>{{ session?.status || '-' }}</strong>
            </div>
            <el-button type="primary" plain class="back-btn" @click="goBackDetail">返回菜谱详情</el-button>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cookingApi, recipeApi } from '@/api'
import { flushBehaviorEvents, trackBehavior } from '@/utils/tracker'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const finishing = ref(false)
const recipe = ref(null)
const session = ref(null)
const currentStep = ref(1)
const pageEnterAt = ref(Date.now())
const baseDurationMs = ref(0)
const lastSaveAt = ref(0)
const nowTick = ref(Date.now())
const leavingPage = ref(false)
let tickTimer = null

const stepItems = computed(() => {
  const raw = Array.isArray(recipe.value?.steps) ? recipe.value.steps : []
  return raw
    .map((item, idx) => ({
      ...item,
      step: item.step || idx + 1
    }))
    .sort((a, b) => a.step - b.step)
})

const totalSteps = computed(() => Math.max(stepItems.value.length, 1))

const currentStepData = computed(() => {
  const exact = stepItems.value.find(item => item.step === currentStep.value)
  if (exact) return exact
  return stepItems.value[currentStep.value - 1] || null
})

const progressPercent = computed(() => {
  if (!totalSteps.value) return 0
  return Math.min(100, Math.round(currentStep.value * 100 / totalSteps.value))
})

const durationMs = computed(() => {
  const live = nowTick.value - pageEnterAt.value
  return Math.max(0, baseDurationMs.value + live)
})

const durationText = computed(() => {
  const totalSec = Math.floor(durationMs.value / 1000)
  const min = Math.floor(totalSec / 60)
  const sec = totalSec % 60
  return `${min} 分 ${sec} 秒`
})

const fetchRecipe = async () => {
  const res = await recipeApi.getDetail(route.params.id)
  recipe.value = res.data
}

const startSession = async () => {
  const recipeId = Number(route.params.id)
  const res = await cookingApi.startSession(recipeId)
  session.value = res.data
  currentStep.value = Math.max(1, Math.min(res.data.currentStep || 1, totalSteps.value))
  baseDurationMs.value = Math.max(0, res.data.durationMs || 0)
  pageEnterAt.value = Date.now()
  trackBehavior('cooking_start', {
    sourcePage: 'cooking_mode',
    recipeId,
    stepNumber: currentStep.value
  })
}

const saveProgress = async (force = false, silent = false) => {
  if (!session.value?.sessionId || finishing.value || session.value?.status !== 'in_progress') return
  const now = Date.now()
  if (!force && now - lastSaveAt.value < 1200) {
    return
  }
  lastSaveAt.value = now
  saving.value = true
  try {
    const res = await cookingApi.updateProgress(session.value.sessionId, {
      currentStep: currentStep.value,
      durationMs: durationMs.value
    })
    session.value = { ...session.value, ...(res.data || {}) }
  } catch (error) {
    const message = error?.message || ''
    if (message.includes('烹饪会话不存在或已结束')) {
      session.value = { ...session.value, status: 'completed' }
      return
    }
    if (!silent && !leavingPage.value) {
      ElMessage.error('进度保存失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}

const goPrev = async () => {
  if (currentStep.value <= 1) return
  currentStep.value -= 1
  trackBehavior('cook_step_prev', {
    sourcePage: 'cooking_mode',
    recipeId: Number(route.params.id),
    stepNumber: currentStep.value
  })
  await saveProgress()
}

const goNext = async () => {
  if (currentStep.value >= totalSteps.value) {
    await finishSession()
    return
  }
  currentStep.value += 1
  trackBehavior('cook_step_next', {
    sourcePage: 'cooking_mode',
    recipeId: Number(route.params.id),
    stepNumber: currentStep.value
  })
  await saveProgress()
}

const jumpStep = async (step) => {
  if (step === currentStep.value || !step) return
  currentStep.value = step
  trackBehavior('cook_step_jump', {
    sourcePage: 'cooking_mode',
    recipeId: Number(route.params.id),
    stepNumber: step
  })
  await saveProgress()
}

const finishSession = async () => {
  if (!session.value?.sessionId || finishing.value) return
  finishing.value = true
  try {
    const res = await cookingApi.finishSession(session.value.sessionId, {
      durationMs: durationMs.value
    })
    session.value = { ...session.value, ...(res.data || {}) }
    leavingPage.value = true
    trackBehavior('cooking_finish', {
      sourcePage: 'cooking_mode',
      recipeId: Number(route.params.id),
      stepNumber: totalSteps.value,
      durationMs: durationMs.value
    })
    await flushBehaviorEvents()
    ElMessage.success('完成啦，已记录本次烹饪')
    router.push(`/recipe/${route.params.id}`)
  } finally {
    finishing.value = false
  }
}

const goBackDetail = () => {
  router.push(`/recipe/${route.params.id}`)
}

onMounted(async () => {
  loading.value = true
  tickTimer = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
  try {
    await fetchRecipe()
    await startSession()
  } catch (error) {
    console.error(error)
    ElMessage.error('烹饪模式初始化失败，请稍后重试')
    router.push(`/recipe/${route.params.id}`)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  leavingPage.value = true
  if (tickTimer) {
    window.clearInterval(tickTimer)
    tickTimer = null
  }
  trackBehavior('cooking_mode_leave', {
    sourcePage: 'cooking_mode',
    recipeId: Number(route.params.id),
    stepNumber: currentStep.value,
    durationMs: durationMs.value
  })
  saveProgress(true, true)
  flushBehaviorEvents()
})
</script>

<style scoped>
.cooking-mode-page {
  padding-top: 0;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.title {
  font-size: 28px;
  color: var(--text-primary);
}

.subtitle {
  margin-top: 6px;
  color: var(--text-secondary);
}

.header-right {
  display: flex;
  gap: 8px;
}

.resume-alert {
  margin-bottom: 14px;
}

.step-card,
.timeline-card {
  border-radius: var(--radius-md);
  margin-bottom: 14px;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-text {
  line-height: 1.9;
  color: var(--text-primary);
  font-size: 16px;
}

.step-image {
  width: 100%;
  max-height: 280px;
  border-radius: var(--radius-md);
}

.action-row {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 440px;
  overflow-y: auto;
}

.timeline-item {
  width: 100%;
  display: flex;
  gap: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
  border-radius: 10px;
  padding: 10px;
  cursor: pointer;
  text-align: left;
  transition: var(--transition);
}

.timeline-item:hover {
  border-color: var(--primary-color);
}

.timeline-item.active {
  border-color: var(--primary-color);
  background: #f3f9ff;
}

.timeline-item.done .num {
  background: #67c23a;
}

.num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-color);
  color: #fff;
  flex-shrink: 0;
}

.txt {
  color: var(--text-primary);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-line {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border-color);
}

.meta-line:last-of-type {
  border-bottom: none;
}

.back-btn {
  width: 100%;
  margin-top: 14px;
}

@media (max-width: 768px) {
  .header-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .title {
    font-size: 24px;
  }

  .action-row .el-button {
    flex: 1;
    min-width: 120px;
  }
}
</style>
