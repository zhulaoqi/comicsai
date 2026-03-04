import { reactive, watch, computed } from 'vue'

export type ReaderTheme = 'default' | 'dark' | 'eye' | 'sepia'
export type ReaderFont = 'system' | 'serif' | 'songti' | 'kaiti' | 'heiti'

export interface ReaderSettings {
  theme: ReaderTheme
  fontSize: number
  lineHeight: number
  fontFamily: ReaderFont
  brightness: number
}

const STORAGE_KEY = 'reader-settings'

const DEFAULT_SETTINGS: ReaderSettings = {
  theme: 'default',
  fontSize: 18,
  lineHeight: 1.9,
  fontFamily: 'system',
  brightness: 100,
}

export const FONT_SIZE_MIN = 14
export const FONT_SIZE_MAX = 28
export const FONT_SIZE_STEP = 2
export const BRIGHTNESS_MIN = 40
export const BRIGHTNESS_MAX = 100

export const THEME_OPTIONS: { key: ReaderTheme; label: string; color: string }[] = [
  { key: 'default', label: '默认', color: '#ffffff' },
  { key: 'eye', label: '护眼', color: '#C7EDCC' },
  { key: 'sepia', label: '纸纹', color: '#F5E6C8' },
  { key: 'dark', label: '夜间', color: '#1a1a2e' },
]

export const FONT_OPTIONS: { key: ReaderFont; label: string; family: string }[] = [
  { key: 'system', label: '系统', family: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif' },
  { key: 'serif', label: '衬线', family: 'Georgia, "Times New Roman", serif' },
  { key: 'songti', label: '宋体', family: '"Songti SC", "SimSun", "宋体", serif' },
  { key: 'kaiti', label: '楷体', family: '"Kaiti SC", "KaiTi", "楷体", serif' },
  { key: 'heiti', label: '黑体', family: '"Heiti SC", "SimHei", "黑体", sans-serif' },
]

export const LINE_HEIGHT_OPTIONS = [
  { value: 1.5, label: '紧凑' },
  { value: 1.75, label: '适中' },
  { value: 2.0, label: '宽松' },
  { value: 2.4, label: '超大' },
]

export const THEME_STYLES: Record<ReaderTheme, {
  bg: string; card: string; text: string; textSecondary: string
  border: string; headerBg: string
}> = {
  default: {
    bg: '#fafbfe', card: '#ffffff', text: '#1a1d26',
    textSecondary: '#6b7280', border: '#e8ecf1', headerBg: '#ffffff',
  },
  dark: {
    bg: '#121212', card: '#1e1e1e', text: '#d4d4d4',
    textSecondary: '#8a8a8a', border: '#2c2c2c', headerBg: '#1e1e1e',
  },
  eye: {
    bg: '#C7EDCC', card: '#D5F2D9', text: '#2d4a2d',
    textSecondary: '#5a7a5a', border: '#a8d8b0', headerBg: '#D5F2D9',
  },
  sepia: {
    bg: '#F5E6C8', card: '#FAF0DC', text: '#5b4636',
    textSecondary: '#8b7355', border: '#e0d0b8', headerBg: '#FAF0DC',
  },
}

function loadFromStorage(): ReaderSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) }
  } catch { /* ignore */ }
  return { ...DEFAULT_SETTINGS }
}

let _instance: ReturnType<typeof createReaderSettings> | null = null

function createReaderSettings() {
  const settings = reactive<ReaderSettings>(loadFromStorage())

  watch(
    () => ({ ...settings }),
    (val) => {
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(val)) } catch { /* ignore */ }
    },
    { deep: true },
  )

  const currentThemeStyle = computed(() => THEME_STYLES[settings.theme])
  const currentFontFamily = computed(() =>
    FONT_OPTIONS.find(f => f.key === settings.fontFamily)?.family ?? FONT_OPTIONS[0].family,
  )

  function increaseFontSize() {
    if (settings.fontSize < FONT_SIZE_MAX) settings.fontSize += FONT_SIZE_STEP
  }
  function decreaseFontSize() {
    if (settings.fontSize > FONT_SIZE_MIN) settings.fontSize -= FONT_SIZE_STEP
  }
  function setTheme(theme: ReaderTheme) { settings.theme = theme }
  function setLineHeight(lh: number) { settings.lineHeight = lh }
  function setFontFamily(f: ReaderFont) { settings.fontFamily = f }
  function setBrightness(b: number) { settings.brightness = Math.max(BRIGHTNESS_MIN, Math.min(BRIGHTNESS_MAX, b)) }
  function resetSettings() { Object.assign(settings, DEFAULT_SETTINGS) }

  return {
    settings,
    currentThemeStyle,
    currentFontFamily,
    increaseFontSize,
    decreaseFontSize,
    setTheme,
    setLineHeight,
    setFontFamily,
    setBrightness,
    resetSettings,
  }
}

export function useReaderSettings() {
  if (!_instance) _instance = createReaderSettings()
  return _instance
}
