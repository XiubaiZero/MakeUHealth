import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { nextTick, readonly, ref, watch } from 'vue'
import ts from 'typescript'

const translationSource = readFileSync(new URL('../src/i18n/zh.ts', import.meta.url), 'utf8')
const translations = JSON.parse(translationSource.slice(translationSource.indexOf('=') + 1))
const source = readFileSync(new URL('../src/i18n/index.ts', import.meta.url), 'utf8')
  .replace(/^import .*$/gm, '').replaceAll('export ', '')
const compiled = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const create = new Function('readonly', 'ref', 'watch', 'zhMessages', 'localStorage', 'navigator', 'document', 'window',
  compiled + '\nreturn {locale, setLocale, t, resolveLocale, dateLocale}')

function setup(language, saved) {
  const values = new Map(saved ? [['health-management-language', saved]] : [])
  const document = { documentElement: { lang: '' }, title: '' }
  const api = create(readonly, ref, watch, translations,
    { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value) },
    { language }, document, { addEventListener() {} })
  return { api, values, document }
}

test('first visit follows browser language and an explicit saved choice wins', () => {
  assert.equal(setup('zh-TW').api.locale.value, 'zh-CN')
  assert.equal(setup('en-US').api.locale.value, 'en')
  assert.equal(setup('zh-CN', 'en').api.locale.value, 'en')
  assert.equal(setup('en-US', 'invalid').api.locale.value, 'en')
})

test('switching language updates messages, document language, dates and saved preference', async () => {
  const { api, values, document } = setup('en-US')
  assert.equal(api.t('Health dashboard'), 'Health dashboard')
  api.setLocale('zh-CN')
  await nextTick()
  assert.equal(api.t('Health dashboard'), '健康总览')
  assert.equal(api.t('Sign out'), '退出登录')
  assert.equal(api.dateLocale(), 'zh-CN')
  assert.equal(document.documentElement.lang, 'zh-CN')
  assert.equal(values.get('health-management-language'), 'zh-CN')
  assert.equal(setup('en-US', values.get('health-management-language')).api.locale.value, 'zh-CN')
  api.setLocale('en')
  await nextTick()
  assert.equal(api.t('Health dashboard'), 'Health dashboard')
  assert.equal(document.documentElement.lang, 'en')
})

test('unknown and authored content is preserved and empty values are safe', () => {
  const { api } = setup('zh-CN')
  assert.equal(api.t('My personal note'), 'My personal note')
  assert.equal(api.t('constructor'), 'constructor')
  assert.equal(api.t(0), '0')
  assert.equal(api.t(null), '')
  assert.equal(api.t(undefined), '')
})
