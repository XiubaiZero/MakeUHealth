import assert from 'node:assert/strict'
import test from 'node:test'
import { createTypeScriptLoader } from './helpers/loadTypescript.mjs'

const load = createTypeScriptLoader()
const { createConversationSession } = load(new URL('../src/features/assistant/conversations/session.ts', import.meta.url))
const { readLegacyHistory, legacyImportAvailable, markLegacyImported } = load(new URL('../src/features/assistant/conversations/legacy.ts', import.meta.url))
function storage() { const values = new Map(); return { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value) } }
const failure = status => Object.assign(new Error(`failure ${status ?? 'network'}`), { status })
function server() {
  const conversations = new Map(), history = new Map(), tasks = new Map(), requests = []
  let counter = 0
  const clone = value => JSON.parse(JSON.stringify(value))
  const api = {
    async list() { return { items: [...conversations.values()].map(clone), hasMore: false } },
    async create() { const item = { id: `chat-${++counter}`, title: null, revision: 0, createdAt: '2026-10-02', updatedAt: '2026-10-02' }; conversations.set(item.id, item); history.set(item.id, []); return clone(item) },
    async history(id, before) { if (!conversations.has(id)) throw failure(404); const filtered = history.get(id).filter(item => before === undefined || item.sequence < before); return { conversation: clone(conversations.get(id)), messages: clone(filtered.slice(-80)), nextBefore: filtered.length > 80 ? filtered.at(-80).sequence : null, activeTask: null, failedTask: null } },
    async send(id, body) {
      requests.push(clone(body)); const key = `${id}:${body.requestId}`
      if (tasks.has(key)) return clone(tasks.get(key))
      const conversation = conversations.get(id)
      if (conversation.revision !== body.expectedRevision) throw failure(409)
      const rows = history.get(id)
      const question = { id: `q-${body.requestId}`, sequence: rows.length+1, role: 'user', content: body.message }
      rows.push(question, { id: `a-${body.requestId}`, sequence: rows.length+2, role: 'assistant', content: 'saved answer', source: 'local' })
      const task = { id: body.requestId, requestId: body.requestId, questionId: question.id, answerId: `a-${body.requestId}`, status: 'completed', error: null }
      tasks.set(key, task); conversation.revision += 2; return clone(task)
    },
    async task(id, request) { if (!tasks.has(`${id}:${request}`)) throw failure(404); return clone(tasks.get(`${id}:${request}`)) },
    async rename(id, expected, title) { if (conversations.get(id).revision !== expected) throw failure(409); conversations.get(id).title = title; conversations.get(id).revision++; return clone(conversations.get(id)) },
    async remove(id) { conversations.delete(id); history.delete(id) },
    async removeMessages(id, expected, ids) { if (conversations.get(id).revision !== expected) throw failure(409); history.set(id, history.get(id).filter(item => !ids.includes(item.id))); conversations.get(id).revision++ },
  }
  return { api, conversations, history, requests }
}
const input = { language: 'zh-CN', context: { age: 20 }, constraints: [], mode: 'local' }
function session(api, options = {}) { return createConversationSession(api, { scope: () => 'account-id-1', pollInterval: 0, storage: storage(), ...options }) }

