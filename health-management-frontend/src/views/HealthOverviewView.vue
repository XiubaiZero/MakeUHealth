<template>
  <div class="overview-page">
    <section class="hero-band">
      <div>
        <p class="eyebrow">{{ t("Overview") }}</p>
        <h1>{{ t("Health dashboard") }}</h1>
        <p class="hero-copy"> {{ t("View your profile, body health, diet intake, fitness plans, and reminders in one place.") }} </p>
      </div>
      <button class="ghost-button" type="button" :disabled="busy.overview" @click="refreshAllData">
        {{ t(busy.overview ? 'Refreshing...' : 'Refresh data') }}
      </button>
    </section>

    <p v-if="statusMessage" class="status-banner">{{ t(statusMessage) }}</p>
    <div v-if="showCompleteProfileReminder" class="profile-reminder">
      <p>{{ t("Your profile is not complete yet. Please add your personal information") }}</p>
      <button class="primary-button" type="button" @click="goToProfileOnboarding">{{ t("Complete now") }}</button>
    </div>

    <section v-if="selectedUser" class="profile-summary-strip">
      <p v-if="selectedRecord && selectedRecordDisplayIndex !== null" class="profile-summary-context"> {{ t("Synced to Record") }} {{ t(selectedRecordDisplayIndex) }} ({{ t(formatDate(selectedRecord.recordedAt)) }})
      </p>
      <article v-for="item in profileSummaryItems" :key="item.key" class="profile-summary-card">
        <div class="profile-summary-icon-wrap">
          <img
            :src="item.icon"
            :alt="`${item.label} icon`"
            class="profile-summary-icon"
            :class="{ 'is-gender': item.key === 'gender' }"
          />
        </div>
        <p class="profile-summary-label">{{ t(item.label) }}</p>
        <p class="profile-summary-value">
          {{ t(item.value) }}
          <span v-if="item.unit">{{ t(item.unit) }}</span>
        </p>
      </article>
    </section>

    <section class="top-grid">
      <article class="card">
        <div class="card-header">
          <div>
            <p class="eyebrow">{{ t("HEALTH") }}</p>
            <h2>{{ t("Profile & recent records") }}</h2>
          </div>
        </div>

        <div v-if="busy.overview && !selectedUser" class="empty-state">{{ t("Loading profile...") }}</div>
        <div v-else-if="!selectedUser" class="empty-state">{{ t("Your profile is not complete yet. Please add your personal information") }}</div>
        <div v-else-if="busy.records" class="empty-state">{{ t("Loading records...") }}</div>
        <div v-else class="timeline">
          <template v-if="healthRecords.length === 0">
            <article class="timeline-item combined-item baseline-item">
              <header>
                <strong>{{ t("Profile baseline") }}</strong>
                <span>{{ t(selectedUser.createdAt ? formatDate(selectedUser.createdAt) : 'New profile') }}</span>
              </header>
              <div class="combined-grid">
                <section class="combined-block">
                  <h3>{{ t("Profile") }}</h3>
                  <p>{{ t("Age:") }} {{ t(displayValue(selectedUser.age)) }} {{ t("yrs") }}</p>
                  <p>{{ t("Gender:") }} {{ t(formatGender(selectedUser.gender)) }}</p>
                  <p>{{ t("Height:") }} {{ t(displayValue(selectedUser.height)) }} {{ t("cm") }}</p>
                  <p>{{ t("Weight:") }} {{ t(displayValue(selectedUser.weight)) }} {{ t("kg") }}</p>
                </section>
                <section class="combined-block">
                  <h3>{{ t("Health record") }}</h3>
                  <p>{{ t("No health record yet.") }}</p>
                  <p>{{ t("FBG - mmol/L") }}</p>
                  <p>{{ t("Heart rate - bpm") }}</p>
                  <p>{{ t("Oxyhemoglobin -%") }}</p>
                </section>
              </div>
            </article>
          </template>
          <template v-else>
            <article
              v-for="(record, index) in healthRecords"
              :key="record.id"
              class="timeline-item combined-item record-row"
              :class="{ active: selectedRecordId === record.id }"
              @click="selectRecord(record)"
            >
              <header>
                <strong>{{ t("Record") }} {{ t(index + 1) }}</strong>
                <div class="record-header-actions">
                  <span>{{ t(formatDate(record.recordedAt)) }}</span>
                  <button
                    type="button"
                    class="delete-icon-button"
                    :aria-label="t('Delete record')"
                    @click.stop="requestDeleteRecord(record.id)"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path
                        d="M9 3h6l1 2h4v2H4V5h4l1-2Zm1 7h2v8h-2v-8Zm4 0h2v8h-2v-8ZM7 10h2v8H7v-8Zm-1 10h12l1-13H5l1 13Z"
                      />
                    </svg>
                  </button>
                </div>
              </header>
              <div class="combined-grid">
                <section class="combined-block">
                  <h3>{{ t("Profile") }}</h3>
                  <p>{{ t("Age:") }} {{ t(displayValue(resolveProfileSnapshot(record, selectedUser).age)) }} {{ t("yrs") }}</p>
                  <p>{{ t("Gender:") }} {{ t(formatGender(resolveProfileSnapshot(record, selectedUser).gender)) }}</p>
                  <p>{{ t("Height:") }} {{ t(displayValue(resolveProfileSnapshot(record, selectedUser).height)) }} {{ t("cm") }}</p>
                  <p>{{ t("Weight:") }} {{ t(displayValue(resolveProfileSnapshot(record, selectedUser).weight)) }} {{ t("kg") }}</p>
                </section>
                <section class="combined-block">
                  <h3>{{ t("Health record") }}</h3>
                  <p>{{ t("BP") }} {{ t(record.systolic) }}/{{ t(record.diastolic) }} {{ t("mmHg") }}</p>
                  <p>{{ t("FBG") }} {{ t(displayValue(record.fbg)) }} {{ t("mmol/L") }}</p>
                  <p>{{ t("Heart rate") }} {{ t(displayValue(record.heartRate)) }} {{ t("bpm") }}</p>
                  <p>{{ t("Oxyhemoglobin") }} {{ t(displayValue(record.oxyhemoglobin)) }}%</p>
                </section>
              </div>
            </article>
          </template>
        </div>
      </article>

      <article class="card">
        <div class="card-header">
          <div>
            <p class="eyebrow">{{ t("Fitness plans") }}</p>
            <h2>{{ t("Goal snapshot") }}</h2>
          </div>
        </div>

        <div v-if="busy.goals" class="empty-state">{{ t("Loading goals...") }}</div>
        <div v-else class="stack-list">
          <article
            v-for="goal in goalCards"
            :key="goal.type"
            class="list-item goal-item"
            role="button"
            tabindex="0"
            @click="goToGoal(goal.type)"
            @keydown.enter.prevent="goToGoal(goal.type)"
            @keydown.space.prevent="goToGoal(goal.type)"
          >
            <header>
              <strong>{{ t(goal.label) }}</strong>
              <span>{{ t(goal.activeGoal?.status || 'inactive') }}</span>
            </header>
            <template v-if="goal.activeGoal">
              <p>{{ t("Current:") }} {{ t(formatNumber(goal.activeGoal.currentValue)) }}</p>
              <p>{{ t("Target:") }} {{ t(formatNumber(goal.activeGoal.targetValue)) }}</p>
              <p>{{ t("Target date:") }} {{ t(formatDate(goal.activeGoal.targetDate)) }}</p>
              <p> {{ t("Latest progress:") }} {{ t(goal.latestProgress ? `${formatNumber(goal.latestProgress.progressPercentage)}%` : '-%') }}
              </p>
            </template>
            <p v-else>{{ t("No active goal.") }}</p>
          </article>
        </div>
      </article>
    </section>

    <section class="bottom-grid">
      <div class="left-stack">
        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Nutrition") }}</p>
              <h2>{{ t("Summary") }}</h2>
            </div>
            <select v-model="statsType" :aria-label="t('Nutrition summary period')">
              <option value="day">{{ t("day") }}</option>
              <option value="week">{{ t("week") }}</option>
              <option value="month">{{ t("month") }}</option>
            </select>
          </div>

          <div ref="chartRef" class="chart"></div>

          <div v-if="busy.stats" class="empty-state compact">{{ t("Loading nutrition stats...") }}</div>
          <div v-else-if="nutritionStats.length === 0" class="empty-state compact">{{ t("No nutrition stats yet.") }}</div>
          <div v-else class="timeline compact-list">
            <article
              v-for="item in nutritionStats"
              :key="item.period"
              class="timeline-item compact-item summary-row"
              :class="{ active: selectedSummaryPeriod === item.period }"
              @click="selectSummaryStatItem(item)"
            >
              <header>
                <strong>{{ t(formatStatsPeriod(item.period)) }}</strong>
                <span>{{ t(item.calories) }} {{ t("kcal") }}</span>
              </header>
              <p>{{ item.nutrients || 'No nutrients info' }}</p>
            </article>
          </div>
        </article>

        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Food history") }}</p>
              <h2>{{ t("Latest intake logs") }}</h2>
            </div>
          </div>

          <div v-if="busy.intakes" class="empty-state">{{ t("Loading intake logs...") }}</div>
          <div v-else-if="foodIntakes.length === 0" class="empty-state">{{ t("No food intake records yet.") }}</div>
          <div v-else class="timeline compact-list logs-list">
            <article
              v-for="item in foodIntakes"
              :key="item.id"
              class="timeline-item compact-item intake-row"
              :class="{ active: selectedIntakeId === item.id }"
              @click="selectIntakeLog(item)"
            >
              <header>
                <strong>{{ item.foodName }}</strong>
                <div class="intake-header-actions">
                  <span>{{ t(item.amount) }} {{ t(item.unit || 'g') }}</span>
                  <button
                    type="button"
                    class="delete-icon-button"
                    :aria-label="t('Delete intake log')"
                    @click.stop="requestDeleteIntake(item.id)"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path
                        d="M9 3h6l1 2h4v2H4V5h4l1-2Zm1 7h2v8h-2v-8Zm4 0h2v8h-2v-8ZM7 10h2v8H7v-8Zm-1 10h12l1-13H5l1 13Z"
                      />
                    </svg>
                  </button>
                </div>
              </header>
              <p>{{ t(displayValue(item.calories)) }} {{ t("kcal") }}</p>
              <p>{{ t(formatDateTime(item.intakeTime)) }}</p>
            </article>
          </div>
        </article>
      </div>

      <article class="card">
        <div class="card-header">
          <div>
            <p class="eyebrow">{{ t("Reminders") }}</p>
            <h2>{{ t("Alarm board") }}</h2>
          </div>
        </div>

        <div v-if="busy.reminders" class="empty-state">{{ t("Loading reminders...") }}</div>
        <div v-else-if="activeReminders.length === 0" class="empty-state">{{ t("No active reminders.") }}</div>
        <div v-else class="timeline compact-list reminder-list">
          <article
            v-for="item in activeReminders"
            :key="item.id"
            class="timeline-item compact-item reminder-row"
            role="button"
            tabindex="0"
            @click="goToReminder(item.id)"
            @keydown.enter.prevent="goToReminder(item.id)"
            @keydown.space.prevent="goToReminder(item.id)"
          >
            <header>
              <strong>{{ t(formatReminderType(item.reminderType)) }}</strong>
              <span>{{ t(item.enabled === false ? 'paused' : 'active') }}</span>
            </header>
            <p>{{ t(formatDateTime(item.reminderTime)) }}</p>
            <p>{{ t(item.note || 'No note') }}</p>
          </article>
        </div>
      </article>
    </section>

    <div v-if="showDeleteRecordConfirm" class="confirm-overlay">
      <div class="confirm-modal" @click.stop>
        <p class="confirm-title">{{ t("Delete this health record?") }}</p>
        <p class="confirm-copy">{{ t("This action cannot be undone.") }}</p>
        <div class="confirm-actions">
          <button type="button" class="confirm-cancel-button" :disabled="busy.deletingRecord" @click="cancelDeleteRecord"> {{ t("Cancel") }} </button>
          <button type="button" class="confirm-delete-button" :disabled="busy.deletingRecord" @click="confirmDeleteRecord">
            {{ t(busy.deletingRecord ? 'Deleting...' : 'Confirm') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteIntakeConfirm" class="confirm-overlay">
      <div class="confirm-modal" @click.stop>
        <p class="confirm-title">{{ t("Delete this intake log?") }}</p>
        <p class="confirm-copy">{{ t("This action cannot be undone.") }}</p>
        <div class="confirm-actions">
          <button type="button" class="confirm-cancel-button" :disabled="busy.deletingIntake" @click="cancelDeleteIntake"> {{ t("Cancel") }} </button>
          <button type="button" class="confirm-delete-button" :disabled="busy.deletingIntake" @click="confirmDeleteIntake">
            {{ t(busy.deletingIntake ? 'Deleting...' : 'Confirm') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { t, dateLocale, locale } from '../i18n'
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { deleteFoodIntake, getFoodIntakeByUserId, getNutritionStats } from '../api/food'
import { getDashboardDataByType } from '../api/fitnessGoals'
import { deleteHealthRecord, getHealthRecordsByUserId } from '../api/healthRecords'
import { getMyReminders } from '../api/reminders'
import type { FoodIntake, GoalDashboardData, HealthRecord, NutritionStats, Reminder, User } from '../api/types'
import { getUsers } from '../api/users'
import profileAgeIcon from '../assets/icons/profile/age.jpg'
import profileFemaleIcon from '../assets/icons/profile/female.jpg'
import profileHeightIcon from '../assets/icons/profile/height.jpg'
import profileMaleIcon from '../assets/icons/profile/male.jpg'
import profileWeightIcon from '../assets/icons/profile/weight.jpg'
import { getAccountId } from '../utils/auth'
import { isProfileOnboardingSkipped } from '../utils/profileOnboarding'

type SummaryType = 'day' | 'week' | 'month'
type GoalType = 'weight_loss' | 'muscle_gain' | 'fat_loss'

type GoalCard = GoalDashboardData & {
  type: GoalType
  label: string
}

type BusyState = {
  overview: boolean
  records: boolean
  deletingRecord: boolean
  intakes: boolean
  stats: boolean
  goals: boolean
  reminders: boolean
  deletingIntake: boolean
}

const chartRef = ref<HTMLDivElement | null>(null)
let nutritionChart: echarts.ECharts | null = null
const router = useRouter()
const selectedUser = ref<User | null>(null)
const selectedUserId = ref<number | null>(null)
const statusMessage = ref('')
const healthRecords = ref<HealthRecord[]>([])
const foodIntakes = ref<FoodIntake[]>([])
const nutritionStats = ref<NutritionStats[]>([])
const reminders = ref<Reminder[]>([])
const goalCards = ref<GoalCard[]>([])
const statsType = ref<SummaryType>('week')
const selectedIntakeId = ref<number | null>(null)
const selectedSummaryPeriod = ref<string | null>(null)
const selectedRecordId = ref<number | null>(null)
const showDeleteRecordConfirm = ref(false)
const pendingDeleteRecordId = ref<number | null>(null)
const showDeleteIntakeConfirm = ref(false)
const pendingDeleteIntakeId = ref<number | null>(null)

const busy = reactive<BusyState>({
  overview: false,
  records: false,
  deletingRecord: false,
  intakes: false,
  stats: false,
  goals: false,
  reminders: false,
  deletingIntake: false,
})
const selectedIntake = computed(() => foodIntakes.value.find((item) => item.id === selectedIntakeId.value) ?? null)
const selectedSummaryStat = computed(
  () => nutritionStats.value.find((item) => item.period === selectedSummaryPeriod.value) ?? null,
)
const selectedRecord = computed(() => healthRecords.value.find((item) => item.id === selectedRecordId.value) ?? null)
const selectedRecordDisplayIndex = computed(() => {
  if (!selectedRecord.value?.id) {
    return null
  }
  const index = healthRecords.value.findIndex((item) => item.id === selectedRecord.value?.id)
  return index >= 0 ? index + 1 : null
})
const activeReminders = computed(() =>
  [...reminders.value].sort((a, b) => {
    const timeA = a.reminderTime ? new Date(a.reminderTime).getTime() : 0
    const timeB = b.reminderTime ? new Date(b.reminderTime).getTime() : 0
    return timeA - timeB
  }),
)
const showCompleteProfileReminder = computed(() => !selectedUser.value && isProfileOnboardingSkipped(getAccountId()))

function resolveProfileSnapshot(record: HealthRecord | null, user: User | null) {
  return {
    age: record?.ageSnapshot ?? user?.age,
    gender: record?.genderSnapshot ?? user?.gender,
    height: record?.heightSnapshot ?? user?.height,
    weight: record?.weightSnapshot ?? user?.weight,
  }
}

const selectedProfileSnapshot = computed(() => resolveProfileSnapshot(selectedRecord.value, selectedUser.value))
const profileGenderIcon = computed(() =>
  selectedProfileSnapshot.value.gender === 'female' ? profileFemaleIcon : profileMaleIcon,
)
const profileSummaryItems = computed(() => {
  if (!selectedUser.value) {
    return []
  }

  return [
    {
      key: 'age',
      label: 'Age',
      value: displayValue(selectedProfileSnapshot.value.age),
      unit: 'yrs',
      icon: profileAgeIcon,
    },
    {
      key: 'gender',
      label: 'Gender',
      value: formatGender(selectedProfileSnapshot.value.gender),
      unit: '',
      icon: profileGenderIcon.value,
    },
    {
      key: 'height',
      label: 'Height',
      value: displayValue(selectedProfileSnapshot.value.height),
      unit: 'cm',
      icon: profileHeightIcon,
    },
    {
      key: 'weight',
      label: 'Weight',
      value: displayValue(selectedProfileSnapshot.value.weight),
      unit: 'kg',
      icon: profileWeightIcon,
    },
  ]
})

function setStatus(message: string) {
  statusMessage.value = message
  window.setTimeout(() => {
    if (statusMessage.value === message) {
      statusMessage.value = ''
    }
  }, 3000)
}

async function goToProfileOnboarding() {
  await router.push({
    name: 'profile-onboarding',
    query: { redirect: '/health' },
  })
}

function displayValue(value?: number | string) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  return value
}

function formatNumber(value?: number | string) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  const parsed = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(parsed)) {
    return value
  }
  return Number(parsed.toFixed(1))
}

function formatDateTime(value?: string) {
  if (!value) {
    return 'Not provided'
  }

  return new Intl.DateTimeFormat(dateLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
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

function formatGender(value?: string) {
  if (!value) {
    return '-'
  }

  return value.charAt(0).toUpperCase() + value.slice(1)
}

function formatReminderType(value?: string) {
  if (!value) {
    return 'Reminder'
  }
  return value
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

function formatStatsPeriod(period: string) {
  if (statsType.value !== 'week') {
    return period
  }

  const match = period.match(/^(\d{4})-(\d{2})$/)
  if (!match) {
    return period
  }

  const isoYear = Number(match[1])
  const isoWeek = Number(match[2])
  if (!isoYear || !isoWeek) {
    return period
  }

  const januaryFourth = new Date(Date.UTC(isoYear, 0, 4))
  const dayOfWeek = januaryFourth.getUTCDay() || 7
  const weekOneMonday = new Date(januaryFourth)
  weekOneMonday.setUTCDate(januaryFourth.getUTCDate() - dayOfWeek + 1)

  const startDate = new Date(weekOneMonday)
  startDate.setUTCDate(weekOneMonday.getUTCDate() + (isoWeek - 1) * 7)

  const endDate = new Date(startDate)
  endDate.setUTCDate(startDate.getUTCDate() + 6)

  const formatIsoDate = (value: Date) =>
    `${value.getUTCFullYear()}-${String(value.getUTCMonth() + 1).padStart(2, '0')}-${String(value.getUTCDate()).padStart(2, '0')}`

  return `${formatIsoDate(startDate)} - ${formatIsoDate(endDate)}`
}

async function loadBaseUser() {
  const users = await getUsers()
  const currentUser = users[0] ?? null
  selectedUser.value = currentUser
  selectedUserId.value = currentUser?.id ?? null
}

async function loadHealthRecords() {
  if (!selectedUserId.value) {
    healthRecords.value = []
    selectedRecordId.value = null
    return
  }

  busy.records = true
  try {
    const records = await getHealthRecordsByUserId(selectedUserId.value)
    healthRecords.value = [...records].sort((a, b) => {
      const timeA = a.recordedAt ? new Date(a.recordedAt).getTime() : 0
      const timeB = b.recordedAt ? new Date(b.recordedAt).getTime() : 0
      return timeB - timeA
    })
    if (selectedRecordId.value && !healthRecords.value.some((item) => item.id === selectedRecordId.value)) {
      selectedRecordId.value = null
    }
  } finally {
    busy.records = false
  }
}

async function loadFoodIntakes() {
  if (!selectedUserId.value) {
    foodIntakes.value = []
    return
  }

  busy.intakes = true
  try {
    const logs = await getFoodIntakeByUserId(selectedUserId.value)
    foodIntakes.value = [...logs].sort((a, b) => {
      const timeA = a.intakeTime ? new Date(a.intakeTime).getTime() : 0
      const timeB = b.intakeTime ? new Date(b.intakeTime).getTime() : 0
      return timeB - timeA
    })

    if (selectedIntakeId.value && !foodIntakes.value.some((item) => item.id === selectedIntakeId.value)) {
      selectedIntakeId.value = null
    }
  } finally {
    busy.intakes = false
  }
}

async function loadNutritionStats() {
  if (!selectedUserId.value) {
    nutritionStats.value = []
    updateChart()
    return
  }

  busy.stats = true
  try {
    const stats = await getNutritionStats(selectedUserId.value, statsType.value)
    nutritionStats.value = stats
    if (selectedSummaryPeriod.value && !nutritionStats.value.some((item) => item.period === selectedSummaryPeriod.value)) {
      selectedSummaryPeriod.value = null
    }
    updateChart()
  } catch {
    nutritionStats.value = []
    selectedSummaryPeriod.value = null
    updateChart()
  } finally {
    busy.stats = false
  }
}

async function loadFoodData() {
  await Promise.all([loadFoodIntakes(), loadNutritionStats()])
}

async function loadGoalSnapshots() {
  const goalTypes: Array<{ type: GoalType; label: string }> = [
    { type: 'muscle_gain', label: 'Muscle gain' },
    { type: 'weight_loss', label: 'Weight loss' },
    { type: 'fat_loss', label: 'Fat loss' },
  ]

  if (!selectedUserId.value) {
    goalCards.value = goalTypes.map((item) => ({
      type: item.type,
      label: item.label,
    }))
    return
  }

  busy.goals = true
  try {
    const responses = await Promise.all(goalTypes.map((item) => getDashboardDataByType(item.type)))
    goalCards.value = goalTypes.map((item, index) => ({
      type: item.type,
      label: item.label,
      ...responses[index],
    }))
  } finally {
    busy.goals = false
  }
}

async function loadReminders() {
  busy.reminders = true
  try {
    reminders.value = await getMyReminders()
  } finally {
    busy.reminders = false
  }
}

async function refreshAllData() {
  busy.overview = true
  try {
    await loadBaseUser()
    await Promise.all([loadHealthRecords(), loadFoodData(), loadGoalSnapshots(), loadReminders()])
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to load dashboard data.')
  } finally {
    busy.overview = false
  }
}

function requestDeleteRecord(recordId?: number) {
  if (!recordId) {
    return
  }
  pendingDeleteRecordId.value = recordId
  showDeleteRecordConfirm.value = true
}

function selectRecord(record: HealthRecord) {
  if (!record.id) {
    return
  }
  selectedRecordId.value = record.id
}

async function goToGoal(goalType: GoalType) {
  await router.push({
    name: 'fitness-goals',
    query: { goalType },
  })
}

async function goToReminder(reminderId?: number) {
  await router.push({
    name: 'reminders',
    query: reminderId ? { reminderId: String(reminderId) } : undefined,
  })
}

function cancelDeleteRecord() {
  pendingDeleteRecordId.value = null
  showDeleteRecordConfirm.value = false
}

async function confirmDeleteRecord() {
  if (!pendingDeleteRecordId.value) {
    cancelDeleteRecord()
    return
  }

  busy.deletingRecord = true
  try {
    await deleteHealthRecord(pendingDeleteRecordId.value)
    if (selectedRecordId.value === pendingDeleteRecordId.value) {
      selectedRecordId.value = null
    }
    await loadHealthRecords()
    setStatus('Health record deleted successfully.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to delete health record.')
  } finally {
    busy.deletingRecord = false
    cancelDeleteRecord()
  }
}

function requestDeleteIntake(intakeId?: number) {
  if (!intakeId) {
    return
  }
  pendingDeleteIntakeId.value = intakeId
  showDeleteIntakeConfirm.value = true
}

function cancelDeleteIntake() {
  pendingDeleteIntakeId.value = null
  showDeleteIntakeConfirm.value = false
}

async function confirmDeleteIntake() {
  if (!pendingDeleteIntakeId.value) {
    cancelDeleteIntake()
    return
  }

  busy.deletingIntake = true
  try {
    await deleteFoodIntake(pendingDeleteIntakeId.value)
    if (selectedIntakeId.value === pendingDeleteIntakeId.value) {
      selectedIntakeId.value = null
    }
    await loadFoodData()
    setStatus('Food intake deleted successfully.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to delete intake log.')
  } finally {
    busy.deletingIntake = false
    cancelDeleteIntake()
  }
}

function selectIntakeLog(item: FoodIntake) {
  if (!item.id) {
    return
  }
  selectedIntakeId.value = item.id
  selectedSummaryPeriod.value = null
  updateChart()
}

function selectSummaryStatItem(item: NutritionStats) {
  selectedSummaryPeriod.value = item.period
  selectedIntakeId.value = null
  updateChart()
}

function initializeChart() {
  if (!chartRef.value) {
    return
  }
  nutritionChart = echarts.init(chartRef.value)
  updateChart()
}

function resizeChart() {
  nutritionChart?.resize()
}

function updateChart() {
  if (!nutritionChart) {
    return
  }

  const nutrientMap = new Map<string, number>()
  const sourceNutrients = selectedIntake.value?.nutrients
    ? [selectedIntake.value.nutrients]
    : selectedSummaryStat.value?.nutrients
      ? [selectedSummaryStat.value.nutrients]
      : nutritionStats.value.map((item) => item.nutrients || '')

  sourceNutrients.forEach((rawNutrients) => {
    if (!rawNutrients || rawNutrients === 'No nutrients info') {
      return
    }

    const nutrientParts = rawNutrients
      .split(',')
      .map((part) => part.trim())
      .filter((part) => part.length > 0)
    if (nutrientParts.length === 0) {
      return
    }

    const equalShare = 100 / nutrientParts.length
    nutrientParts.forEach((part) => {
      const ratioMatch = part.match(/^(.+?)\((\d+(?:\.\d+)?)%\)$/)
      const name = (ratioMatch?.[1] || part).trim()
      const ratio = ratioMatch ? Number(ratioMatch[2]) : equalShare
      nutrientMap.set(name, (nutrientMap.get(name) || 0) + ratio)
    })
  })

  const nutrientData = Array.from(nutrientMap.entries())
    .map(([name, value]) => ({ name: t(name), value: Number(value.toFixed(1)) }))
    .sort((a, b) => b.value - a.value)

  if (nutrientData.length === 0) {
    nutritionChart.clear()
    return
  }

  nutritionChart.setOption(
    {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {d}%',
      },
      legend: {
        orient: 'vertical',
        right: 16,
        top: 'middle',
        textStyle: {
          color: '#4b5c5b',
        },
      },
      series: [
        {
          name: selectedIntake.value
            ? `${selectedIntake.value.foodName} ${t('ratio')}`
            : selectedSummaryStat.value
              ? `${formatStatsPeriod(selectedSummaryStat.value.period)} ${t('ratio')}`
              : t('Nutrition ratio'),
          type: 'pie',
          radius: ['44%', '72%'],
          center: ['36%', '50%'],
          data: nutrientData,
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#fff',
            borderWidth: 2,
          },
          label: {
            show: true,
            formatter: '{b}\n{d}%',
            color: '#35565d',
            fontSize: 11,
          },
          labelLine: {
            length: 12,
            length2: 8,
          },
        },
      ],
    },
    { notMerge: true },
  )
}

watch(locale, updateChart)

watch(statsType, () => {
  selectedIntakeId.value = null
  selectedSummaryPeriod.value = null
  void loadNutritionStats()
})

onMounted(async () => {
  await refreshAllData()
  await nextTick()
  initializeChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  nutritionChart?.dispose()
  nutritionChart = null
})
</script>

<style scoped>
.overview-page {
  display: grid;
  gap: 1.2rem;
}

.hero-band {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.3rem;
  border-radius: 20px;
  border: 1px solid #dae5e0;
  background: linear-gradient(90deg, #fffdf9 0%, #eff8f6 100%);
}

.eyebrow {
  margin: 0 0 0.35rem;
  color: #855a41;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-size: 0.76rem;
  font-weight: 700;
}

h1,
h2 {
  margin: 0;
  color: #163b46;
}

h1 {
  font-size: 2.8rem;
}

.hero-copy {
  margin: 0.7rem 0 0;
  color: #355a66;
  line-height: 1.5;
}

.status-banner {
  margin: 0;
  padding: 0.8rem 1rem;
  border-radius: 12px;
  border: 1px solid #f0cf73;
  background: #fff6d4;
  color: #775200;
}

.profile-reminder {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  padding: 0.95rem 1rem;
  border-radius: 14px;
  border: 1px solid #f1d57a;
  background: #fff6d6;
  color: #7d5a00;
}

.profile-reminder p {
  margin: 0;
  line-height: 1.5;
}

.profile-reminder .primary-button {
  border: 0;
  border-radius: 12px;
  padding: 0.65rem 0.95rem;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, #1f7a8c, #3aa17e);
  color: #fff;
}

.profile-summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.profile-summary-context {
  grid-column: 1 / -1;
  margin: 0;
  color: #5b7393;
  font-size: 0.88rem;
  font-weight: 600;
}

.profile-summary-card {
  border: 3px solid #d9d5ee;
  border-radius: 18px;
  background: #faf9ff;
  padding: 0.95rem 0.7rem;
  display: grid;
  justify-items: center;
  gap: 0.45rem;
}

.profile-summary-icon-wrap {
  width: 54px;
  height: 54px;
  border-radius: 12px;
  border: 1.5px solid #bac0e9;
  background: #ffffff;
  display: grid;
  place-items: center;
}

.profile-summary-icon {
  width: 38px;
  height: 38px;
  object-fit: contain;
  image-rendering: auto;
}

.profile-summary-icon.is-gender {
  width: 40px;
  height: 40px;
  object-fit: cover;
  object-position: center;
}

.profile-summary-label {
  margin: 0;
  font-size: 0.85rem;
  color: #8f8bb3;
  font-weight: 600;
}

.profile-summary-value {
  margin: 0;
  font-size: 1.6rem;
  line-height: 1;
  color: #3a3682;
  font-weight: 700;
}

.profile-summary-value span {
  margin-left: 0.15rem;
  font-size: 0.92rem;
  color: #9f9abf;
  font-weight: 600;
}

.top-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.2rem;
}

.left-stack {
  display: grid;
  gap: 1.2rem;
}

.card {
  padding: 1.2rem;
  border-radius: 22px;
  background: #fffdfa;
  border: 1px solid #dce7e2;
  box-shadow: 0 16px 36px rgba(17, 57, 64, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.card-header select {
  width: 130px;
  padding: 0.72rem 0.86rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #ffffff;
  color: #173f46;
  font: inherit;
}

.ghost-button {
  border: 0;
  border-radius: 14px;
  padding: 0.82rem 1.05rem;
  font: inherit;
  cursor: pointer;
  background: #edf5f4;
  color: #24555f;
}

.ghost-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stack-list {
  display: grid;
  gap: 0.75rem;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem;
}

.metric-item {
  margin: 0;
  padding: 0.72rem 0.8rem;
  border: 1px solid #d7e4df;
  background: #f6fbfa;
  border-radius: 12px;
  display: grid;
  gap: 0.2rem;
}

.metric-item span {
  color: #58727a;
}

.metric-item strong {
  color: #163b46;
  font-size: 1.05rem;
}

.record-panel {
  padding: 0.85rem 0.95rem;
  border: 1px solid #d7e4df;
  border-radius: 12px;
  background: #f8fcfb;
}

.record-panel h3 {
  margin: 0 0 0.5rem;
  color: #173f46;
  font-size: 1.05rem;
}

.record-panel p {
  margin: 0.22rem 0;
  color: #567078;
}

.list-item {
  padding: 0.9rem;
  border: 1px solid #d8e3de;
  border-radius: 12px;
  background: #f8fcfb;
}

.goal-item,
.reminder-row {
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}

.goal-item:hover,
.reminder-row:hover {
  transform: translateY(-1px);
  border-color: #c4d9d3;
}

.goal-item:focus-visible,
.reminder-row:focus-visible {
  outline: 2px solid rgba(31, 122, 140, 0.24);
  outline-offset: 2px;
  border-color: #1f7a8c;
  background: #e7f3f1;
}

.list-item header,
.timeline-item header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.4rem;
}

.list-item strong,
.timeline-item strong {
  color: #1a434f;
}

.list-item p,
.list-item span,
.timeline-item p,
.timeline-item span {
  margin: 0;
  color: #587176;
}

.record-header-actions {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.combined-item {
  background: #f3f8f7;
  border-color: #d4e2de;
}

.record-row {
  cursor: pointer;
  transition:
    transform 0.15s ease,
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.record-row:hover {
  transform: translateY(-1px);
  border-color: #bed4ce;
}

.record-row.active {
  border-color: #1f7a8c;
  background: #e7f3f1;
}

.combined-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.combined-block {
  padding: 0.7rem;
  border-radius: 14px;
  border: 1px solid #d7e5e2;
  background: #ffffff;
}

.combined-block h3 {
  margin: 0 0 0.5rem;
  font-size: 0.9rem;
  color: #1b4a52;
}

.combined-block p {
  margin: 0.2rem 0;
  color: #4f6d73;
}

.chart {
  width: 100%;
  height: 380px;
  margin-bottom: 0.75rem;
  border-radius: 16px;
  border: 1px solid #d8e5e1;
  background: #f7fbfa;
}

.timeline {
  display: grid;
  gap: 0.75rem;
}

.compact-list {
  max-height: 320px;
  overflow: auto;
}

.logs-list {
  max-height: 340px;
}

.timeline-item {
  width: 100%;
  padding: 0.85rem 0.9rem;
  text-align: left;
  border-radius: 0;
  border: 1px solid #dde8e2;
  background: #f7faf8;
}

.summary-row,
.intake-row {
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}

.summary-row:hover,
.intake-row:hover {
  transform: translateY(-1px);
  border-color: #c4d9d3;
}

.summary-row.active,
.intake-row.active {
  border-color: #1f7a8c;
  background: #e7f3f1;
}

.intake-header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.delete-icon-button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #a5b0b5;
  background: #eef3f4;
  cursor: pointer;
  transition: all 0.2s ease;
}

.delete-icon-button svg {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

.delete-icon-button:hover {
  color: #ffffff;
  background: #bd3124;
  transform: translateY(-1px);
}

.empty-state {
  padding: 1.2rem;
  border-radius: 14px;
  background: #f4f8f6;
  color: #6c848a;
  text-align: center;
}

.empty-state.compact {
  padding-top: 0.75rem;
}

.reminder-list {
  max-height: 700px;
}

.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
}

.confirm-modal {
  width: min(92vw, 420px);
  padding: 1.4rem;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e1e7df;
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.22);
}

.confirm-title {
  margin: 0;
  font-size: 1.12rem;
  color: #12343b;
  font-weight: 700;
}

.confirm-copy {
  margin: 0.55rem 0 0;
  color: #587176;
}

.confirm-actions {
  margin-top: 1.2rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
}

.confirm-cancel-button,
.confirm-delete-button {
  border: 0;
  border-radius: 10px;
  padding: 0.6rem 1rem;
  font: inherit;
  cursor: pointer;
  color: #ffffff;
}

.confirm-cancel-button {
  background: #8f989e;
}

.confirm-delete-button {
  background: #bd3124;
}

.confirm-cancel-button:disabled,
.confirm-delete-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 1180px) {
  h1 {
    font-size: 2.2rem;
  }

  .top-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .profile-summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-band,
  .card-header,
  .list-item header,
  .timeline-item header {
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .combined-grid {
    grid-template-columns: 1fr;
  }

  .profile-summary-strip {
    grid-template-columns: 1fr;
  }

  .chart {
    height: 320px;
  }
}
</style>
