<template>
  <el-dialog
    :model-value="modelValue"
    title="选择你的兴趣标签"
    width="760px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @update:model-value="handleVisibilityChange"
  >
    <div class="tag-onboarding">
      <div class="tag-onboarding__hero">
        <div>
          <h3>选择你感兴趣的饮食标签</h3>
          <p>选择你感兴趣的标签，我们会根据你的偏好为你推荐更适合的内容。</p>
        </div>
        <el-tag type="warning" effect="light" round>
          首次登录推荐填写
        </el-tag>
      </div>

      <div class="tag-onboarding__summary">
        <span>已选 {{ selectedCount }} 个标签</span>
        <span>标签可随时在个人菜单中重新编辑</span>
      </div>

      <section class="tag-section">
        <div class="tag-section__header">
          <h4>饮食目标</h4>
          <span>单选</span>
        </div>
        <div class="tag-grid tag-grid--single">
          <button
            v-for="item in goalOptions"
            :key="item.value"
            type="button"
            class="interest-tag"
            :class="{ 'is-active': form.dietGoal === item.value }"
            @click="form.dietGoal = item.value"
          >
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </button>
        </div>
      </section>

      <section class="tag-section">
        <div class="tag-section__header">
          <h4>做饭节奏</h4>
          <span>单选</span>
        </div>
        <div class="tag-grid tag-grid--single">
          <button
            v-for="item in timeBudgetOptions"
            :key="item.value"
            type="button"
            class="interest-tag"
            :class="{ 'is-active': form.timeBudget === item.value }"
            @click="form.timeBudget = item.value"
          >
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </button>
        </div>
      </section>

      <section class="tag-section">
        <div class="tag-section__header">
          <h4>烹饪熟练度</h4>
          <span>单选</span>
        </div>
        <div class="tag-grid tag-grid--single">
          <button
            v-for="item in skillOptions"
            :key="item.value"
            type="button"
            class="interest-tag"
            :class="{ 'is-active': form.cookingSkill === item.value }"
            @click="form.cookingSkill = item.value"
          >
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </button>
        </div>
      </section>

      <section class="tag-section">
        <div class="tag-section__header">
          <h4>喜欢的口味</h4>
          <span>多选</span>
        </div>
        <div class="chip-list">
          <button
            v-for="item in tasteOptions"
            :key="item"
            type="button"
            class="chip"
            :class="{ 'is-selected': form.preferredTastes.includes(item) }"
            @click="toggleMulti(form.preferredTastes, item)"
          >
            {{ item }}
          </button>
        </div>
      </section>

      <section class="tag-section">
        <div class="tag-section__header">
          <h4>常做食材</h4>
          <span>多选</span>
        </div>
        <div class="chip-list">
          <button
            v-for="item in ingredientOptions"
            :key="item"
            type="button"
            class="chip chip--soft"
            :class="{ 'is-selected': form.favoriteIngredients.includes(item) }"
            @click="toggleMulti(form.favoriteIngredients, item)"
          >
            {{ item }}
          </button>
        </div>
      </section>

      <section class="tag-section">
        <div class="tag-section__header">
          <h4>需要避开的食材</h4>
          <span>多选</span>
        </div>
        <div class="chip-list">
          <button
            v-for="item in tabooOptions"
            :key="item"
            type="button"
            class="chip chip--danger"
            :class="{ 'is-selected': form.tabooIngredients.includes(item) }"
            @click="toggleMulti(form.tabooIngredients, item)"
          >
            {{ item }}
          </button>
        </div>
      </section>

      <section class="tag-section">
        <div class="tag-section__header">
          <h4>常见就餐场景</h4>
          <span>多选</span>
        </div>
        <div class="chip-list">
          <button
            v-for="item in mealMomentOptions"
            :key="item"
            type="button"
            class="chip chip--accent"
            :class="{ 'is-selected': form.mealMoments.includes(item) }"
            @click="toggleMulti(form.mealMoments, item)"
          >
            {{ item }}
          </button>
        </div>
        <div class="chip-list chip-list--scene-catalog">
          <button
            v-for="scene in sceneOptions"
            :key="scene.code"
            type="button"
            class="chip chip--outlined"
            :class="{ 'is-selected': form.preferredScenes.includes(scene.name) }"
            @click="toggleMulti(form.preferredScenes, scene.name)"
          >
            {{ scene.name }}
          </button>
        </div>
      </section>

      <section class="tag-section">
        <div class="tag-section__header">
          <h4>常用厨具</h4>
          <span>多选</span>
        </div>
        <div class="chip-list">
          <button
            v-for="item in cookwareOptions"
            :key="item"
            type="button"
            class="chip"
            :class="{ 'is-selected': form.availableCookwares.includes(item) }"
            @click="toggleMulti(form.availableCookwares, item)"
          >
            {{ item }}
          </button>
        </div>
      </section>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="skipOnce" :disabled="submitting">稍后填写</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存兴趣标签</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi, sceneApi } from '@/api'
