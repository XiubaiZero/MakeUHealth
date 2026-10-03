import { readonly, ref } from 'vue'
import type { AssistantPreferences, PreferencesApi } from './api'

/** Server-owned account preference. No offline writes or cross-account response reuse. */
export function createPreferencesState(api: PreferencesApi, scope: () => string) {
  const enterSendEnabled = ref(true), revision = ref<number | null>(null), loading = ref(false), saving = ref(false), error = ref('')
  let account = scope(), epoch = 0
  function reset() {
    epoch++; account = scope(); enterSendEnabled.value = true; revision.value = null
    loading.value = false; saving.value = false; error.value = ''
  }
  function checkScope() { if (scope() !== account) reset() }
  function apply(value: AssistantPreferences) {
    if (typeof value.enterSendEnabled !== 'boolean' || !Number.isSafeInteger(value.revision) || value.revision < 0) throw new Error('Invalid chat preferences response.')
    if (revision.value === null || value.revision >= revision.value) { enterSendEnabled.value = value.enterSendEnabled; revision.value = value.revision }
  }
  async function refresh() {
    checkScope()
    if (account === 'guest' || loading.value || saving.value) return
    const ticket = ++epoch, owner = account; loading.value = true
    try {
      const value = await api.get()
      if (ticket !== epoch || owner !== scope()) return
      apply(value); error.value = ''
    } catch {
      if (ticket === epoch && owner === scope()) error.value = 'Failed to load chat preferences. The current keyboard mode is still in use.'
    } finally { if (ticket === epoch && owner === scope()) loading.value = false }
  }
  async function save(enabled: boolean) {
    checkScope()
    if (account === 'guest' || saving.value || revision.value === null) return
    const ticket = ++epoch, owner = account, expected = revision.value
    loading.value = false; saving.value = true
    try {
      const value = await api.update(enabled, expected)
      if (ticket !== epoch || owner !== scope()) return
      apply(value); error.value = ''
    } catch (failure) {
      if (ticket !== epoch || owner !== scope()) return
      if ((failure as Error & { status?: number }).status === 409) {
        saving.value = false
        await refresh()
        if (owner === scope()) error.value = 'Chat preferences changed on another device. Review the latest setting and try again.'
      } else error.value = 'Failed to save chat preferences. Your previous setting is still in use.'
    } finally { if (ticket === epoch && owner === scope()) saving.value = false }
  }
  return { enterSendEnabled: readonly(enterSendEnabled), revision: readonly(revision), loading: readonly(loading), saving: readonly(saving), error: readonly(error), refresh, save, reset, checkScope }
}
