<template>
  <section class="memory-manager" :aria-label="t('Long-term memory')">
    <p>{{ t('Only confirmed facts are used in other chats. Extracting suggestions uses the model once.') }}</p>
    <label v-if="state"><input type="checkbox" :checked="state.settings.enabled" :disabled="busy" @change="toggle" /> {{ t('Long-term memory') }}</label>
    <p v-if="state">{{ t('Confirmed memories') }}: {{ confirmed.length }} / {{ state.settings.capacity }} · {{ t('Suggestions awaiting confirmation') }}: {{ pending.length }}</p>
    <div class="memory-actions">
      <button type="button" :disabled="busy" @click="refresh">{{ t('Refresh') }}</button>
      <button v-if="conversationId" type="button" :disabled="busy || !!job || !state?.settings.enabled" @click="extract">{{ t('Extract from this chat') }}</button>
      <button type="button" :disabled="busy || !state" @click="edit(null)">{{ t('Add memory') }}</button>
      <button type="button" :disabled="busy || !state?.items.length" @click="clearing = true">{{ t('Clear memories') }}</button>
    </div>
    <div v-if="clearing" class="memory-editor">
      <p>{{ t('Clear all confirmed memories and suggestions? Chat history will remain.') }}</p>
      <button type="button" :disabled="busy" @click="clear">{{ t('Confirm') }}</button>
      <button type="button" @click="clearing = false">{{ t('Cancel') }}</button>
    </div>
    <form v-if="editing" class="memory-editor" @submit.prevent="save">
      <label>{{ t('Category') }}<select v-model="category"><option v-for="value in categories" :key="value" :value="value">{{ t(categoryNames[value]!) }}</option></select></label>
      <label>{{ t('Memory content') }}<textarea v-model="content" rows="3" maxlength="400" required /></label>
      <small>{{ Array.from(content).length }} / 200</small>
      <label v-if="current?.status === 'pending'">{{ t('Replace an existing memory (optional)') }}<select v-model="replaceId"><option value="">{{ t('Keep existing memories') }}</option><option v-for="item in confirmed" :key="item.id" :value="item.id">{{ item.content }}</option></select></label>
      <div class="memory-actions"><button type="submit" :disabled="busy || !content.trim() || Array.from(content).length > 200">{{ t(current?.status === 'pending' ? 'Confirm memory' : 'Save') }}</button><button type="button" @click="editing = false">{{ t('Cancel') }}</button></div>
    </form>
    <p v-if="job" role="status">{{ t('Extracting suggestions...') }}</p>
    <p v-if="error" class="memory-error" role="alert">{{ t(error) }}</p>
    <p v-if="notice" role="status">{{ t(notice) }}</p>
    <article v-for="item in state?.items" :key="item.id" class="memory-item">
      <strong>{{ t(item.status === 'pending' ? 'Suggestions awaiting confirmation' : 'Confirmed memories') }} · {{ t(categoryNames[item.category] || 'Other') }}</strong>
      <p>{{ item.content }}</p>
      <details v-if="item.sources.length"><summary>{{ t('Source evidence') }}</summary><blockquote v-for="source in item.sources" :key="source.messageId">{{ source.evidence }}</blockquote><small>{{ t('Editing or deleting the source also removes this memory.') }}</small></details>
      <small v-else>{{ t('Manually added') }}</small>
      <div class="memory-actions"><button type="button" :disabled="busy" @click="edit(item)">{{ t(item.status === 'pending' ? 'Review and confirm' : 'Edit') }}</button><button type="button" :disabled="busy" @click="remove(item)">{{ t(item.status === 'pending' ? 'Dismiss' : 'Delete') }}</button></div>
    </article>
  </section>
