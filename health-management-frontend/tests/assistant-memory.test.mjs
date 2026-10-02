import assert from 'node:assert/strict'
import test from 'node:test'
import { createTypeScriptLoader } from './helpers/loadTypescript.mjs'

test('memory transport uses owned conversation endpoints and revision, never browser history', async () => {
  const requests = []
  const client = { async get(path) { requests.push({ path }); return { data: { enabled: true, revision: 3 } } }, async patch(path, body) { requests.push({ path, body }); return { data: { ...body, revision: 4 } } } }
  const load = createTypeScriptLoader({ mocks: new Map([[new URL('../src/api/client.ts', import.meta.url), { default: client }]]) })
  const { memoryApi } = load(new URL('../src/features/assistant/memory/api.ts', import.meta.url))
  assert.equal((await memoryApi.conversation('owned-chat')).enabled, true)
  assert.equal((await memoryApi.toggle('owned-chat', 3, false)).enabled, false)
  assert.deepEqual(requests[1], { path: '/assistant/conversations/owned-chat/memory', body: { expectedRevision: 3, enabled: false } })
})

test('personal memory transport sends revisions, explicit confirmation and extraction IDs', async () => {
  const requests = []
  const client = {
    async get(path) { requests.push({ path }); return { data: path.endsWith('/messages') ? { conversation: { revision: 7 } } : { settings: { enabled: true }, items: [] } } },
    async post(path, body) { requests.push({ path, body }); return { data: body } },
    async patch(path, body) { requests.push({ path, body }); return { data: body } },
    async delete(path, options) { requests.push({ path, options }) },
  }
  const load = createTypeScriptLoader({ mocks: new Map([[new URL('../src/api/client.ts', import.meta.url), { default: client }]]) })
  const { memoryApi } = load(new URL('../src/features/assistant/memory/api.ts', import.meta.url))
  await memoryApi.extract('owned-chat', 'zh-CN', 'idempotent-id')
  assert.deepEqual(requests[1], { path: '/assistant/conversations/owned-chat/memory/extractions', body: { requestId: 'idempotent-id', expectedRevision: 7, language: 'zh-CN' } })
  await memoryApi.change('candidate', { expectedRevision: 2, category: 'diet', content: '素食', confirmed: true, replaceId: 'old', replaceRevision: 4 })
  assert.equal(requests[2].body.confirmed, true)
  assert.equal(requests[2].body.replaceRevision, 4)
  await memoryApi.remove({ id: 'item', revision: 3 })
  assert.deepEqual(requests[3], { path: '/assistant/memory/items/item', options: { params: { expectedRevision: 3 } } })
  await memoryApi.clear(9)
  assert.deepEqual(requests[4], { path: '/assistant/memory', options: { params: { expectedRevision: 9 } } })
})