import { trackBehavior } from '@/utils/tracker'

const SCENE_META_PREFIX = '场景:'
const INGREDIENT_META_PREFIX = '食材:'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'completed', 'skipped'])

const submitting = ref(false)
const sceneOptions = ref([])

const goalOptions = [
  { value: '均衡', label: '均衡饮食', description: '想吃得舒服，日常家常为主' },
  { value: '减脂', label: '轻负担', description: '更关注清爽、控油和低卡' },
  { value: '增肌', label: '高蛋白', description: '偏向增肌和营养补充' },
  { value: '控糖', label: '控糖管理', description: '尽量减少高糖高负担选择' },
  { value: '随意', label: '都可以', description: '先随便看看，之后再慢慢调' }
]

const timeBudgetOptions = [
  { value: 'quick', label: '快手 20 分钟', description: '适合工作日和早餐时段' },
  { value: 'medium', label: '30-60 分钟', description: '适合认真做一顿家常饭' },
  { value: 'long', label: '慢工细作', description: '周末炖煮、烘焙都可以' }
]

const skillOptions = [
  { value: '新手', label: '新手友好', description: '步骤简单，成功率优先' },
  { value: '进阶', label: '想学新菜', description: '愿意尝试更丰富的做法' },
  { value: '熟练', label: '已经很会做', description: '可以接受复杂步骤和硬菜' }
]

const tasteOptions = ['咸鲜', '甜味', '原味', '微辣', '清淡', '酸甜', '奶香', '咸香', '酸辣', '麻辣', '酱香', '蒜香']
const ingredientOptions = ['鸡蛋', '牛奶', '胡萝卜', '土豆', '洋葱', '黄油', '面粉', '番茄', '鸡肉', '牛肉', '虾仁', '蘑菇']
const tabooOptions = ['花生', '海鲜', '牛奶', '鸡蛋', '麸质', '辛辣', '香菜', '葱姜蒜']
const cookwareOptions = ['炒锅', '蒸锅', '砂锅', '烤箱', '微波炉', '电饭煲', '平底锅', '高压锅']
const mealMomentOptions = ['早餐', '午餐', '晚餐', '下午茶', '夜宵', '朋友聚餐', '家常菜', '宴客菜', '儿童餐', '老人餐']

const sceneNameSet = computed(() => new Set(sceneOptions.value.map((item) => item.name)))

const form = reactive({
  dietGoal: '均衡',
  cookingSkill: '新手',
  timeBudget: 'quick',
  preferredTastes: [],
  favoriteIngredients: [],
  tabooIngredients: [],
  availableCookwares: [],
  preferredScenes: [],
  mealMoments: []
})

const selectedCount = computed(() => {
  return [
    form.dietGoal,
    form.timeBudget,
    form.cookingSkill,
    ...form.preferredTastes,
    ...form.favoriteIngredients,
    ...form.tabooIngredients,
    ...form.availableCookwares,
    ...form.preferredScenes,
    ...form.mealMoments
  ].filter(Boolean).length
})

const uniqueList = (items) => Array.from(new Set(items.filter(Boolean)))

const normalizePreferredScenePayload = () => {
  return uniqueList([
    ...form.preferredScenes,
    ...form.mealMoments.map((item) => `${SCENE_META_PREFIX}${item}`),
    ...form.favoriteIngredients.map((item) => `${INGREDIENT_META_PREFIX}${item}`)
  ])
}

const resetForm = () => {
  form.dietGoal = '均衡'
  form.cookingSkill = '新手'
  form.timeBudget = 'quick'
  form.preferredTastes = []
  form.favoriteIngredients = []
  form.tabooIngredients = []
  form.availableCookwares = []
  form.preferredScenes = []
  form.mealMoments = []
}

const splitPreferredScenes = (items) => {
  const canonicalScenes = []
  const mealMoments = []
  const favoriteIngredients = []

  for (const item of Array.isArray(items) ? items : []) {
    if (!item) continue
    if (item.startsWith(SCENE_META_PREFIX)) {
      mealMoments.push(item.slice(SCENE_META_PREFIX.length))
      continue
    }
    if (item.startsWith(INGREDIENT_META_PREFIX)) {
      favoriteIngredients.push(item.slice(INGREDIENT_META_PREFIX.length))
      continue
    }
    if (sceneNameSet.value.has(item)) {
      canonicalScenes.push(item)
      continue
    }
    if (mealMomentOptions.includes(item)) {
      mealMoments.push(item)
    }
  }

  return {
    canonicalScenes: uniqueList(canonicalScenes),
    mealMoments: uniqueList(mealMoments),
    favoriteIngredients: uniqueList(favoriteIngredients)
  }
}

const loadScenes = async () => {
  try {
    const res = await sceneApi.getList()
    sceneOptions.value = res.data || []
  } catch (error) {
    sceneOptions.value = []
  }
}