</template>
<script setup lang="ts">
import { computed, ref, onBeforeUnmount, watch } from 'vue'
import { memoryApi, type MemoryState, type MemoryItem, type ExtractionJob } from '../features/assistant/memory/api'
import { locale, t } from '../i18n'
import { getAuthStorageScope } from '../utils/auth'
const props = defineProps<{ conversationId?: string }>()
const state = ref<MemoryState | null>(null), busy = ref(false), error = ref(''), notice = ref('')
const editing = ref(false), clearing = ref(false), current = ref<MemoryItem | null>(null), category = ref('diet'), content = ref(''), replaceId = ref('')
const categories = ['diet', 'training', 'routine', 'other']
const categoryNames: Record<string, string> = { diet: 'Diet', training: 'Training', routine: 'Routine', other: 'Other' }
const confirmed = computed(() => state.value?.items.filter(i => i.status === 'confirmed') || [])
const pending = computed(() => state.value?.items.filter(i => i.status === 'pending') || [])
const job = ref<ExtractionJob | null>(null)
let epoch = 0, timer: ReturnType<typeof setTimeout> | undefined
function active(ticket: number, scope: string) { return ticket === epoch && scope === getAuthStorageScope() }
async function refresh() {
  const ticket = epoch, scope = getAuthStorageScope(); busy.value = true; error.value = ''
  try { const value = await memoryApi.state(); if (active(ticket, scope)) state.value = value }
  catch (e) { if (active(ticket, scope)) error.value = e instanceof Error ? e.message : 'Memory storage is unavailable. Please retry.' }
  finally { if (active(ticket, scope)) busy.value = false }
}
async function action(work: () => Promise<unknown>) {
  if (busy.value) return
  const ticket = epoch, scope = getAuthStorageScope(); busy.value = true; error.value = ''; notice.value = ''
  try { await work(); if (active(ticket, scope)) { const fresh = await memoryApi.state(); if (active(ticket, scope)) { editing.value = false; clearing.value = false; state.value = fresh } } }
  catch (e) {
    if (active(ticket, scope)) {
      error.value = e instanceof Error ? e.message : 'Memory storage is unavailable. Please retry.'
      const fresh = await memoryApi.state().catch(() => null); if (active(ticket, scope) && fresh) state.value = fresh
    }
  } finally { if (active(ticket, scope)) busy.value = false }
}
function edit(item: MemoryItem | null) { current.value = item; category.value = item?.category || 'diet'; content.value = item?.content || ''; replaceId.value = ''; editing.value = true; clearing.value = false }
function save() {
  if (!state.value) return
  const item = current.value, replace = confirmed.value.find(i => i.id === replaceId.value)
  const change = { expectedRevision: item?.revision ?? state.value.settings.revision, category: category.value, content: content.value.trim(), confirmed: true, ...(replace ? { replaceId: replace.id, replaceRevision: replace.revision } : {}) }
  void action(() => item ? memoryApi.change(item.id, change) : memoryApi.add(change))
}
function remove(item: MemoryItem) { void action(() => memoryApi.remove(item)) }
function toggle() { if (state.value) { const { revision, enabled } = state.value.settings; void action(() => memoryApi.accountToggle(revision, !enabled)) } }
function clear() { if (state.value) { const revision = state.value.settings.revision; void action(() => memoryApi.clear(revision)) } }
async function extract() {
  if (!props.conversationId || busy.value || job.value) return
  const ticket = epoch, scope = getAuthStorageScope(), id = props.conversationId
  busy.value = true; error.value = ''; notice.value = ''
  try {
    const value = await memoryApi.extract(id, locale.value, crypto.randomUUID())
    if (active(ticket, scope)) { job.value = value; schedule(ticket, scope, id) }
  } catch (e) { if (active(ticket, scope)) error.value = e instanceof Error ? e.message : 'Memory extraction failed. Please retry.' }
  finally { if (active(ticket, scope)) busy.value = false }
}
function schedule(ticket: number, scope: string, id: string) {
  timer = setTimeout(async () => {
    if (!active(ticket, scope) || !job.value) return
    if (document.hidden) { schedule(ticket, scope, id); return }
    try {
      const next = await memoryApi.extraction(id, job.value.requestId)
      if (!active(ticket, scope)) return
      job.value = next
      if (next.status === 'completed' || next.status === 'failed') {
        job.value = null
        if (next.status === 'failed') error.value = next.error || 'Memory extraction failed. Please retry.'
        else notice.value = 'Extraction complete. Review suggestions before using them.'
        const fresh = await memoryApi.state(); if (active(ticket, scope)) state.value = fresh
      } else schedule(ticket, scope, id)
    } catch { if (active(ticket, scope)) { job.value = null; error.value = 'Memory storage is unavailable. Please retry.' } }
  }, 2000)
}
watch(() => props.conversationId, () => { epoch++; clearTimeout(timer); job.value = null; editing.value = false; clearing.value = false; state.value = null; void refresh() }, { immediate: true })
onBeforeUnmount(() => { epoch++; clearTimeout(timer) })
</script>
<style scoped>
.memory-manager { width: 100%; min-width: 0; font-size: 13px; color: #375d62; }
.memory-manager p { line-height: 1.6; overflow-wrap: anywhere; }
.memory-manager label { display: flex; gap: 7px; align-items: center; }
.memory-actions { display: flex; flex-wrap: wrap; gap: 7px; margin: 10px 0; }
button { background: #eaf4ef; border: 1px solid #bad6cc; border-radius: 8px; padding: 7px 10px; color: #234f52; cursor: pointer; }
button:disabled { opacity: .5; cursor: default; }
button:focus-visible, input:focus-visible, select:focus-visible, textarea:focus-visible { outline: 2px solid #408f88; outline-offset: 2px; }
.memory-item, .memory-editor { margin-top: 12px; padding: 12px; border: 1px solid #dce7df; border-radius: 12px; background: #fafdf9; }
.memory-editor label { display: grid; margin: 8px 0; }
textarea, select { box-sizing: border-box; max-width: 100%; width: 100%; border: 1px solid #b8ceca; border-radius: 6px; padding: 7px; font: inherit; }
blockquote { margin: 10px 0; padding-left: 10px; border-left: 3px solid #9dbeb4; overflow-wrap: anywhere; }
.memory-error { color: #a23f30; }
</style>
