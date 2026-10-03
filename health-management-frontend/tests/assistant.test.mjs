import assert from 'node:assert/strict'
import test from 'node:test'
import { createTypeScriptLoader } from './helpers/loadTypescript.mjs'

function create(apiClient, environment = {}, language = 'en') {
  const load = createTypeScriptLoader({ environment, mocks: new Map([
    [new URL('../src/api/client.ts', import.meta.url), { default: apiClient }],
    [new URL('../src/i18n/index.ts', import.meta.url), { locale: typeof language === 'string' ? { value: language } : language, t: value => value === 'male' ? '男' : value }],
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
  assert.match(await create(client, { VITE_ASSISTANT_MODE: 'local' })('what can you do', { hasProfile: false, last7DaysFoodCount: 0 }), /What I can do:/)
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

test('the same module observes language changes in requests and local replies', async () => {
  const language = { value: 'en' }
  const payloads = []
  const reply = create({ post: async (_url, body) => { payloads.push(body); return { data: { answer: 'answer' } } } }, {}, language)
  const context = { hasProfile: false, last7DaysFoodCount: 0 }
  await reply('fitness plan', context)
  assert.equal(await reply('what can you do', context), 'answer')
  language.value = 'zh-CN'
  await reply('fitness plan', context)
  assert.equal(await reply('what can you do', context), 'answer')
  assert.deepEqual(payloads.map(body => body.language), ['en', 'en', 'zh-CN', 'zh-CN'])
  assert.ok(payloads[2].constraints.includes('Respond in Simplified Chinese.'))
})

test('local goal summaries retain all goal types and zero values', async () => {
  const context = { hasProfile: true, last7DaysFoodCount: 0, allGoalSnapshots: [
    { goalType: 'muscle_gain', currentValue: 0, targetValue: 5, latestProgressPercentage: 0 },
    { goalType: 'fat_loss', currentValue: 20, targetValue: 15 },
    { goalType: 'weight_loss', currentValue: 75, targetValue: 70 },
  ] }
  const client = { post: async () => { assert.fail('No network expected') } }
  const english = await create(client, { VITE_ASSISTANT_MODE: 'LOCAL' })('fitness plan', context)
  assert.match(english, /muscle gain \[status: -, current: 0, target: 5, progress: 0%\]/)
  assert.match(english, /fat loss/)
  assert.match(english, /weight loss/)
  const chinese = await create(client, { VITE_ASSISTANT_MODE: 'local' }, 'zh-CN')('运动计划', context)
  assert.match(chinese, /增肌：当前 0，目标 5，进度 0%/)
  assert.match(chinese, /减脂/)
  assert.match(chinese, /减重/)
})

test('suggestion rules keep their intentional differences from local capability matching', () => {
  const load = createTypeScriptLoader()
  const rules = load(new URL('../src/features/assistant/rules.ts', import.meta.url))
  assert.equal(rules.isCapabilityQuestionLoose('what functions do you have'), true)
  assert.equal(rules.isCapabilityPromptQuestion('what functions do you have'), false)
  assert.equal(rules.resolveSuggestionPrompts('fitness plan', 'Try asking: example').length, 5)
  assert.equal(rules.resolveSuggestionPrompts('fitness plan', '普通回答'), undefined)
})
