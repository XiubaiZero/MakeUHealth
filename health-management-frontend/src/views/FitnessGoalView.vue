<template>
  <div class="goal-page">
    <section v-if="!hasProfile" class="profile-required-card">
      <h2>{{ t("Complete your personal profile first") }}</h2>
      <p>{{ t("Before creating or tracking fitness goals, please fill in your personal profile details.") }}</p>
      <div class="profile-required-actions">
        <button class="primary-button" type="button" @click="goToProfileOnboarding"> {{ t("Complete profile") }} </button>
        <button class="secondary-button" type="button" @click="goToHealthEntry">{{ t("Go to profile entry") }}</button>
      </div>
    </section>

    <section class="hero">
      <div>
        <p class="eyebrow">{{ t("Fitness goals") }}</p>
        <h1>{{ t("Track one active goal type at a time") }}</h1>
        <p class="hero-copy"> {{ t("Track weekly changes for weight loss, fat loss, and muscle gain goals linked to your account.") }} </p>
      </div>
      <div class="goal-switches">
        <button
          v-for="item in goalTypes"
          :key="item.value"
          class="type-button"
          :class="{ active: goalForm.goalType === item.value }"
          type="button"
          @click="switchGoalType(item.value)"
        >
          <span>{{ t(item.icon) }}</span>
          <strong>{{ t(item.label) }}</strong>
        </button>
      </div>
    </section>

    <p v-if="statusMessage" class="status-banner">{{ t(statusMessage) }}</p>

    <div class="layout">
      <section class="stack">
        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Create goal") }}</p>
              <h2>{{ t(currentGoalType.label) }}</h2>
            </div>
          </div>

          <form class="form-grid" @submit.prevent="handleCreateGoal">
            <label>
              <span>{{ t(currentGoalType.currentLabel) }}</span>
              <input
                v-model.number="goalForm.currentValue"
                min="0"
                step="0.1"
                type="number"
                :placeholder="t('current value')"
                required
              />
            </label>
            <label>
              <span>{{ t(currentGoalType.targetLabel) }}</span>
              <input
                v-model.number="goalForm.targetValue"
                min="0"
                step="0.1"
                type="number"
                :placeholder="t('target value')"
                required
              />
            </label>
            <label class="full-width">
              <span>{{ t("Target date") }}</span>
              <CustomDatePicker v-model="goalForm.targetDate" :min="minDate" />
            </label>

            <div v-if="preview.totalWeeks > 0" class="preview full-width">
              <article>
                <span>{{ t("Total weeks") }}</span>
                <strong>{{ t(preview.totalWeeks) }}</strong>
              </article>
              <article>
                <span>{{ t("Weekly change") }}</span>
                <strong>{{ t(formatNumber(preview.weeklyChange)) }} {{ t(currentGoalType.unit) }}</strong>
              </article>
              <article>
                <span>{{ t("Total change") }}</span>
                <strong>{{ t(formatNumber(preview.totalChange)) }} {{ t(currentGoalType.unit) }}</strong>
              </article>
            </div>

            <div class="form-actions full-width">
              <button class="primary-button" type="submit" :disabled="busy.create || !canCreateGoal">
                {{ t(busy.create ? 'Creating...' : 'Create goal') }}
              </button>
              <button class="secondary-button" type="button" @click="resetGoalForm">{{ t("Reset") }}</button>
            </div>
          </form>
        </article>

        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Record progress") }}</p>
              <h2>{{ t(activeGoal ? 'Update this week' : 'Waiting for a goal') }}</h2>
            </div>
          </div>

          <div v-if="!activeGoal" class="empty-state"> {{ t("Create a") }} {{ t(currentGoalType.label.toLowerCase()) }} {{ t("goal first.") }} </div>

          <form v-else class="form-grid" @submit.prevent="handleSaveProgress">
            <label class="full-width">
              <span>{{ t(currentGoalType.progressLabel) }}</span>
              <input v-model.number="progressValue" min="0" step="0.1" type="number" required />
            </label>

            <div class="form-actions full-width">
              <button class="primary-button" type="submit" :disabled="busy.progress">
                {{ t(busy.progress ? 'Saving...' : progressButtonLabel) }}
              </button>
              <button class="secondary-button" type="button" @click="resetProgressInput">{{ t("Clear") }}</button>
            </div>
          </form>
        </article>
      </section>

      <aside class="stack">
        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Dashboard") }}</p>
              <h2>{{ t("Active goal") }}</h2>
            </div>
            <button class="ghost-button" type="button" @click="loadGoalData">{{ t("Refresh") }}</button>
          </div>

          <div v-if="!activeGoal" class="empty-state">{{ t("No active goal for this type.") }}</div>
          <div v-else class="dashboard">
            <div class="goal-banner">
              <span>{{ t(currentGoalType.label) }}</span>
              <strong>
                {{ t(formatNumber(activeGoal.currentValue)) }} -> {{ t(formatNumber(activeGoal.targetValue)) }} {{ t(currentGoalType.unit) }}
              </strong>
              <small>{{ t("Target date:") }} {{ t(formatDate(activeGoal.targetDate)) }}</small>
            </div>

            <div class="metric-grid">
              <article>
                <span>{{ t("Status") }}</span>
                <strong>{{ t(activeGoal.status || 'active') }}</strong>
              </article>
              <article>
                <span>{{ t("Weekly target") }}</span>
                <strong>{{ t(formatNumber(activeGoal.weeklyChange)) }}</strong>
              </article>
              <article>
                <span>{{ t("Remaining weeks") }}</span>
                <strong>{{ t(dashboard.remainingWeeks ?? 0) }}</strong>
              </article>
              <article>
                <span>{{ t("Current reading") }}</span>
                <strong>{{ t(formatNumber(currentDisplayedValue)) }}</strong>
              </article>
            </div>

            <div class="progress-wrap">
              <div class="progress-head">
                <span>{{ t("Overall progress") }}</span>
                <strong>{{ t(progressPercent) }}%</strong>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div>
              </div>
            </div>
          </div>
        </article>

        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("History") }}</p>
              <h2>{{ t("Weekly progress") }}</h2>
            </div>
          </div>

          <div v-if="busy.history" class="empty-state">{{ t("Loading progress history...") }}</div>
          <div v-else-if="history.length === 0" class="empty-state">{{ t("No progress records yet.") }}</div>
          <div v-else class="history-list">
            <article v-for="item in history" :key="item.id" class="history-item">
              <header>
                <strong>{{ t(formatWeek(item.weekStart, item.weekEnd)) }}</strong>
                <span :class="item.isOnTrack ? 'good' : 'warn'">
                  {{ t(item.isOnTrack ? 'On track' : 'Needs attention') }}
                </span>
              </header>
              <p>{{ t("Current value:") }} {{ t(formatNumber(item.currentValue)) }} {{ t(currentGoalType.unit) }}</p>
              <p>{{ t("Weekly change:") }} {{ t(formatNumber(item.weeklyChange)) }} {{ t(currentGoalType.unit) }}</p>
              <p>{{ t("Progress:") }} {{ t(formatNumber(item.progressPercentage)) }}%</p>
            </article>
          </div>
        </article>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { t, dateLocale } from '../i18n'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import CustomDatePicker from '../components/CustomDatePicker.vue'
