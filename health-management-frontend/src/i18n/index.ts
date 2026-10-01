import { readonly, ref, watch } from 'vue'
import { zhMessages } from './zh'

export type AppLocale = 'en' | 'zh-CN'
export const LANGUAGE_STORAGE_KEY = 'health-management-language'

export function resolveLocale(value: string | null | undefined, browserLanguage = 'en'): AppLocale {
  if (value === 'en' || value === 'zh-CN') return value
  return browserLanguage.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en'
}

function initialLocale(): AppLocale {
  try {
    return resolveLocale(localStorage.getItem(LANGUAGE_STORAGE_KEY), navigator.language)
  } catch {
    return resolveLocale(null, typeof navigator === 'undefined' ? 'en' : navigator.language)
  }
}

const currentLocale = ref<AppLocale>(initialLocale())
export const locale = readonly(currentLocale)
export const dateLocale = () => currentLocale.value === 'zh-CN' ? 'zh-CN' : 'en-GB'

export function setLocale(value: AppLocale) {
  currentLocale.value = value
  try { localStorage.setItem(LANGUAGE_STORAGE_KEY, value) } catch { /* Still switch for this session. */ }
}

// English source phrases serve as readable keys; unknown text is preserved.
export function t(value: unknown): string {
  if (value === null || value === undefined) return ''
  const text = String(value)
  if (currentLocale.value === 'en') return text
  const key = text.trim().replace(/\s+/g, ' ')
  const translated = zhMessages[key]
  return typeof translated === 'string' ? translated : text
}

watch(currentLocale, (value) => {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = value
    document.title = value === 'zh-CN' ? 'HMS · 个人健康管理' : 'HMS · Health management'
  }
}, { immediate: true })

if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key === LANGUAGE_STORAGE_KEY) currentLocale.value = resolveLocale(event.newValue, navigator.language)
  })
}
