import assert from 'node:assert/strict'
import test from 'node:test'
import { createTypeScriptLoader } from './helpers/loadTypescript.mjs'

function create(apiClient, environment = {}, language = 'en') {
  const load = createTypeScriptLoader({ environment, mocks: new Map([
    [new URL('../src/api/client.ts', import.meta.url), { default: apiClient }],
    [new URL('../src/i18n/index.ts', import.meta.url), { locale: { value: language }, t: value => value === 'male' ? '男' : value }],
  ]) })
  return load(new URL('../src/api/assistant.ts', import.meta.url)).requestAssistantReply
}

test('assistant identifies local fallback after an API failure', async () => {
  const reply = create({ post: async () => { throw new Error('unavailable') } }, {})
  const result = await reply('Give me a fitness plan', { hasProfile: true, age: 30, last7DaysFoodCount: 3 })
  assert.match(result, /age 30/)
  assert.match(result, /local fallback/)
})

test('assistant uses the successful API answer without calling it a fallback', async () => {
  const reply = create({ post: async () => ({ data: { answer: 'Test API answer' } }) }, {})
  assert.equal(await reply('Give me a fitness plan', { hasProfile: false, last7DaysFoodCount: 0 }), 'Test API answer')
})

test('assistant sends the selected language with the request and constraints', async () => {
  let payload
  const reply = create({ post: async (_url, body) => { payload = body; return { data: { answer: '测试回答' } } } }, {}, 'zh-CN')
  assert.equal(await reply('请根据我的健康数据制定运动计划', { hasProfile: true, last7DaysFoodCount: 0 }), '测试回答')
  assert.equal(payload.language, 'zh-CN')
  assert.ok(payload.constraints.includes('Respond in Simplified Chinese.'))
})

test('Chinese capability and fallback replies use Chinese', async () => {
  const reply = create({ post: async () => { throw new Error('offline') } }, {}, 'zh-CN')
  assert.match(await reply('你能做什么', { hasProfile: false, last7DaysFoodCount: 0 }), /健康数据/)
  const result = await reply('请根据我的健康数据制定运动计划', { hasProfile: true, age: 30, last7DaysFoodCount: 3 })
  assert.match(result, /30 岁/)
  assert.match(result, /本地参考回答/)
})

test('capability and local mode do not call the network', async () => {
  const client = { post: async () => { assert.fail('No network expected') } }
  assert.match(await create(client)('what can you do', { hasProfile: false, last7DaysFoodCount: 0 }), /What I can do:/)
  const local = create(client, { VITE_ASSISTANT_MODE: 'local' }, 'zh-CN')
  assert.equal(await local('write a poem', { hasProfile: false, last7DaysFoodCount: 0 }), '我只能回答身体健康、饮食营养和健身计划相关的问题，请提出这些范围内的问题。')
  assert.match(await local('运动计划', { hasProfile: false, last7DaysFoodCount: 0 }), /请先完善个人档案/)
})

test('request preserves endpoint, context, default language and 60 second timeout', async () => {
  let sent
  const context = { hasProfile: true, age: 30, last7DaysFoodCount: 3 }
  const reply = create({ post: async (...args) => { sent = args; return { data: { answer: 'answer' } } } }, { VITE_ASSISTANT_API_URL: '/custom/chat' })
  assert.equal(await reply('fitness plan', context), 'answer')
  assert.equal(sent[0], '/custom/chat')
  assert.deepEqual(sent[1].context, context)
  assert.equal(sent[1].language, 'en')
  assert.equal(sent[2].timeout, 60000)
})

test('empty API answer uses the existing unmarked fallback', async () => {
  const answer = await create({ post: async () => ({ data: { answer: '' } }) })('fitness plan', { hasProfile: true, age: 40, last7DaysFoodCount: 0 })
  assert.match(answer, /age 40/)
  assert.doesNotMatch(answer, /API is unavailable/)
})

test('parallel calls retain their own context', async () => {
  const contexts = []
  const reply = create({ post: async (_url, payload) => { contexts.push(payload.context); return { data: { answer: String(payload.context.age) } } } })
  const result = await Promise.all([20, 30].map(age => reply('fitness plan', { hasProfile: true, age, last7DaysFoodCount: 0 })))
  assert.deepEqual(result, ['20', '30'])
  assert.deepEqual(contexts.map(item => item.age), [20, 30])
})
