<template>
  <div class="scene-top-nav" role="tablist" aria-label="场景模式">
    <button
      v-for="mode in modeOptions"
      :key="mode.code"
      class="scene-mode-tab"
      :class="{ active: currentMode === mode.code }"
      type="button"
      role="tab"
      :aria-selected="currentMode === mode.code"
      @click="handleSelect(mode.code)"
    >
      <span class="mode-emoji">{{ mode.emoji }}</span>
      <span class="mode-label">{{ mode.label }}</span>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useSceneModeStore } from '@/stores/sceneMode'
import { trackBehavior } from '@/utils/tracker'

const sceneModeStore = useSceneModeStore()
const { currentMode } = storeToRefs(sceneModeStore)
const modeOptions = computed(() => sceneModeStore.modeOptions)

const handleSelect = (mode) => {
  if (!mode || mode === currentMode.value) return
  sceneModeStore.setMode(mode)
  trackBehavior('scene_mode_switch', {
    sourcePage: 'header',
    extra: { mode }
  })
}
</script>

<style scoped>
.scene-top-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
  transition: all var(--transition);
}

.scene-mode-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: var(--text-regular);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.28s ease;
}

.scene-mode-tab:hover {
  background: rgba(15, 23, 42, 0.06);
  color: var(--text-primary);
  transform: translateY(-1px);
}

.scene-mode-tab.active {
  border-color: transparent;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: #fff;
  transform: scale(1.04);
  box-shadow: var(--mode-card-glow, var(--shadow-sm));
}

.mode-emoji {
  line-height: 1;
  font-size: 15px;
}

.mode-label {
  white-space: nowrap;
}
</style>