import {
  calculateGoal,
  createGoal,
  getDashboardDataByType,
  getProgressByGoalType,
  recordProgress,
  updateThisWeekProgress,
} from '../api/fitnessGoals'
import type { FitnessGoal, GoalDashboardData, WeeklyProgress } from '../api/types'
import { getUsers } from '../api/users'
import { getAccountId } from '../utils/auth'
import { setProfileComplete, setProfileOnboardingSkipped } from '../utils/profileOnboarding'

type GoalTypeValue = 'weight_loss' | 'muscle_gain' | 'fat_loss'

const goalTypes = [
  {
    value: 'weight_loss' as GoalTypeValue,
    label: 'Weight loss',
    icon: 'WL',
    unit: 'kg',
    currentLabel: 'Current weight (kg)',
    targetLabel: 'Target weight (kg)',
    progressLabel: 'Current weight this week (kg)',
  },
  {
    value: 'muscle_gain' as GoalTypeValue,
    label: 'Muscle gain',
    icon: 'MG',
    unit: 'kg',
    currentLabel: 'Current muscle mass (kg)',
    targetLabel: 'Target muscle mass (kg)',
    progressLabel: 'Current muscle mass this week (kg)',
  },
  {
    value: 'fat_loss' as GoalTypeValue,
    label: 'Fat loss',
    icon: 'FL',
    unit: '%',
    currentLabel: 'Current body fat (%)',
    targetLabel: 'Target body fat (%)',
    progressLabel: 'Current body fat this week (%)',
  },
]

