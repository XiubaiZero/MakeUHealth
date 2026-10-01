import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import ts from 'typescript'

const source = readFileSync(new URL('../src/api/assistant.ts', import.meta.url), 'utf8')
  .replace(/^import .*$/gm, '')
  .replaceAll('import.meta.env', 'environment')
  .replaceAll('export ', '')
const compiled = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const instantiate = new Function('apiClient', 'environment', 'locale', 't', compiled + '\nreturn requestAssistantReply')
const create = (apiClient, environment, language = 'en') => instantiate(apiClient, environment, { value: language }, (value) => value === 'male' ? '男' : value)

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