const loadProfile = async () => {
  try {
    const res = await userApi.getOnboarding()
    const data = res.data || {}
    const parsedScenes = splitPreferredScenes(data.preferredScenes)
    form.dietGoal = data.dietGoal || '均衡'
    form.cookingSkill = data.cookingSkill || '新手'
    form.timeBudget = data.timeBudget || 'quick'
    form.preferredTastes = Array.isArray(data.preferredTastes) ? uniqueList(data.preferredTastes) : []
    form.tabooIngredients = Array.isArray(data.tabooIngredients) ? uniqueList(data.tabooIngredients) : []
    form.availableCookwares = Array.isArray(data.availableCookwares) ? uniqueList(data.availableCookwares) : []
    form.preferredScenes = parsedScenes.canonicalScenes
    form.mealMoments = parsedScenes.mealMoments
    form.favoriteIngredients = parsedScenes.favoriteIngredients
  } catch (error) {
    resetForm()
  }
}

watch(() => props.modelValue, async (visible) => {
  if (!visible) return
  await loadScenes()
  await loadProfile()
})

const handleVisibilityChange = (visible) => {
  emit('update:modelValue', visible)
}

const toggleMulti = (list, value) => {
  const index = list.indexOf(value)
  if (index >= 0) {
    list.splice(index, 1)
  } else {
    list.push(value)
  }
}

const skipOnce = () => {
  trackBehavior('questionnaire_skip', {
    sourcePage: 'onboarding_dialog',
    extra: {
      mode: 'interest-tags'
    }
  })
  emit('skipped')
  emit('update:modelValue', false)
}

const submit = async () => {
  submitting.value = true
  try {
    await userApi.updateOnboarding({
      dietGoal: form.dietGoal,
      cookingSkill: form.cookingSkill,
      timeBudget: form.timeBudget,
      preferredTastes: uniqueList(form.preferredTastes),
      tabooIngredients: uniqueList(form.tabooIngredients),
      availableCookwares: uniqueList(form.availableCookwares),
      preferredScenes: normalizePreferredScenePayload()
    })
    trackBehavior('questionnaire_submit', {
      sourcePage: 'onboarding_dialog',
      extra: {
        completed: true,
        mode: 'interest-tags',
        tagCount: selectedCount.value
      }
    })
    ElMessage.success('兴趣标签已保存，推荐会按你的偏好调整')
    emit('completed')
    emit('update:modelValue', false)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.tag-onboarding {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.tag-onboarding__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.08), rgba(255, 159, 67, 0.12));
}

.tag-onboarding__hero h3 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--text-primary);
}

.tag-onboarding__hero p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.tag-onboarding__summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text-secondary);
}

.tag-section {
  padding: 16px 18px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: #fff;
}

.tag-section__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
  margin-bottom: 14px;
}

.tag-section__header h4 {
  margin: 0;
  font-size: 16px;
  color: var(--text-primary);
}

.tag-section__header span {
  font-size: 12px;
  color: var(--text-secondary);
}

.tag-grid {
  display: grid;
  gap: 12px;
}

.tag-grid--single {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.interest-tag {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  width: 100%;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-primary);
  cursor: pointer;
  transition: var(--transition);
  text-align: left;
}

.interest-tag strong {
  font-size: 14px;
}

.interest-tag small {
  color: var(--text-secondary);
  line-height: 1.5;
}

.interest-tag:hover,
.interest-tag.is-active {
  border-color: rgba(255, 107, 107, 0.4);
  background: rgba(255, 107, 107, 0.08);
  box-shadow: 0 8px 18px rgba(255, 107, 107, 0.08);
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-primary);
  border-radius: 999px;
  padding: 9px 16px;
  cursor: pointer;
  transition: var(--transition);
  font-size: 14px;
}

.chip:hover,
.chip.is-selected {
  border-color: rgba(255, 107, 107, 0.5);
  background: rgba(255, 107, 107, 0.08);
  color: var(--primary-color);
}

.chip--soft:hover,
.chip--soft.is-selected {
  border-color: rgba(15, 155, 142, 0.45);
  background: rgba(15, 155, 142, 0.1);
  color: #0f7d73;
}

.chip--danger:hover,
.chip--danger.is-selected {
  border-color: rgba(231, 76, 60, 0.45);
  background: rgba(231, 76, 60, 0.08);
  color: #d94841;
}

.chip--accent:hover,
.chip--accent.is-selected,
.chip--outlined:hover,
.chip--outlined.is-selected {
  border-color: rgba(255, 159, 67, 0.45);
  background: rgba(255, 159, 67, 0.1);
  color: #c96b13;
}

.chip-list--scene-catalog {
  margin-top: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 768px) {
  .tag-onboarding__hero {
    flex-direction: column;
  }

  .tag-section {
    padding: 14px;
  }

  .tag-section__header {
    flex-direction: column;
    margin-bottom: 12px;
  }
}
</style>