const defaultGoalType = goalTypes[0]!
const route = useRoute()
const router = useRouter()

const busy = reactive({
  create: false,
  progress: false,
  history: false,
})

const goalForm = reactive({
  goalType: 'weight_loss' as GoalTypeValue,
  currentValue: null as number | null,
  targetValue: null as number | null,
  targetDate: '',
})

const preview = reactive({
  totalWeeks: 0,
  weeklyChange: 0,
  totalChange: 0,
})

const dashboard = ref<GoalDashboardData>({})
const history = ref<WeeklyProgress[]>([])
const progressValue = ref<number | null>(null)
const statusMessage = ref('')
const hasProfile = ref(true)

const currentGoalType = computed(() => {
  return goalTypes.find((item) => item.value === goalForm.goalType) ?? defaultGoalType
})

const activeGoal = computed<FitnessGoal | null>(() => dashboard.value.activeGoal ?? null)
const currentDisplayedValue = computed(() => {
  if (dashboard.value.latestProgress?.currentValue !== undefined) {
    return dashboard.value.latestProgress.currentValue
  }
  return activeGoal.value?.currentValue ?? 0
})

const progressButtonLabel = computed(() =>
  dashboard.value.thisWeekProgress ? 'Update this week progress' : 'Record this week progress',
)

const canCreateGoal = computed(
  () => {
    const current = goalForm.currentValue
    const target = goalForm.targetValue
    return (
      current !== null &&
      target !== null &&
      Number.isFinite(Number(current)) &&
      Number.isFinite(Number(target)) &&
      Number(current) >= 0 &&
      Number(target) >= 0 &&
      Boolean(goalForm.targetDate) &&
      goalForm.targetDate >= minDate.value
    )
  },
)

const minDate = computed(() => {
  const date = new Date()
  date.setDate(date.getDate() + 7)
  return date.toISOString().split('T')[0] ?? ''
})

const progressPercent = computed(() => {
  if (!activeGoal.value) {
    return 0
  }

  const start = Number(activeGoal.value.currentValue)
  const target = Number(activeGoal.value.targetValue)
  const current = Number(currentDisplayedValue.value)
  const totalChange = target - start

  if (totalChange === 0) {
    return 100
  }

  const currentChange = current - start
  const percent = (currentChange / totalChange) * 100
  return Math.max(0, Math.min(100, Number(percent.toFixed(2))))
})

function setStatus(message: string) {
  statusMessage.value = message
  window.setTimeout(() => {
    if (statusMessage.value === message) {
      statusMessage.value = ''
    }
  }, 3000)
}

