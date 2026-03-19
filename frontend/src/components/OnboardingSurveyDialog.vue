<template>
  <el-dialog
    :model-value="modelValue"
    title="完善偏好问卷（7 题）"
    width="640px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @update:model-value="handleVisibilityChange"
  >
    <el-form label-position="top" :model="form">
      <el-form-item label="1. 你的饮食目标">
        <el-select v-model="form.dietGoal" placeholder="请选择饮食目标" style="width: 100%">
          <el-option label="均衡饮食" value="均衡" />
          <el-option label="减脂" value="减脂" />
          <el-option label="增肌" value="增肌" />
          <el-option label="控糖" value="控糖" />
          <el-option label="随意" value="随意" />
        </el-select>
      </el-form-item>

      <el-form-item label="2. 你的烹饪水平">
        <el-radio-group v-model="form.cookingSkill">
          <el-radio value="新手">新手</el-radio>
          <el-radio value="进阶">进阶</el-radio>
          <el-radio value="熟练">熟练</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="3. 单次可投入时长">
        <el-radio-group v-model="form.timeBudget">
          <el-radio value="quick">20 分钟内</el-radio>
          <el-radio value="medium">30-60 分钟</el-radio>
          <el-radio value="long">60 分钟以上</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="4. 口味偏好（可多选）">
        <el-select v-model="form.preferredTastes" multiple filterable allow-create default-first-option style="width: 100%">
          <el-option v-for="item in tasteOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <el-form-item label="5. 忌口/过敏（可多选）">
        <el-select v-model="form.tabooIngredients" multiple filterable allow-create default-first-option style="width: 100%">
          <el-option v-for="item in tabooOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <el-form-item label="6. 常用厨具（可多选）">
        <el-select v-model="form.availableCookwares" multiple filterable allow-create default-first-option style="width: 100%">
          <el-option v-for="item in cookwareOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <el-form-item label="7. 常见就餐场景（可多选）">
        <el-select v-model="form.preferredScenes" multiple style="width: 100%">
          <el-option v-for="scene in sceneOptions" :key="scene.code" :label="scene.name" :value="scene.name" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="skipOnce" :disabled="submitting">稍后填写</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存并应用推荐</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi, sceneApi } from '@/api'
import { trackBehavior } from '@/utils/tracker'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'completed'])

const submitting = ref(false)
const sceneOptions = ref([])

const tasteOptions = ['咸鲜', '香辣', '酸甜', '清淡', '麻辣', '蒜香']
const tabooOptions = ['花生', '海鲜', '牛奶', '鸡蛋', '麸质', '辛辣']
const cookwareOptions = ['炒锅', '蒸锅', '砂锅', '烤箱', '微波炉', '电饭煲', '平底锅', '空气炸锅']

const form = reactive({
  dietGoal: '均衡',
  cookingSkill: '新手',
  timeBudget: 'quick',
  preferredTastes: [],
  tabooIngredients: [],
  availableCookwares: [],
  preferredScenes: []
})

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
    form.dietGoal = data.dietGoal || '均衡'
    form.cookingSkill = data.cookingSkill || '新手'
    form.timeBudget = data.timeBudget || 'quick'
    form.preferredTastes = Array.isArray(data.preferredTastes) ? data.preferredTastes : []
    form.tabooIngredients = Array.isArray(data.tabooIngredients) ? data.tabooIngredients : []
    form.availableCookwares = Array.isArray(data.availableCookwares) ? data.availableCookwares : []
    form.preferredScenes = Array.isArray(data.preferredScenes) ? data.preferredScenes : []
  } catch (error) {
    // ignore
  }
}

watch(() => props.modelValue, async (visible) => {
  if (!visible) return
  await Promise.all([loadScenes(), loadProfile()])
})

const handleVisibilityChange = (visible) => {
  emit('update:modelValue', visible)
}

const skipOnce = () => {
  emit('update:modelValue', false)
}

const submit = async () => {
  submitting.value = true
  try {
    await userApi.updateOnboarding({
      dietGoal: form.dietGoal,
      cookingSkill: form.cookingSkill,
      timeBudget: form.timeBudget,
      preferredTastes: form.preferredTastes,
      tabooIngredients: form.tabooIngredients,
      availableCookwares: form.availableCookwares,
      preferredScenes: form.preferredScenes
    })
    trackBehavior('questionnaire_submit', { sourcePage: 'onboarding_dialog', extra: { completed: true } })
    ElMessage.success('偏好已保存，推荐将自动更新')
    emit('completed')
    emit('update:modelValue', false)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
