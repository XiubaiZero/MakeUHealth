<template>
  <Teleport to="body">
    <div v-if="open" class="settings-overlay" @click.self="close">
      <section ref="dialogRef" class="settings-dialog" role="dialog" aria-modal="true" aria-labelledby="settings-title" tabindex="-1">
        <header class="settings-header">
          <button v-if="panel !== 'settings'" class="settings-icon-button" type="button" :aria-label="t('Back to settings')" @click="panel = 'settings'">←</button>
          <div>
            <p class="settings-eyebrow">HMS</p>
            <h2 id="settings-title">{{ t(panel === 'language' ? 'Language' : panel === 'memory' ? 'Long-term memory' : panel === 'logout' ? 'Sign out' : 'Settings') }}</h2>
          </div>
          <button class="settings-icon-button settings-close" type="button" :aria-label="t('Close settings')" @click="close">×</button>
        </header>

        <div v-if="panel === 'settings'" class="settings-options">
          <button class="settings-row" type="button" @click="panel = 'language'">
            <span class="settings-row-icon" aria-hidden="true">文 / A</span>
            <span class="settings-row-copy"><strong>{{ t('Language') }}</strong><small>{{ locale === 'zh-CN' ? '简体中文' : 'English' }}</small></span>
            <span aria-hidden="true">›</span>
          </button>
          <button v-if="authenticated" class="settings-row" type="button" @click="panel = 'memory'"><span class="settings-row-icon" aria-hidden="true">AI</span><span class="settings-row-copy"><strong>{{ t('Long-term memory') }}</strong><small>{{ t('Manage confirmed personal facts') }}</small></span><span aria-hidden="true">›</span></button>
          <button v-if="authenticated" class="settings-row settings-logout" type="button" @click="panel = 'logout'">
            <span class="settings-row-icon" aria-hidden="true">↪</span>
            <span class="settings-row-copy"><strong>{{ t('Sign out') }}</strong><small>{{ t('End your current session') }}</small></span>
            <span aria-hidden="true">›</span>
          </button>
        </div>

        <div v-else-if="panel === 'language'" class="settings-language">
          <p class="settings-copy">{{ t('Choose your display language. Changes apply immediately.') }}</p>
          <div class="language-options" role="group" :aria-label="t('Display language')">
            <button v-for="option in languages" :key="option.value" type="button" class="language-option" :class="{ selected: locale === option.value }" :aria-pressed="locale === option.value" @click="setLocale(option.value)">
              <span><strong :lang="option.value">{{ option.label }}</strong><small>{{ option.description }}</small></span>
              <span class="language-check" aria-hidden="true">{{ locale === option.value ? '✓' : '' }}</span>
            </button>
          </div>
          <p class="settings-hint">{{ t('Your language preference is saved on this device.') }}</p>
          <button class="settings-primary" type="button" @click="close">{{ t('Done') }}</button>
        </div>

        <MemoryManager v-else-if="panel === 'memory' && authenticated" />
        <div v-else class="settings-confirmation">
          <p class="settings-copy">{{ t('Are you sure you want to sign out?') }}</p>
          <div class="settings-actions">
            <button class="settings-secondary" type="button" @click="panel = 'settings'">{{ t('Cancel') }}</button>
            <button class="settings-danger" type="button" :disabled="signingOut" @click="confirmLogout">{{ t(signingOut ? 'Signing out...' : 'Sign out') }}</button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { locale, setLocale, t, type AppLocale } from '../i18n'
import MemoryManager from './MemoryManager.vue'

const props = defineProps<{ open: boolean; authenticated: boolean; signingOut?: boolean }>()
const emit = defineEmits<{ close: []; logout: [] }>()
const panel = ref<'settings' | 'language' | 'logout' | 'memory'>('settings')
const dialogRef = ref<HTMLElement | null>(null)
const languages: { value: AppLocale; label: string; description: string }[] = [
  { value: 'en', label: 'English', description: '英语' },
  { value: 'zh-CN', label: '简体中文', description: 'Simplified Chinese' },
]
let previousFocus: HTMLElement | null = null
let previousOverflow = ''

