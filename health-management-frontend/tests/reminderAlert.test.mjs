import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import ts from 'typescript'

const compile = (source) => ts.transpileModule(source, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 },
}).outputText
const utility = await import('data:text/javascript;base64,' + Buffer.from(compile(
  readFileSync(new URL('../src/utils/reminderSchedule.ts', import.meta.url), 'utf8'),
)).toString('base64'))

// Exercise the component's script with controlled API, storage and lifecycle boundaries.
// No DOM assertions: these tests cover acknowledgement and asynchronous session behavior.
const component = readFileSync(new URL('../src/components/GlobalReminderAlert.vue', import.meta.url), 'utf8')
const script = component.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const executable = compile(script.replace(/^import .*$/gm, ''))
const create = new Function('ref', 'onMounted', 'onBeforeUnmount', 'getMyReminders', 'updateReminder',
  'getAccountId', 'isAuthenticated', 'getDueOccurrenceTime', 'parseReminderDateTime',
  'reminderAcknowledgementStorageKey', 'localStorage', 'window', executable + `
return { loadReminders, confirmTriggeredReminder, currentTriggeredItem, confirmationError, handleStorageChange }`)

function harness() {
  const state = { account: 1, reminders: [], rejectUpdate: false, updateCount: 0, unmount: null }
  const storage = new Map()
  const api = create((value) => ({ value }), () => {}, (fn) => { state.unmount = fn },
    () => state.fetch ? state.fetch() : Promise.resolve(state.reminders),
    async () => { state.updateCount++; if (state.rejectUpdate) throw new Error('offline') },
    () => state.account, () => state.account !== null,
    utility.getDueOccurrenceTime, utility.parseReminderDateTime, utility.reminderAcknowledgementStorageKey,
    { getItem: (key) => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value) },
    { addEventListener() {}, removeEventListener() {}, clearInterval() {} })
  return { state, storage, api }
}
const reminder = { id: 9, reminderType: 'exercise', reminderTime: '2020-01-01T10:00:00', repeatPattern: 'none', enabled: true }

test('failed confirmation stays visible and can be retried without premature acknowledgement', async () => {
  const { state, storage, api } = harness()
  state.reminders = [reminder]
  await api.loadReminders()
  state.rejectUpdate = true
  await api.confirmTriggeredReminder()
  assert.equal(api.currentTriggeredItem.value.reminder.id, 9)
  assert.match(api.confirmationError.value, /try again/)
  assert.equal(storage.size, 0)
  state.rejectUpdate = false
  await api.confirmTriggeredReminder()
  assert.equal(api.currentTriggeredItem.value, null)
  assert.equal(state.updateCount, 2)
  assert.equal(storage.size, 1)
  await api.loadReminders()
  assert.equal(api.currentTriggeredItem.value, null)
})

test('switching accounts clears old alerts and does not reuse their acknowledgement', async () => {
  const { state, api } = harness()
  state.reminders = [reminder]
  await api.loadReminders()
  await api.confirmTriggeredReminder()
  state.account = 2
  await api.loadReminders()
  assert.equal(api.currentTriggeredItem.value.reminder.id, 9)
  state.account = null
  await api.loadReminders()
  assert.equal(api.currentTriggeredItem.value, null)
})

test('a response from the old account cannot display alerts in a new session', async () => {
  const { state, api } = harness()
  let finish
  state.fetch = () => new Promise((resolve) => { finish = resolve })
  const pending = api.loadReminders()
  state.account = 2
  finish([reminder])
  await pending
  assert.equal(api.currentTriggeredItem.value, null)
})

test('disabled or deleted reminders are removed from the visible queue', async () => {
  const { state, api } = harness()
  state.reminders = [reminder, { ...reminder, id: 10 }]
  await api.loadReminders()
  state.reminders = [{ ...reminder, id: 10, enabled: false }]
  await api.loadReminders()
  assert.equal(api.currentTriggeredItem.value, null)
})

test('unmount ignores in-flight responses', async () => {
  const { state, api } = harness()
  let finish
  state.fetch = () => new Promise((resolve) => { finish = resolve })
  const pending = api.loadReminders()
  state.unmount()
  finish([reminder])
  await pending
  assert.equal(api.currentTriggeredItem.value, null)
})

test('another tab’s acknowledgement dismisses the same occurrence', async () => {
  const { state, storage, api } = harness()
  state.reminders = [reminder]
  await api.loadReminders()
  const occurrence = api.currentTriggeredItem.value.occurrenceKey
  storage.set(utility.reminderAcknowledgementStorageKey(1), JSON.stringify({ [occurrence]: 'confirmed' }))
  api.handleStorageChange()
  assert.equal(api.currentTriggeredItem.value, null)
})
