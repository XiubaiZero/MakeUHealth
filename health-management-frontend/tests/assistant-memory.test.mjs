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
