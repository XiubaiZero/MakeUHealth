<template>
  <div class="memory-control">
    <label><input type="checkbox" :checked="settings?.enabled ?? true" :disabled="disabled || busy || !settings" @change="toggle" />{{ t('Conversation memory') }}</label>
    <small v-if="settings && !settings.enabled">{{ t('History is saved, but is not sent to the model.') }}</small>
    <p v-if="error" role="status">{{ t(error) }}</p>
    <button type="button" :aria-expanded="managing" :disabled="!conversationId" @click="managing = !managing">{{ t(managing ? 'Close memory manager' : 'Manage memory') }}</button>
    <MemoryManager v-if="managing" :conversation-id="conversationId" />
  </div>
</template>
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { memoryApi, type ConversationMemory } from '../features/assistant/memory/api'
import { t } from '../i18n'
import { getAuthStorageScope } from '../utils/auth'
import MemoryManager from './MemoryManager.vue'
const props = defineProps<{ conversationId: string; disabled?: boolean }>()
const settings = ref<ConversationMemory | null>(null)
const busy = ref(false)
const error = ref('')
const managing = ref(false)
let epoch = 0
onBeforeUnmount(() => { epoch++ })
watch(() => props.conversationId, async id => {
  const ticket = ++epoch, scope = getAuthStorageScope(); settings.value = null; error.value = ''
  if (!id) return
  busy.value = true
  try { const result = await memoryApi.conversation(id); if (ticket === epoch && scope === getAuthStorageScope()) settings.value = result }
  catch { if (ticket === epoch) error.value = 'Memory storage is unavailable. Please retry.' }
  finally { if (ticket === epoch) busy.value = false }
}, { immediate: true })
async function toggle() {
  if (!settings.value || busy.value) return
  const ticket = epoch, id = props.conversationId, scope = getAuthStorageScope(); busy.value = true; error.value = ''
  try { const result = await memoryApi.toggle(id, settings.value.revision, !settings.value.enabled); if (ticket === epoch && scope === getAuthStorageScope()) settings.value = result }
  catch { if (ticket === epoch) { error.value = 'Memory changed on another device. Refresh and try again.'; const fresh = await memoryApi.conversation(id).catch(() => null); if (ticket === epoch && scope === getAuthStorageScope()) settings.value = fresh } }
  finally { if (ticket === epoch) busy.value = false }
}
</script>
<style scoped>
.memory-control { display: grid; gap: 4px; max-width: 100%; font-size: 13px; color: #375d62; }
.memory-control label { display: flex; align-items: center; gap: 7px; cursor: pointer; }
.memory-control input { accent-color: #408f88; }
.memory-control small, .memory-control p { margin: 0; overflow-wrap: anywhere; font-size: 12px; }
.memory-control > button { justify-self: start; border: 1px solid #bad6cc; border-radius: 8px; padding: 6px 10px; background: #eaf4ef; color: #375d62; cursor: pointer; }
</style>
