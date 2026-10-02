import { computed, ref, watch } from 'vue'
import type { Conversation, ConversationApi, ConversationMessage, GenerationTask, TurnInput, TurnRequest } from './types'

export function requestId() {
  // getRandomValues also works on an HTTP LAN origin where randomUUID may be unavailable.
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = ((bytes[6] ?? 0) & 15) | 64
  bytes[8] = ((bytes[8] ?? 0) & 63) | 128
  const hex = [...bytes].map(value => value.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export function createConversationSession(api: ConversationApi, options: {
  scope: () => string; storage?: Pick<Storage, 'getItem' | 'setItem'>; pollInterval?: number
}) {
  const conversations = ref<Conversation[]>([])
  const current = ref<Conversation | null>(null)
  const messages = ref<ConversationMessage[]>([])
  const activeTask = ref<GenerationTask | null>(null)
  const failedTask = ref<GenerationTask | null>(null)
  const nextBefore = ref<number | null>(null)
  const hasMoreConversations = ref(false)
  const working = ref(false)
  const online = ref(false)
  const error = ref('')
  const draft = ref('')
  const editingId = ref<string | null>(null)
  const unconfirmed = ref(false)
  const pendingQuestion = ref('')
  const sending = computed(() => activeTask.value !== null)
  let scope = options.scope()
  let disposed = false
  let suspended = false
  let epoch = 0
  let timer: ReturnType<typeof setTimeout> | undefined
  let pending: { conversation: string; request: TurnRequest } | null = null
  const draftMemory = new Map<string, string>()
  const draftKey = () => `assistant-draft-v1:${scope}:${current.value?.id || 'new'}`
  function saveSelection() {
    try { options.storage?.setItem(`assistant-selected-v1:${scope}`, JSON.stringify(current.value?.id ?? null)) } catch { /* Keep the current selection in memory. */ }
  }
  function savePending(value: typeof pending) {
    pending = value
    try { options.storage?.setItem(`assistant-pending-v1:${scope}`, JSON.stringify(value)) } catch { /* Keep this request in memory. */ }
  }
  function storeDraft() {
    const raw = JSON.stringify({ draft: draft.value, editingId: editingId.value })
    draftMemory.set(draftKey(), raw)
    try { options.storage?.setItem(draftKey(), raw) } catch { /* Retain memory when storage is full. */ }
  }
  function restoreDraft() {
    try {
      const raw = draftMemory.get(draftKey()) || options.storage?.getItem(draftKey()) || '{}'
      const saved = JSON.parse(raw)
      draft.value = typeof saved.draft === 'string' ? saved.draft : ''
      editingId.value = typeof saved.editingId === 'string' ? saved.editingId : null
    } catch { draft.value = ''; editingId.value = null }
  }
  const stopDraftWatch = watch([draft, editingId], storeDraft, { flush: 'post' })
  function stopPolling() { if (timer !== undefined) clearTimeout(timer); timer = undefined }
  function valid(ticket: number) { return !disposed && ticket === epoch && scope === options.scope() }
  function checkScope() {
    if (scope === options.scope()) return
    stopPolling(); epoch++; scope = options.scope(); pending = null; unconfirmed.value = false
    current.value = null; conversations.value = []; messages.value = []; activeTask.value = null; failedTask.value = null
    online.value = false; draft.value = ''; editingId.value = null; restoreDraft()
  }
  function failure(cause: unknown) {
    const status = (cause as { status?: number })?.status
    error.value = cause instanceof Error ? cause.message : 'Chat storage is unavailable. Your draft has been kept.'
    if (status === undefined || status >= 500) online.value = false
  }
  function schedulePoll() {
    stopPolling()
    if (!activeTask.value || !current.value || disposed || suspended || options.pollInterval === 0) return
    timer = setTimeout(async () => {
      await refreshHistory()
      if (online.value) schedulePoll()
    }, options.pollInterval ?? 2000)
  }
  async function refreshList(append = false) {
    const ticket = epoch
    const page = await api.list(append ? conversations.value.length : 0)
    if (!valid(ticket)) return
    conversations.value = append ? [...new Map([...conversations.value, ...page.items].map(item => [item.id, item])).values()] : page.items
    hasMoreConversations.value = page.hasMore
  }
  async function refreshHistory() {
    checkScope()
    if (!current.value) return
    const id = current.value.id, ticket = epoch
    try {
      const history = await api.history(id)
      if (!valid(ticket) || current.value?.id !== id) return
      if (history.conversation.revision < current.value.revision) return
      current.value = history.conversation; messages.value = history.messages
      activeTask.value = history.activeTask; failedTask.value = history.failedTask; nextBefore.value = history.nextBefore
      online.value = true
      if (pending?.conversation === id && history.messages.some(message => message.role === 'user' && message.content === pending?.request.message)) {
        // Only a task lookup confirms a particular request; matching text alone is insufficient.
        await confirmPending(false)
      }
      const entry = conversations.value.findIndex(item => item.id === id)
      if (entry >= 0) conversations.value[entry] = history.conversation
      schedulePoll()
    } catch (cause) {
      if (!valid(ticket)) return
      failure(cause)
      if ((cause as { status?: number }).status === 404) {
        storeDraft(); current.value = null; saveSelection(); messages.value = []; activeTask.value = null; failedTask.value = null; savePending(null); unconfirmed.value = false
        error.value = 'Conversation was deleted. Your draft has been kept.'
        await refreshList().catch(failure)
      }
    }
  }
  async function select(conversation: Conversation) {
    if (working.value || unconfirmed.value) return
    storeDraft(); stopPolling(); epoch++
    current.value = conversation; messages.value = []; activeTask.value = null; failedTask.value = null; nextBefore.value = null
    saveSelection()
    pending = null; unconfirmed.value = false; pendingQuestion.value = ''; error.value = ''; restoreDraft()
    await refreshHistory()
  }
  async function refresh() {
    checkScope()
    if (working.value || disposed) return
    error.value = ''
    try {
      await refreshList()
      if (disposed || scope !== options.scope()) return
      if (!current.value && conversations.value[0]) await select(conversations.value[0])
      else await refreshHistory()
      if (!current.value) online.value = true
      if (unconfirmed.value) await confirmPending(false)
    } catch (cause) { failure(cause) }
  }
  async function older() {
    if (!current.value || nextBefore.value === null || working.value) return
    working.value = true
    const id = current.value.id, ticket = epoch
    try {
      const history = await api.history(id, nextBefore.value)
      if (!valid(ticket) || current.value?.id !== id) return
      if (history.conversation.revision !== current.value.revision) {
        error.value = 'Conversation changed on another device. Refresh and try again.'
        await refreshHistory(); return
      }
      messages.value = [...history.messages, ...messages.value]; nextBefore.value = history.nextBefore
      online.value = true
    } catch (cause) { if (valid(ticket)) failure(cause) }
    finally { working.value = false }
  }
  async function create() {
    if (working.value || unconfirmed.value) return
    working.value = true; const ticket = epoch
    try {
      const conversation = await api.create()
      if (!valid(ticket)) return
      working.value = false; await select(conversation); await refreshList(); online.value = true
    } catch (cause) { if (valid(ticket)) failure(cause) }
    finally { working.value = false }
  }
  function accepted(task: GenerationTask) {
    if (!pending) return
    if (task.status === 'deleted') {
      savePending(null); unconfirmed.value = false; pendingQuestion.value = ''
      error.value = 'This request belongs to deleted history.'; return
    }
    if (draft.value.trim() === pending.request.message && editingId.value === pending.request.editMessageId) { draft.value = ''; editingId.value = null; storeDraft() }
    activeTask.value = task.status === 'queued' || task.status === 'running' ? task : null
    savePending(null); unconfirmed.value = false; pendingQuestion.value = ''; online.value = true
  }
  async function confirmPending(resubmitMissing: boolean) {
    if (!pending) return
    const ticket = epoch, submission = pending
    try {
      const task = await api.task(submission.conversation, submission.request.requestId)
      if (valid(ticket) && pending === submission) accepted(task)
    } catch (cause) {
      if (!valid(ticket) || pending !== submission) return
      const status = (cause as { status?: number }).status
      if (status === 404 && resubmitMissing) {
        try { const task = await api.send(submission.conversation, submission.request); if (valid(ticket) && pending === submission) accepted(task) }
        catch (retryError) {
          if (!valid(ticket)) return
          failure(retryError)
          const retryStatus = (retryError as { status?: number }).status
          if (retryStatus !== undefined && retryStatus < 500) { savePending(null); unconfirmed.value = false; pendingQuestion.value = '' }
        }
      } else if (status === 410) { savePending(null); unconfirmed.value = false; failure(cause) }
      else { failure(cause) }
    }
  }
  async function resolvePending() {
    if (working.value || !pending) return
    working.value = true
    try { await confirmPending(true); await refreshHistory() }
    finally { working.value = false }
  }
  async function send(input: TurnInput) {
    checkScope()
    if (working.value || sending.value || unconfirmed.value || !online.value || !draft.value.trim()) return
    working.value = true; error.value = ''; const ticket = epoch
    try {
      if (!current.value) {
        const originalDraftKey = draftKey()
        const conversation = await api.create()
        if (!valid(ticket)) return
        current.value = conversation; storeDraft()
        saveSelection()
        draftMemory.delete(originalDraftKey)
        try { options.storage?.setItem(originalDraftKey, '{}') } catch { /* The draft now belongs to the created conversation. */ }
      }
      const request: TurnRequest = { ...input, message: draft.value.trim(), requestId: requestId(), expectedRevision: current.value.revision, editMessageId: editingId.value }
      savePending({ conversation: current.value.id, request }); pendingQuestion.value = request.message
      const task = await api.send(current.value.id, request)
      if (!valid(ticket)) return
      accepted(task)
      await refreshHistory(); await refreshList()
    } catch (cause) {
      if (!valid(ticket)) return
      const status = (cause as { status?: number }).status
      failure(cause)
      if (pending && (status === undefined || status >= 500)) { unconfirmed.value = true; await confirmPending(false); await refreshHistory() }
      else { savePending(null); unconfirmed.value = false; pendingQuestion.value = ''; if (status === 409) { await refreshHistory(); error.value = 'Conversation changed on another device. Refresh and try again.' } }
    } finally { working.value = false }
  }
  async function mutate(action: (conversation: Conversation) => Promise<unknown>) {
    if (!current.value || working.value) return false
    working.value = true; const ticket = epoch
    try {
      await action(current.value)
      if (!valid(ticket)) return false
      online.value = true; return true
    } catch (cause) {
      if (valid(ticket)) { failure(cause); if ((cause as { status?: number }).status === 409) { await refreshHistory(); error.value = 'Conversation changed on another device. Refresh and try again.' } }
      return false
    } finally { working.value = false }
  }
  async function rename(title: string) {
    if (await mutate(item => api.rename(item.id, item.revision, title))) { await refreshHistory(); await refreshList().catch(failure) }
  }
  async function remove() {
    if (!await mutate(item => api.remove(item.id, item.revision))) return false
    stopPolling(); epoch++; current.value = null; messages.value = []; activeTask.value = null; failedTask.value = null; nextBefore.value = null
    saveSelection()
    savePending(null); unconfirmed.value = false; draft.value = ''; editingId.value = null
    await refresh(); return true
  }
  async function removeMessages(ids: string[]) {
    if (!await mutate(item => api.removeMessages(item.id, item.revision, ids))) return false
    await refreshHistory(); return true
  }
  function suspend() { suspended = true; stopPolling() }
  function resume() { suspended = false }
  function dispose() { storeDraft(); disposed = true; epoch++; stopPolling(); stopDraftWatch() }
  try {
    const stored = JSON.parse(options.storage?.getItem(`assistant-pending-v1:${scope}`) || 'null')
    if (typeof stored?.conversation === 'string' && typeof stored?.request?.requestId === 'string' && typeof stored.request.message === 'string') {
      pending = stored; unconfirmed.value = true; pendingQuestion.value = stored.request.message
      current.value = { id: stored.conversation, title: null, revision: stored.request.expectedRevision, createdAt: '', updatedAt: '' }
    }
  } catch { /* Ignore invalid request metadata. */ }
  if (!current.value) {
    try {
      const selected = JSON.parse(options.storage?.getItem(`assistant-selected-v1:${scope}`) || 'null')
      if (typeof selected === 'string') current.value = { id: selected, title: null, revision: 0, createdAt: '', updatedAt: '' }
    } catch { /* Ignore invalid selection metadata. */ }
  }
  restoreDraft()
  return { conversations, current, messages, activeTask, failedTask, nextBefore, hasMoreConversations, working, online, error,
    draft, editingId, unconfirmed, pendingQuestion, sending, refresh, refreshHistory, refreshList, select, older, create, send,
    resolvePending, rename, remove, removeMessages, suspend, resume, dispose }
}
