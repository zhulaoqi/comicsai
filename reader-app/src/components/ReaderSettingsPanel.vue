<template>
  <Teleport to="body">
    <Transition name="settings-panel">
      <div v-if="visible" class="settings-overlay" @click.self="$emit('close')">
        <div class="settings-panel" :style="panelStyle">
          <!-- Brightness -->
          <div class="settings-section">
            <div class="settings-row settings-row--brightness">
              <svg class="settings-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
              </svg>
              <input
                type="range"
                class="settings-slider"
                :min="BRIGHTNESS_MIN"
                :max="BRIGHTNESS_MAX"
                :value="settings.brightness"
                @input="setBrightness(Number(($event.target as HTMLInputElement).value))"
              />
              <svg class="settings-icon settings-icon--lg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
              </svg>
            </div>
          </div>

          <!-- Font size -->
          <div class="settings-section">
            <div class="settings-row settings-row--fontsize">
              <button class="settings-btn settings-btn--font" :disabled="settings.fontSize <= FONT_SIZE_MIN" @click="decreaseFontSize">
                A<span class="settings-btn__minus">-</span>
              </button>
              <div class="settings-fontsize-bar">
                <div class="settings-fontsize-track">
                  <div class="settings-fontsize-fill" :style="{ width: fontSizePercent + '%' }" />
                </div>
                <span class="settings-fontsize-label">{{ settings.fontSize }}</span>
              </div>
              <button class="settings-btn settings-btn--font settings-btn--font-lg" :disabled="settings.fontSize >= FONT_SIZE_MAX" @click="increaseFontSize">
                A<span class="settings-btn__plus">+</span>
              </button>
            </div>
          </div>

          <!-- Theme -->
          <div class="settings-section">
            <div class="settings-row settings-row--theme">
              <button
                v-for="t in THEME_OPTIONS"
                :key="t.key"
                class="settings-theme-btn"
                :class="{ 'settings-theme-btn--active': settings.theme === t.key }"
                :style="{ background: t.color, color: t.key === 'dark' ? '#ccc' : '#333' }"
                @click="setTheme(t.key)"
              >
                {{ t.label }}
              </button>
            </div>
          </div>

          <!-- Line height -->
          <div class="settings-section">
            <span class="settings-label">行距</span>
            <div class="settings-row settings-row--lineheight">
              <button
                v-for="lh in LINE_HEIGHT_OPTIONS"
                :key="lh.value"
                class="settings-chip"
                :class="{ 'settings-chip--active': settings.lineHeight === lh.value }"
                @click="setLineHeight(lh.value)"
              >
                {{ lh.label }}
              </button>
            </div>
          </div>

          <!-- Font family -->
          <div class="settings-section">
            <span class="settings-label">字体</span>
            <div class="settings-row settings-row--font-family">
              <button
                v-for="f in FONT_OPTIONS"
                :key="f.key"
                class="settings-chip"
                :class="{ 'settings-chip--active': settings.fontFamily === f.key }"
                @click="setFontFamily(f.key)"
              >
                {{ f.label }}
              </button>
            </div>
          </div>

          <!-- Auto scroll -->
          <div class="settings-section">
            <div class="settings-row settings-row--auto-scroll">
              <span class="settings-label" style="margin-bottom: 0">自动滚动</span>
              <div class="settings-toggle-group">
                <button
                  class="settings-toggle"
                  :class="{ 'settings-toggle--on': autoScroll }"
                  @click="$emit('toggle-auto-scroll')"
                >
                  <span class="settings-toggle__track">
                    <span class="settings-toggle__thumb" />
                  </span>
                </button>
                <template v-if="autoScroll">
                  <button class="settings-btn settings-btn--small" @click="$emit('scroll-speed', -1)">慢</button>
                  <div class="settings-speed-bar">
                    <div class="settings-speed-fill" :style="{ width: scrollSpeedPercent + '%' }" />
                  </div>
                  <button class="settings-btn settings-btn--small" @click="$emit('scroll-speed', 1)">快</button>
                </template>
              </div>
            </div>
          </div>

          <!-- Reset -->
          <div class="settings-section settings-section--reset">
            <button class="settings-reset-btn" @click="resetSettings">恢复默认设置</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  useReaderSettings,
  THEME_OPTIONS,
  FONT_OPTIONS,
  LINE_HEIGHT_OPTIONS,
  FONT_SIZE_MIN,
  FONT_SIZE_MAX,
  BRIGHTNESS_MIN,
  BRIGHTNESS_MAX,
} from '../composables/useReaderSettings'

const props = defineProps<{
  visible: boolean
  autoScroll: boolean
  scrollSpeed: number
}>()

defineEmits<{
  close: []
  'toggle-auto-scroll': []
  'scroll-speed': [delta: number]
}>()

const {
  settings,
  increaseFontSize,
  decreaseFontSize,
  setTheme,
  setLineHeight,
  setFontFamily,
  setBrightness,
  resetSettings,
  currentThemeStyle,
} = useReaderSettings()

const fontSizePercent = computed(() =>
  ((settings.fontSize - FONT_SIZE_MIN) / (FONT_SIZE_MAX - FONT_SIZE_MIN)) * 100,
)

const scrollSpeedPercent = computed(() => {
  const min = 1, max = 5
  return (((props.scrollSpeed ?? 2) - min) / (max - min)) * 100
})

