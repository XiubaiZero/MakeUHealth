import assert from 'node:assert/strict'
import test from 'node:test'
import { createTypeScriptLoader } from './helpers/loadTypescript.mjs'

const goalTypeOptions = [
  { value: 'muscle_gain', label: 'Muscle gain' },
  { value: 'weight_loss', label: 'Weight loss' },
  { value: 'fat_loss', label: 'Fat loss' },
]
const contextUrl = new URL('../src/features/assistant/context.ts', import.meta.url)

function fixture() {
  return {
    profile: { id: 1, age: 30, gender: 'male', height: 175, weight: 75 },
    latestRecord: { systolic: 120, diastolic: 80, fbg: 5, heartRate: 65, oxyhemoglobin: 98, recordedAt: '2026-10-02' },
    last7DaysFoodCount: 3,
    selectedGoalType: 'muscle_gain', selectedGoalLabel: 'Muscle gain', goalTypeOptions,
    fitnessDashboards: {
      muscle_gain: { activeGoal: { goalType: 'muscle_gain', currentValue: 0, targetValue: 5, weeklyChange: 0, targetDate: '2026-12-01', status: 'active' }, latestProgress: { currentValue: 0, progressPercentage: 0 }, remainingWeeks: 0 },
      weight_loss: { activeGoal: { goalType: 'weight_loss', currentValue: 75, targetValue: 70 } },
      fat_loss: {},
    },
  }
}

test('context includes the existing fields, all goal snapshots, zero values and null placeholders', () => {
  const { buildAssistantContext } = createTypeScriptLoader()(contextUrl)
  const input = fixture()
  const original = structuredClone(input)
  const result = buildAssistantContext(input)
  assert.equal(result.hasProfile, true)
  assert.equal(result.latestSystolic, 120)
  assert.equal(result.latestRecordDate, '2026-10-02')
  assert.equal(result.last7DaysFoodCount, 3)
  assert.equal(result.activeGoalCurrentValue, 0)
  assert.equal(result.latestProgressPercentage, 0)
  assert.equal(result.remainingGoalWeeks, 0)
  assert.deepEqual(result.allGoalSnapshots.map(item => item.goalType), ['muscle_gain', 'weight_loss', 'fat_loss'])
  assert.equal(result.allGoalSnapshots[0].currentValue, 0)
  assert.equal(result.allGoalSnapshots[2].currentValue, null)
  assert.deepEqual(input, original)
})

test('goal switching and different users create independent context snapshots', () => {
  const { buildAssistantContext } = createTypeScriptLoader()(contextUrl)
  const input = fixture()
  const muscle = buildAssistantContext(input)
  const weight = buildAssistantContext({ ...input, selectedGoalType: 'weight_loss', selectedGoalLabel: 'Weight loss' })
  const other = buildAssistantContext({ ...input, profile: { ...input.profile, age: 60 } })
  assert.equal(weight.selectedGoalType, 'weight_loss')
  assert.equal(weight.selectedGoalLabel, 'Weight loss')
  assert.equal(weight.activeGoalTargetValue, 70)
  assert.equal(muscle.activeGoalTargetValue, 5)
  assert.equal(other.age, 60)
  assert.equal(muscle.age, 30)
  assert.notEqual(muscle.allGoalSnapshots, other.allGoalSnapshots)
})

test('missing profile, record and goals preserve undefined and null values', () => {
  const { buildAssistantContext } = createTypeScriptLoader()(contextUrl)
  const result = buildAssistantContext({ ...fixture(), profile: null, latestRecord: null, fitnessDashboards: { muscle_gain: {}, weight_loss: {}, fat_loss: {} } })
  assert.equal(result.hasProfile, false)
  assert.equal(result.age, undefined)
  assert.equal(result.latestRecordDate, undefined)
  assert.equal(result.activeGoalType, undefined)
  assert.ok(result.allGoalSnapshots.every(item => item.status === null && item.latestProgressValue === null))
})

test('record selection sorts a copy and food counting preserves the original seven-day boundary', () => {
  const { findLatestHealthRecord, countRecentFoodIntakes } = createTypeScriptLoader()(contextUrl)
  const records = [{ recordedAt: '2026-09-30' }, { recordedAt: '2026-10-02' }, {}]
  assert.equal(findLatestHealthRecord(records), records[1])
  assert.equal(records[0].recordedAt, '2026-09-30')
  assert.equal(findLatestHealthRecord([]), null)
  const now = Date.parse('2026-10-02T00:00:00Z')
  const count = countRecentFoodIntakes([
    { intakeTime: '2026-09-25T00:00:00Z' },
    { intakeTime: '2026-09-24T23:59:59Z' },
    { intakeTime: '2026-10-03T00:00:00Z' },
    { intakeTime: 'invalid' }, {},
  ], now)
  assert.equal(count, 2)
})

test('the real context and request modules compose into the existing API body and fallback', async () => {
  const sent = []
  let unavailable = false
  const load = createTypeScriptLoader({ mocks: new Map([
    [new URL('../src/api/client.ts', import.meta.url), { default: { post: async (...args) => { if (unavailable) throw new Error('offline'); sent.push(args); return { data: { answer: 'saved-data advice' } } } } }],
    [new URL('../src/i18n/index.ts', import.meta.url), { locale: { value: 'en' }, t: value => value }],
  ]) })
  const context = load(contextUrl).buildAssistantContext(fixture())
  const { requestAssistantReply } = load(new URL('../src/api/assistant.ts', import.meta.url))
  assert.equal(await requestAssistantReply('fitness plan', context), 'saved-data advice')
  assert.equal(sent[0][0], '/assistant/chat')
  assert.deepEqual(sent[0][1].context, context)
  assert.equal(sent[0][1].constraints.length, 5)
  assert.equal(sent[0][2].timeout, 60000)
  unavailable = true
  const fallback = await requestAssistantReply('fitness plan', context)
  assert.match(fallback, /age 30/)
  assert.match(fallback, /muscle gain/)
  assert.match(fallback, /local fallback/)
})