function formatNumber(value?: number | null) {
  if (value === undefined || value === null) {
    return '0.00'
  }

  return Number(value).toFixed(2)
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat(dateLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

function formatWeek(start?: string, end?: string) {
  if (!start || !end) {
    return '-'
  }

  const startDate = new Date(start)
  const endDate = new Date(end)
  const formatter = new Intl.DateTimeFormat(dateLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
  return `${formatter.format(startDate)} - ${formatter.format(endDate)}`
}

function resetGoalForm() {
  goalForm.currentValue = null
  goalForm.targetValue = null
  goalForm.targetDate = ''
  preview.totalWeeks = 0
  preview.weeklyChange = 0
  preview.totalChange = 0
}

function resetProgressInput() {
  progressValue.value = null
}

function switchGoalType(goalType: GoalTypeValue) {
  goalForm.goalType = goalType
}

async function goToProfileOnboarding() {
  const accountId = getAccountId()
  setProfileComplete(accountId, false)
  setProfileOnboardingSkipped(accountId, false)
  await router.push({
    name: 'profile-onboarding',
    query: { redirect: '/fitness-goals' },
  })
}

async function goToHealthEntry() {
  await router.push({ name: 'health-entry' })
}

function parseGoalTypeQuery(value: unknown): GoalTypeValue | null {
  if (typeof value !== 'string') {
    return null
  }
  return goalTypes.some((item) => item.value === value) ? (value as GoalTypeValue) : null
}

async function loadPreview() {
  if (!canCreateGoal.value) {
    preview.totalWeeks = 0
    preview.weeklyChange = 0
    preview.totalChange = 0
    return
  }

  try {
    const result = await calculateGoal({
      currentValue: Number(goalForm.currentValue),
      targetValue: Number(goalForm.targetValue),
      targetDate: goalForm.targetDate,
    })

    preview.totalWeeks = result.totalWeeks
    preview.weeklyChange = result.weeklyChange
    preview.totalChange = result.totalChange
  } catch {
    preview.totalWeeks = 0
    preview.weeklyChange = 0
    preview.totalChange = 0
  }
}

async function loadGoalData() {
  if (!hasProfile.value) {
    dashboard.value = {}
    history.value = []
    progressValue.value = null
    return
  }

  busy.history = true
  try {
    const [dashboardData, historyData] = await Promise.all([
      getDashboardDataByType(goalForm.goalType),
      getProgressByGoalType(goalForm.goalType),
    ])

    dashboard.value = dashboardData
    history.value = historyData
    progressValue.value = dashboardData.thisWeekProgress?.currentValue ?? null
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to load fitness goal data')
  } finally {
    busy.history = false
  }
}

async function handleCreateGoal() {
  if (!hasProfile.value) {
    setStatus('Please complete your personal profile first.')
    return
  }

  busy.create = true
  try {
    await createGoal({
      goalType: goalForm.goalType,
      currentValue: Number(goalForm.currentValue),
      targetValue: Number(goalForm.targetValue),
      targetDate: goalForm.targetDate,
    })
    await loadGoalData()
    setStatus('Goal created successfully.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to create goal')
  } finally {
    busy.create = false
  }
}

async function handleSaveProgress() {
  if (!hasProfile.value) {
    setStatus('Please complete your personal profile first.')
    return
  }

  if (!activeGoal.value?.id || progressValue.value === null) {
    setStatus('Create a goal and enter a progress value first.')
    return
  }

  busy.progress = true
  try {
    if (dashboard.value.thisWeekProgress) {
      await updateThisWeekProgress(activeGoal.value.id, Number(progressValue.value))
    } else {
      await recordProgress(activeGoal.value.id, Number(progressValue.value))
    }

    await loadGoalData()
    setStatus('Progress saved successfully.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to save progress')
  } finally {
    busy.progress = false
  }
}

watch(
  () => [goalForm.currentValue, goalForm.targetValue, goalForm.targetDate],
  () => {
    void loadPreview()
  },
)

watch(
  () => goalForm.goalType,
  () => {
    resetGoalForm()
    void loadGoalData()
  },
)

onMounted(async () => {
  const goalTypeFromQuery = parseGoalTypeQuery(route.query.goalType)
  if (goalTypeFromQuery) {
    goalForm.goalType = goalTypeFromQuery
  }

  try {
    const users = await getUsers()
    hasProfile.value = users.length > 0
  } catch {
    hasProfile.value = false
  }
  await loadGoalData()
})

watch(
  () => route.query.goalType,
  (value) => {
    const goalType = parseGoalTypeQuery(value)
    if (goalType && goalType !== goalForm.goalType) {
      goalForm.goalType = goalType
    }
  },
)
</script>

<style scoped>
.goal-page {
  display: grid;
  gap: 1.5rem;
}

.profile-required-card {
  padding: 1.25rem;
  border-radius: 22px;
  border: 1px solid #f1d57a;
  background: #fff6d6;
  color: #6a5100;
  display: grid;
  gap: 0.8rem;
}

.profile-required-card h2,
.profile-required-card p {
  margin: 0;
}

.profile-required-actions {
  display: flex;
  gap: 0.75rem;
}

.hero {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 24px;
  background:
    radial-gradient(circle at right top, rgba(223, 163, 91, 0.28), transparent 30%),
    linear-gradient(135deg, #f6efe2 0%, #e5eadf 45%, #d6e7ef 100%);
  border: 1px solid rgba(18, 52, 59, 0.08);
}

.eyebrow {
  margin: 0 0 0.35rem;
  color: #8a5a44;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.75rem;
  font-weight: 700;
}

.hero h1,
.card h2 {
  margin: 0;
  color: #12343b;
}

.hero-copy {
  margin: 0.8rem 0 0;
  max-width: 44rem;
  color: #30565a;
  line-height: 1.65;
}

.goal-switches {
  display: grid;
  gap: 0.75rem;
}

.type-button {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1rem 1.1rem;
  border-radius: 18px;
  border: 1px solid #d5e0d6;
  background: rgba(255, 255, 255, 0.75);
  color: #234b52;
  cursor: pointer;
  text-align: left;
  font: inherit;
}

.type-button.active {
  background: #e5f2ef;
  border-color: #1f7a8c;
}

.status-banner {
  margin: 0;
  padding: 0.85rem 1rem;
  border-radius: 14px;
  background: #fff6d6;
  color: #7d5a00;
  border: 1px solid #f1d57a;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 1fr);
  gap: 1.5rem;
}

.stack {
  display: grid;
  gap: 1rem;
}

.card {
  padding: 1.25rem;
  border-radius: 22px;
  background: #fffdf8;
  border: 1px solid #e1e7df;
  box-shadow: 0 18px 40px rgba(20, 55, 60, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.form-grid label {
  display: grid;
  gap: 0.45rem;
}

.form-grid span {
  color: #34565d;
  font-size: 0.92rem;
  font-weight: 600;
}

.form-grid input {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #ffffff;
  color: #173f46;
  font: inherit;
}

.full-width {
  grid-column: 1 / -1;
}

.preview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.preview article,
.metric-grid article {
  display: grid;
  gap: 0.35rem;
  padding: 1rem;
  border-radius: 16px;
  background: #f3f7f4;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
}

.primary-button,
.secondary-button,
.ghost-button {
  padding: 0.85rem 1.1rem;
  border: 0;
  border-radius: 14px;
  font: inherit;
  cursor: pointer;
}

.primary-button {
  background: linear-gradient(135deg, #1f7a8c, #3aa17e);
  color: #fff;
}

.secondary-button {
  background: #efe6d9;
  color: #5d4636;
}

.ghost-button {
  background: #edf5f4;
  color: #24555f;
}

.primary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.empty-state {
  padding: 1.15rem;
  border-radius: 16px;
  background: #f4f7f5;
  color: #698186;
  text-align: center;
}

.dashboard {
  display: grid;
  gap: 1rem;
}

.goal-banner {
  display: grid;
  gap: 0.35rem;
  padding: 1.1rem;
  border-radius: 18px;
  background: linear-gradient(135deg, #1f7a8c, #4d8c75);
  color: #fff;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.progress-wrap {
  display: grid;
  gap: 0.5rem;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  color: #30565a;
  font-weight: 600;
}

.progress-bar {
  height: 12px;
  border-radius: 999px;
  background: #dce7e3;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #1f7a8c, #3aa17e);
}

.history-list {
  display: grid;
  gap: 0.75rem;
  max-height: 520px;
  overflow: auto;
}

.history-item {
  padding: 0.95rem 1rem;
  border-radius: 16px;
  background: #f7faf8;
  border: 1px solid #dde8e2;
}

.history-item header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.45rem;
}

.history-item p {
  margin: 0.25rem 0 0;
  color: #587176;
}

.good {
  color: #2f855a;
}

.warn {
  color: #c05621;
}

@media (max-width: 1024px) {
  .hero,
  .layout {
    grid-template-columns: 1fr;
  }

  .profile-required-actions {
    flex-direction: column;
  }
}

@media (max-width: 720px) {
  .form-grid,
  .preview,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .card-header,
  .history-item header,
  .form-actions {
    flex-direction: column;
  }
}
</style>
