import assert from 'node:assert/strict'
import test from 'node:test'
import { createTypeScriptLoader } from './helpers/loadTypescript.mjs'
const { composerKeyAction, composerSendAction, insertComposerNewline } = createTypeScriptLoader()(new URL('../src/features/assistant/composer.ts', import.meta.url))
const key = overrides => ({ key: 'Enter', ctrlKey: false, shiftKey: false, altKey: false, metaKey: false, isComposing: false, repeat: false, keyCode: 13, ...overrides })
test('enabled Enter sends and Ctrl Enter inserts a newline; other modifiers retain native behavior', () => {
  assert.equal(composerKeyAction(key(), true, false), 'send')
  assert.equal(composerKeyAction(key({ ctrlKey: true }), true, false), 'newline')
  for (const field of ['shiftKey', 'altKey', 'metaKey']) assert.equal(composerKeyAction(key({ [field]: true }), true, false), 'native')
  assert.equal(composerKeyAction(key(), false, false), 'native')
  assert.equal(composerKeyAction(key({ ctrlKey: true }), false, false), 'native')
})
test('IME selection and repeated Enter never submit', () => {
  for (const event of [key({ isComposing: true }), key({ keyCode: 229 })]) assert.equal(composerKeyAction(event, true, false), 'native')
  assert.equal(composerKeyAction(key(), true, true), 'native')
  assert.equal(composerKeyAction(key({ repeat: true }), true, false), 'ignore')
  assert.equal(composerKeyAction(key(), true, false), 'send')
})
test('Ctrl Enter replaces the selected text and returns the actual caret position', () => {
  assert.deepEqual(insertComposerNewline('abcXYZdef', 3, 6), { text: 'abc\ndef', caret: 4 })
  assert.deepEqual(insertComposerNewline('你好', 1, 1), { text: '你\n好', caret: 2 })
})
test('all blank inputs warn before editing confirmation; unavailable and duplicate sends stay blocked', () => {
  for (const text of ['', ' ', '\t\n', '　', '  \n　']) assert.equal(composerSendAction(text, false, true, false), 'blank')
  assert.equal(composerSendAction('你好', false, true, false), 'confirm')
  assert.equal(composerSendAction('你好', false, true, true), 'send')
  assert.equal(composerSendAction('你好', true, false, false), 'blocked')
})
