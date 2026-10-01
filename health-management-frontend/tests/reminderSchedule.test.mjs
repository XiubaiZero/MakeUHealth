import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import ts from 'typescript'

// Compile the pure TS utility with the project's existing compiler; no test dependency needed.
const source = readFileSync(new URL('../src/utils/reminderSchedule.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } })
const { getDueOccurrenceTime: due, reminderAcknowledgementStorageKey: key } =
  await import('data:text/javascript;base64,' + Buffer.from(compiled.outputText).toString('base64'))
const schedule = (time, repeat = 'none', enabled = true) => ({ reminderTime: time, repeatPattern: repeat, enabled })
const expectDue = (reminder, now, expected) => assert.equal(due(reminder, new Date(now))?.getTime(), new Date(expected).getTime())

test('one-time reminders wait until due, then remain due until acknowledged', () => {
  const reminder = schedule('2026-10-01T10:00:00')
  assert.equal(due(reminder, new Date('2026-10-01T09:59:59')), null)
  expectDue(reminder, '2026-10-02T11:00:00', '2026-10-01T10:00:00')
})
test('disabled, invalid and unsupported schedules do not trigger', () => {
  for (const reminder of [schedule('bad'), schedule('2026-01-01', 'none', false), schedule('2026-01-01', 'yearly')]) {
    assert.equal(due(reminder, new Date('2026-10-01T12:00:00')), null)
  }
})
test('daily reminders catch up once before today’s scheduled time', () => {
  expectDue(schedule('2026-09-01T10:00:00', 'daily'), '2026-10-01T09:00:00', '2026-09-30T10:00:00')
  expectDue(schedule('2026-09-01T10:00:00', 'daily'), '2026-10-01T10:00:00', '2026-10-01T10:00:00')
})
test('weekly reminders retain the original weekday and catch up the previous occurrence', () => {
  expectDue(schedule('2026-09-24T10:00:00', 'weekly'), '2026-10-01T09:00:00', '2026-09-24T10:00:00')
  expectDue(schedule('2026-09-24T10:00:00', 'weekly'), '2026-10-08T12:00:00', '2026-10-08T10:00:00')
})
test('monthly reminders clamp to February then restore the original day in March', () => {
  const reminder = schedule('2026-01-31T10:00:00', 'monthly')
  expectDue(reminder, '2026-02-28T10:00:00', '2026-02-28T10:00:00')
  expectDue(reminder, '2026-03-01T12:00:00', '2026-02-28T10:00:00')
  expectDue(reminder, '2026-03-31T10:00:00', '2026-03-31T10:00:00')
})
test('monthly recurrence supports leap years and year boundaries', () => {
  expectDue(schedule('2027-12-31T10:00:00', 'monthly'), '2028-02-29T10:00:00', '2028-02-29T10:00:00')
  expectDue(schedule('2026-12-31T10:00:00', 'monthly'), '2027-01-31T09:00:00', '2026-12-31T10:00:00')
})
test('daily recurrence follows local calendar dates across daylight-saving changes', () => {
  const previous = process.env.TZ
  process.env.TZ = 'America/New_York'
  try {
    expectDue(schedule('2026-03-07T09:00:00', 'daily'), '2026-03-09T09:00:00', '2026-03-09T09:00:00')
    expectDue(schedule('2026-10-31T09:00:00', 'daily'), '2026-11-02T09:00:00', '2026-11-02T09:00:00')
  } finally {
    if (previous === undefined) delete process.env.TZ
    else process.env.TZ = previous
  }
})
test('acknowledgements are isolated by account and stable across refreshes', () => {
  assert.notEqual(key(1), key(2))
  const storage = new Map([[key(1), JSON.stringify({ '9:2026-10-01T02:00:00.000Z': 'confirmed' })]])
  assert.equal(JSON.parse(storage.get(key(1)))['9:2026-10-01T02:00:00.000Z'], 'confirmed')
  assert.equal(storage.get(key(2)), undefined)
})