test('two device sessions load the same server records and preserve independent drafts', async () => {
  const remote = server(), first = session(remote.api), second = session(remote.api)
  try {
    await first.refresh(); first.draft.value = '健康计划'; await first.send(input)
    assert.equal(first.messages.value.length, 2); assert.equal(first.draft.value, '')
    await second.refresh(); assert.deepEqual(second.messages.value, first.messages.value)
    second.draft.value = 'my draft'; await first.rename('renamed'); await second.refresh()
    assert.equal(second.current.value.title, 'renamed'); assert.equal(second.draft.value, 'my draft')
  } finally { first.dispose(); second.dispose() }
})
test('lost response looks up the accepted task and does not submit a second question', async () => {
  const remote = server(), original = remote.api.send
  remote.api.send = async (...args) => { await original(...args); throw failure() }
  const state = session(remote.api)
  try { await state.refresh(); state.draft.value = 'fitness plan'; await state.send(input); assert.equal(remote.requests.length, 1); assert.equal(state.messages.value.length, 2); assert.equal(state.unconfirmed.value, false); assert.equal(state.draft.value, '') }
  finally { state.dispose() }
})
test('unknown submission retains UUID across page recreation and retries the original request', async () => {
  const remote = server(), original = remote.api.send, attempted = []
  remote.api.send = async (id, body) => { attempted.push(body.requestId); throw failure() }
  const tab = storage(), first = session(remote.api, { storage: tab })
  await first.refresh(); first.draft.value = 'fitness plan'; await first.send(input)
  assert.equal(first.draft.value, 'fitness plan'); assert.equal(first.unconfirmed.value, true)
  first.dispose()
  const second = session(remote.api, { storage: tab })
  try {
    assert.equal(second.unconfirmed.value, true); assert.equal(second.draft.value, 'fitness plan')
    remote.api.send = async (id, body) => { attempted.push(body.requestId); return original(id, body) }
    await second.resolvePending(); assert.equal(new Set(attempted).size, 1); assert.equal(second.messages.value.length, 2); assert.equal(second.unconfirmed.value, false)
  } finally { second.dispose() }
})
test('storage outage keeps loaded messages and draft and pauses sends', async () => {
  const remote = server(), state = session(remote.api)
  try {
    await state.refresh(); state.draft.value = 'health'; await state.send(input); state.draft.value = 'next question'
    remote.api.history = async () => { throw failure(503) }
    await state.refresh(); assert.equal(state.online.value, false); assert.equal(state.messages.value.length, 2); assert.equal(state.draft.value, 'next question')
    await state.send(input); assert.equal(remote.requests.length, 1)
  } finally { state.dispose() }
})
test('conflicting update refreshes server records without discarding an edited draft', async () => {
  const remote = server(), state = session(remote.api)
  try {
    await state.refresh(); state.draft.value = 'health'; await state.send(input)
    state.editingId.value = state.messages.value[0].id; state.draft.value = 'edited question'
    remote.conversations.get(state.current.value.id).revision++
    await state.send(input); assert.match(state.error.value, /another device/); assert.equal(state.draft.value, 'edited question'); assert.ok(state.editingId.value); assert.equal(state.messages.value.length, 2)
  } finally { state.dispose() }
})
test('pagination retrieves all messages and switching keeps each conversation draft', async () => {
  const remote = server(), first = await remote.api.create(), second = await remote.api.create()
  remote.history.set(first.id, Array.from({ length: 105 }, (_, i) => ({ id: `${i}`, sequence: i+1, role: 'user', content: `${i}` })))
  const state = session(remote.api)
  try {
    await state.select(first); assert.equal(state.messages.value.length, 80); await state.older(); assert.equal(state.messages.value.length, 105); assert.equal(state.nextBefore.value, null)
    state.draft.value = 'first draft'; await state.select(second); assert.equal(state.draft.value, ''); state.draft.value = 'second draft'; await state.select(first); assert.equal(state.draft.value, 'first draft')
  } finally { state.dispose() }
})
test('response from a previous selection or account cannot overwrite the current page', async () => {
  const remote = server(), first = await remote.api.create(), second = await remote.api.create(), original = remote.api.history
  let finish; remote.api.history = async id => id === first.id ? new Promise(resolve => { finish = resolve }) : original(id)
  let scope = 'account-id-1'; const state = session(remote.api, { scope: () => scope })
  try {
    const previous = state.select(first); await state.select(second); finish(await original(first.id)); await previous; assert.equal(state.current.value.id, second.id)
    state.draft.value = 'private draft'; scope = 'account-id-2'; remote.api.list = async () => ({ items: [], hasMore: false }); await state.refresh()
    assert.equal(state.draft.value, ''); assert.equal(state.messages.value.length, 0); assert.equal(state.current.value, null)
  } finally { state.dispose() }
})
test('local history is account-scoped, optional, deduplicated and never removed', () => {
  const saved = storage(), scope = 'account-id-1', key = `smart-assistant-chat-history-v2:${scope}`
  const raw = JSON.stringify([{ id: 1, role: 'user', content: 'original text' }, { id: 2, role: 'assistant', content: 'reply', suggestionPrompts: ['try', null] }, { id: 3, role: 'invalid', content: 'invalid' }])
  saved.setItem(key, raw); assert.equal(readLegacyHistory(saved, 'guest').length, 0); assert.equal(readLegacyHistory(saved, 'account-id-2').length, 0)
  const imported = readLegacyHistory(saved, scope); assert.equal(imported.length, 2); assert.equal(legacyImportAvailable(saved, scope), true)
  markLegacyImported(saved, scope, imported); assert.equal(legacyImportAvailable(saved, scope), false); assert.equal(saved.getItem(key), raw)
  saved.setItem(key, '{broken'); assert.deepEqual(readLegacyHistory(saved, scope), [])
})
test('page recreation restores selected conversation and its draft despite newer conversations', async () => {
  const remote = server(), selected = await remote.api.create(), tab = storage()
  const first = session(remote.api, { storage: tab }); await first.select(selected); first.draft.value = 'retained draft'; first.dispose()
  await remote.api.create(); remote.api.list = async () => ({ items: [...remote.conversations.values()].reverse(), hasMore: false })
  const second = session(remote.api, { storage: tab })
  try { await second.refresh(); assert.equal(second.current.value.id, selected.id); assert.equal(second.draft.value, 'retained draft') }
  finally { second.dispose() }
})
test('explicit retry conflict clears uncertainty while retaining the draft', async () => {
  const remote = server(), original = remote.api.send
  remote.api.send = async () => { throw failure() }
  const state = session(remote.api)
  try {
    await state.refresh(); state.draft.value = 'unsaved question'; await state.send(input); assert.equal(state.unconfirmed.value, true)
    remote.conversations.get(state.current.value.id).revision++
    remote.api.send = original
    await state.resolvePending(); assert.equal(state.unconfirmed.value, false); assert.equal(state.draft.value, 'unsaved question'); assert.equal(state.messages.value.length, 0)
  } finally { state.dispose() }
})
test('real conversation transport sends authenticated-client payloads, revision and cursor', async () => {
  const calls = [], client = Object.fromEntries(['get','post','patch','delete'].map(method => [method, async (...args) => { calls.push({ method, args }); return { data: {} } }]))
  const module = createTypeScriptLoader({ mocks: new Map([[new URL('../src/api/client.ts', import.meta.url), { default: client }]]) })(new URL('../src/features/assistant/conversations/api.ts', import.meta.url))
  const api = module.conversationApi
  await api.history('chat', 25); await api.send('chat', { ...input, requestId: 'id', expectedRevision: 3, editMessageId: null }); await api.remove('chat', 4)
  assert.deepEqual(calls[0].args[1].params, { before: 25, limit: 80 }); assert.equal(calls[1].args[2].timeout, 60000); assert.equal(calls[1].args[1].expectedRevision, 3); assert.deepEqual(calls[2].args[1].params, { expectedRevision: 4 })
})