const panelStyle = computed(() => ({
  background: currentThemeStyle.value.card,
  color: currentThemeStyle.value.text,
  borderColor: currentThemeStyle.value.border,
}))
</script>

<style scoped>
.settings-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: flex-end;
}

.settings-panel {
  width: 100%;
  border-top: 1px solid;
  border-radius: 16px 16px 0 0;
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.12);
  max-height: 70vh;
  overflow-y: auto;
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.settings-section--reset {
  align-items: center;
  padding-top: 4px;
}

.settings-label {
  font-size: 13px;
  font-weight: 500;
  opacity: 0.6;
  margin-bottom: 2px;
}

/* Brightness */
.settings-row--brightness {
  display: flex;
  align-items: center;
  gap: 12px;
}

.settings-icon {
  flex-shrink: 0;
  opacity: 0.5;
}

.settings-icon--lg {
  opacity: 0.8;
}

.settings-slider {
  flex: 1;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: currentColor;
  opacity: 0.2;
  border-radius: 2px;
  outline: none;
}

.settings-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #4a6cf7;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.settings-slider::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #4a6cf7;
  cursor: pointer;
  border: none;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

/* Font size */
.settings-row--fontsize {
  display: flex;
  align-items: center;
  gap: 12px;
}

.settings-btn--font {
  display: flex;
  align-items: baseline;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  border: 1px solid currentColor;
  opacity: 0.6;
  transition: opacity 0.15s, background 0.15s;
  flex-shrink: 0;
}

.settings-btn--font-lg {
  font-size: 18px;
}

.settings-btn--font:not(:disabled):hover {
  opacity: 1;
  background: rgba(74, 108, 247, 0.1);
}

.settings-btn--font:disabled {
  opacity: 0.25;
  cursor: not-allowed;
}

.settings-btn__minus,
.settings-btn__plus {
  font-size: 10px;
  margin-left: 1px;
}

.settings-fontsize-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.settings-fontsize-track {
  flex: 1;
  height: 4px;
  background: currentColor;
  opacity: 0.15;
  border-radius: 2px;
  overflow: hidden;
}

.settings-fontsize-fill {
  height: 100%;
  background: #4a6cf7;
  border-radius: 2px;
  transition: width 0.2s;
}

.settings-fontsize-label {
  font-size: 13px;
  min-width: 24px;
  text-align: center;
  opacity: 0.6;
}

/* Theme buttons */
.settings-row--theme {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.settings-theme-btn {
  width: 56px;
  height: 36px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  border: 2px solid transparent;
  transition: border-color 0.2s, transform 0.15s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.settings-theme-btn:hover {
  transform: scale(1.05);
}

.settings-theme-btn--active {
  border-color: #4a6cf7;
  box-shadow: 0 0 0 2px rgba(74, 108, 247, 0.25);
}

/* Chips (line-height & font family) */
.settings-row--lineheight,
.settings-row--font-family {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.settings-chip {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid currentColor;
  opacity: 0.5;
  transition: all 0.15s;
}

.settings-chip:hover {
  opacity: 0.8;
}

.settings-chip--active {
  background: #4a6cf7;
  color: #fff !important;
  border-color: #4a6cf7;
  opacity: 1;
}

/* Auto-scroll */
.settings-row--auto-scroll {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.settings-toggle-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.settings-toggle {
  display: flex;
  align-items: center;
}

.settings-toggle__track {
  display: block;
  width: 44px;
  height: 24px;
  border-radius: 12px;
  background: currentColor;
  opacity: 0.2;
  position: relative;
  transition: background 0.2s, opacity 0.2s;
}

.settings-toggle--on .settings-toggle__track {
  background: #4a6cf7;
  opacity: 1;
}

.settings-toggle__thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s;
}

.settings-toggle--on .settings-toggle__thumb {
  transform: translateX(20px);
}

.settings-btn--small {
  font-size: 12px;
  opacity: 0.6;
  padding: 2px 6px;
  border-radius: 4px;
}

.settings-btn--small:hover {
  opacity: 1;
}

.settings-speed-bar {
  width: 60px;
  height: 3px;
  background: currentColor;
  opacity: 0.15;
  border-radius: 2px;
  overflow: hidden;
}

.settings-speed-fill {
  height: 100%;
  background: #4a6cf7;
  border-radius: 2px;
  transition: width 0.2s;
}

/* Reset button */
.settings-reset-btn {
  font-size: 13px;
  color: #4a6cf7;
  opacity: 0.7;
  transition: opacity 0.15s;
  padding: 6px 16px;
  border-radius: 20px;
  border: 1px solid #4a6cf7;
}

.settings-reset-btn:hover {
  opacity: 1;
}

/* Transitions */
.settings-panel-enter-active,
.settings-panel-leave-active {
  transition: opacity 0.25s ease;
}

.settings-panel-enter-active .settings-panel,
.settings-panel-leave-active .settings-panel {
  transition: transform 0.3s cubic-bezier(0.33, 1, 0.68, 1);
}

.settings-panel-enter-from,
.settings-panel-leave-to {
  opacity: 0;
}

.settings-panel-enter-from .settings-panel {
  transform: translateY(100%);
}

.settings-panel-leave-to .settings-panel {
  transform: translateY(100%);
}

/* Desktop wider */
@media (min-width: 769px) {
  .settings-panel {
    max-width: 480px;
    margin: 0 auto;
    border-radius: 16px 16px 0 0;
  }

  .settings-overlay {
    justify-content: center;
  }
}
</style>