function close() { if (!props.signingOut) emit('close') }
function confirmLogout() { if (!props.signingOut) emit('logout') }

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') { event.preventDefault(); close(); return }
  if (event.key !== 'Tab') return
  const elements = Array.from(dialogRef.value?.querySelectorAll<HTMLElement>('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), summary, [tabindex="0"]') || []).filter(e => e.getClientRects().length)
  if (!elements?.length) return
  const first = elements[0]!, last = elements[elements.length - 1]!
  if (event.shiftKey && (document.activeElement === first || document.activeElement === dialogRef.value)) {
    event.preventDefault(); last.focus()
  } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialogRef.value)) {
    event.preventDefault(); first.focus()
  }
}

function releaseDialog() {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = previousOverflow
  if (previousFocus?.isConnected) previousFocus.focus()
}

watch(() => props.open, async (open) => {
  if (open) {
    panel.value = 'settings'
    previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', handleKeydown)
    await nextTick()
    if (props.open) dialogRef.value?.focus()
  } else releaseDialog()
})
watch(panel, async () => { await nextTick(); if (props.open) dialogRef.value?.focus() })
onBeforeUnmount(() => { if (props.open) releaseDialog() })
</script>

<style scoped>
.settings-overlay { position: fixed; inset: 0; z-index: 4000; display: grid; place-items: center; padding: 20px; background: rgba(14, 29, 32, .4); backdrop-filter: blur(5px); }
.settings-dialog { width: min(420px, 100%); max-height: calc(100dvh - 40px); overflow: auto; background: #fffdf8; border: 1px solid #dce9e4; border-radius: 26px; padding: 26px; box-shadow: 0 24px 70px rgba(20,45,49,.22); color: #12343b; }
.settings-dialog:focus { outline: none; }
.settings-header { display: flex; gap: 12px; align-items: center; margin-bottom: 24px; }
.settings-eyebrow { margin: 0 0 4px; font-size: 11px; letter-spacing: .16em; color: #4c928e; font-weight: 700; }
.settings-header h2 { margin: 0; font-size: 24px; }
.settings-icon-button { flex-shrink: 0; width: 34px; height: 34px; border: 0; border-radius: 10px; background: #edf4f0; color: #375d62; cursor: pointer; font-size: 22px; }
.settings-close { margin-left: auto; }
.settings-options { display: grid; gap: 10px; }
.settings-row { width: 100%; display: flex; align-items: center; gap: 14px; padding: 16px; border: 1px solid #e0eae4; border-radius: 16px; background: #f7faf6; color: inherit; text-align: left; cursor: pointer; }
.settings-row:hover { border-color: #6aaca5; background: #eff7f2; }
.settings-row-icon { width: 38px; height: 38px; border-radius: 12px; display: grid; place-items: center; font-size: 13px; font-weight: 700; background: #deeeea; color: #347b75; }
.settings-row-copy { flex: 1; display: grid; gap: 5px; }
.settings-row-copy strong { font-size: 15px; }
.settings-row-copy small, .language-option small { font-size: 12px; color: #69807e; }
.settings-logout .settings-row-icon { background: #f8ebe4; color: #a75c45; font-size: 22px; }
.settings-copy { margin: 0 0 18px; font-size: 14px; line-height: 1.7; color: #53716f; }
.language-options { display: grid; gap: 10px; }
.language-option { display: flex; justify-content: space-between; align-items: center; width: 100%; padding: 16px; border: 1px solid #dce7df; border-radius: 16px; background: transparent; color: #12343b; text-align: left; cursor: pointer; }
.language-option > span:first-child { display: grid; gap: 5px; }
.language-option.selected { background: #eaf5ef; border-color: #50a59c; }
.language-check { display: grid; place-items: center; width: 23px; height: 23px; border-radius: 50%; border: 1px solid #afc8bf; color: #388f85; }
.selected .language-check { border-color: #388f85; }
.settings-hint { font-size: 12px; color: #69807e; line-height: 1.6; margin: 18px 0; }
.settings-primary, .settings-secondary, .settings-danger { padding: 11px 18px; border-radius: 12px; border: 0; font: inherit; font-size: 14px; font-weight: 600; cursor: pointer; }
.settings-primary { width: 100%; background: #408f88; color: #fff; }
.settings-actions { display: flex; gap: 10px; justify-content: flex-end; }
.settings-secondary { background: #edf3ee; color: #375d62; }
.settings-danger { background: #ad624c; color: #fff; }
.settings-danger:disabled { opacity: .6; cursor: wait; }
button:focus-visible { outline: 3px solid #8bc4bb; outline-offset: 3px; }
@media (prefers-reduced-motion: no-preference) { .settings-dialog { animation: appear .16s ease-out; } @keyframes appear { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } } }
</style>
