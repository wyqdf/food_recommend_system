import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const STORAGE_KEY = 'scene_mode_preference'
const DEFAULT_MODE = 'family'

const MODE_CONFIGS = [
  {
    code: 'family',
    label: '家庭模式',
    title: 'Family Mode',
    description: '暖橙氛围 | 营养均衡',
    emoji: '🏡',
    gradient: 'linear-gradient(135deg, #ffa974 0%, #ffcf98 60%, #ffe7c7 100%)',
    theme: {
      primaryColor: '#e85d2a',
      primaryLight: '#f17b4f',
      primaryDark: '#cb4b1e',
      secondaryColor: '#f58a2c',
      accentColor: '#ffb347',
      bgGradient: 'linear-gradient(180deg, #fff6ef 0%, #ffe9dd 60%, #fefcf9 100%)',
      heroGradient: 'linear-gradient(135deg, #ff8c5a 0%, #ffb284 50%, #ffd7ba 100%)',
      highlightBg: 'rgba(255, 221, 197, 0.5)',
      cardGlow: '0 12px 30px rgba(232, 93, 42, 0.35)'
    }
  },
  {
    code: 'fitness',
    label: '减脂健身',
    title: 'Fitness Mode',
    description: '清爽绿色 | 高蛋白低脂',
    emoji: '💪',
    gradient: 'linear-gradient(135deg, #96fbc4 0%, #45d1a6 50%, #17b897 100%)',
    theme: {
      primaryColor: '#0ea271',
      primaryLight: '#34d399',
      primaryDark: '#0b7d56',
      secondaryColor: '#14b487',
      accentColor: '#7ae6c6',
      bgGradient: 'linear-gradient(180deg, #f0fff5 0%, #dafbef 55%, #f7fffb 100%)',
      heroGradient: 'linear-gradient(135deg, #56e5b3 0%, #2bc59d 60%, #099a7c 100%)',
      highlightBg: 'rgba(149, 243, 196, 0.35)',
      cardGlow: '0 12px 30px rgba(16, 163, 125, 0.3)'
    }
  },
  {
    code: 'quick',
    label: '打工人速食',
    title: 'Quick Meal Mode',
    description: '霓虹紫调 | 20 分钟快手',
    emoji: '⚡',
    gradient: 'linear-gradient(135deg, #c084fc 0%, #a855f7 55%, #7c3aed 100%)',
    theme: {
      primaryColor: '#8b5cf6',
      primaryLight: '#a78bfa',
      primaryDark: '#6d28d9',
      secondaryColor: '#f472b6',
      accentColor: '#f0abfc',
      bgGradient: 'linear-gradient(180deg, #f7f3ff 0%, #f2ecff 55%, #fbf8ff 100%)',
      heroGradient: 'linear-gradient(135deg, #b794f4 0%, #a855f7 50%, #7c3aed 100%)',
      highlightBg: 'rgba(222, 205, 255, 0.45)',
      cardGlow: '0 14px 34px rgba(124, 58, 237, 0.35)'
    }
  },
  {
    code: 'party',
    label: '周末聚会',
    title: 'Party Mode',
    description: '莓果金调 | 多人分享',
    emoji: '🎉',
    gradient: 'linear-gradient(135deg, #fda5d7 0%, #fd6585 55%, #f36363 100%)',
    theme: {
      primaryColor: '#ee2c7b',
      primaryLight: '#f871a0',
      primaryDark: '#c51961',
      secondaryColor: '#f97393',
      accentColor: '#ffd166',
      bgGradient: 'linear-gradient(180deg, #fff0f8 0%, #ffe0ee 60%, #fff8fb 100%)',
      heroGradient: 'linear-gradient(135deg, #ff8fb3 0%, #ff5f7e 50%, #ffaf7b 100%)',
      highlightBg: 'rgba(255, 176, 208, 0.45)',
      cardGlow: '0 14px 36px rgba(238, 44, 123, 0.32)'
    }
  }
]

const MODE_MAP = MODE_CONFIGS.reduce((acc, item) => {
  acc[item.code] = item
  return acc
}, {})

const CSS_VAR_MAP = {
  primaryColor: '--primary-color',
  primaryLight: '--primary-light',
  primaryDark: '--primary-dark',
  secondaryColor: '--secondary-color',
  accentColor: '--accent-color',
  bgGradient: '--bg-gradient',
  heroGradient: '--hero-gradient',
  highlightBg: '--mode-highlight-bg',
  cardGlow: '--mode-card-glow'
}

const applyTheme = (modeConfig) => {
  if (typeof document === 'undefined' || !modeConfig) return
  const root = document.documentElement
  Object.entries(modeConfig.theme || {}).forEach(([key, value]) => {
    const cssVar = CSS_VAR_MAP[key]
    if (cssVar) {
      root.style.setProperty(cssVar, value)
    }
  })
  document.body?.setAttribute('data-scene-mode', modeConfig.code)
}

export const useSceneModeStore = defineStore('sceneMode', () => {
  const storedValue = localStorage.getItem(STORAGE_KEY)
  const initialMode = MODE_MAP[storedValue] ? storedValue : DEFAULT_MODE
  const currentMode = ref(initialMode)

  const currentModeConfig = computed(() => MODE_MAP[currentMode.value] || MODE_MAP[DEFAULT_MODE])

  const setMode = (mode) => {
    if (!MODE_MAP[mode] || mode === currentMode.value) return
    currentMode.value = mode
    localStorage.setItem(STORAGE_KEY, mode)
    applyTheme(MODE_MAP[mode])
  }

  const initializeTheme = () => {
    if (!MODE_MAP[currentMode.value]) {
      currentMode.value = DEFAULT_MODE
    }
    applyTheme(MODE_MAP[currentMode.value])
  }

  return {
    modeOptions: MODE_CONFIGS,
    currentMode,
    currentModeConfig,
    setMode,
    initializeTheme
  }
})

