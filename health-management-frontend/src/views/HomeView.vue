<template>
  <div class="page">
    <section class="hero">
      <div>
        <p class="eyebrow">{{ t("Integrated dashboard") }}</p>
        <h1>{{ t("Health and nutrition in one workflow") }}</h1>
        <p class="hero-copy"> {{ t("Save your personal profile, record health indicators, log food intake, and view nutrition totals.") }} </p>
      </div>
      <div class="hero-cards">
        <article class="summary-card">
          <span>{{ t("Health records") }}</span>
          <strong>{{ t(healthRecords.length) }}</strong>
        </article>
        <article class="summary-card">
          <span>{{ t("Food logs") }}</span>
          <strong>{{ t(foodIntakes.length) }}</strong>
        </article>
      </div>
    </section>

    <p v-if="statusMessage" class="status-banner">{{ t(statusMessage) }}</p>

    <div class="layout">
      <section class="panel stack">
        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Step 1") }}</p>
              <h2>{{ t("Personal profile & health record") }}</h2>
            </div>
            <button class="ghost-button" type="button" @click="refreshProfileAndRecords"> {{ t("Refresh profile data") }} </button>
          </div>

          <div class="module-stack">
            <form class="form-grid" @submit.prevent="handleSubmitProfileAndHealth">
              <div class="field-group-title full-width">
                <h3 class="module-title">{{ t("Personal profile") }}</h3>
                <p class="module-copy">{{ t("Save your personal baseline data.") }}</p>
              </div>
              <label>
                <span>{{ t("Age") }}</span>
                <input v-model.number="userForm.age" min="1" type="number" :placeholder="t('age')" required />
              </label>
              <label>
                <span>{{ t("Gender") }}</span>
                <select v-model="userForm.gender" required>
                  <option value="" disabled>{{ t("Select gender") }}</option>
                  <option value="male">{{ t("male") }}</option>
                  <option value="female">{{ t("female") }}</option>
                </select>
              </label>
              <label>
                <span>{{ t("Height (cm)") }}</span>
                <input v-model.number="userForm.height" min="1" step="0.1" type="number" :placeholder="t('height')" required />
              </label>
              <label>
                <span>{{ t("Weight (kg)") }}</span>
                <input v-model.number="userForm.weight" min="1" step="0.1" type="number" :placeholder="t('weight')" required />
              </label>

              <div class="field-group-title full-width">
                <h3 class="module-title">{{ t("Health record") }}</h3>
                <p class="module-copy">{{ t("Submit blood pressure, glucose, and pulse indicators.") }}</p>
              </div>
              <label>
                <span>{{ t("Systolic") }}</span>
                <input v-model.number="healthForm.systolic" min="60" type="number" :placeholder="t('systolic')" required />
              </label>
              <label>
                <span>{{ t("Diastolic") }}</span>
                <input v-model.number="healthForm.diastolic" min="40" type="number" :placeholder="t('diastolic')" required />
              </label>
              <label>
                <span>{{ t("FBG (mmol/L)") }}</span>
                <input v-model.number="healthForm.fbg" min="0" step="0.1" type="number" :placeholder="t('fbg')" />
              </label>
              <label>
                <span>{{ t("Heart rate (bpm)") }}</span>
                <input v-model.number="healthForm.heartRate" min="0" type="number" :placeholder="t('heart rate')" />
              </label>
              <label>
                <span>{{ t("Oxyhemoglobin (%)") }}</span>
                <input v-model.number="healthForm.oxyhemoglobin" min="0" step="0.1" type="number" :placeholder="t('oxyhemoglobin')" />
              </label>
              <label>
                <span>{{ t("Recorded on") }}</span>
                <CustomDatePicker v-model="healthForm.recordedAt" />
              </label>

              <div class="form-actions full-width">
                <button class="primary-button" type="submit" :disabled="isSubmittingProfileHealth">
                  {{ t(isSubmittingProfileHealth ? 'Submitting...' : 'Submit profile & record') }}
                </button>
                <button class="secondary-button" type="button" @click="resetProfileAndHealthForm"> {{ t("Reset") }} </button>
              </div>
            </form>
          </div>
        </article>

        <article v-if="showEntryFoodModules" class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Step 2") }}</p>
              <h2>{{ t("Food intake") }}</h2>
            </div>
            <button
              class="ghost-button"
              type="button"
              :disabled="!selectedUserId"
              @click="loadFoodData"
            > {{ t("Reload food data") }} </button>
          </div>

          <form class="form-grid" @submit.prevent="handleCreateFoodIntake">
            <label>
              <span>{{ t("Food") }}</span>
              <select v-if="foodLibrary.length > 0" v-model="foodForm.foodName" required>
                <option value="" disabled>{{ t("Select food") }}</option>
                <option v-for="food in foodLibrary" :key="food.id" :value="food.foodName">
                  {{ food.foodName }}
                </option>
              </select>
              <input
                v-else
                v-model="foodForm.foodName"
                type="text"
                :placeholder="t('Enter food name manually')"
                required
              />
            </label>
            <label>
              <span>{{ t("Amount") }}</span>
              <input v-model.number="foodForm.amount" min="1" step="0.1" type="number" :placeholder="t('amount')" required />
            </label>
            <label>
              <span>{{ t("Unit") }}</span>
              <input v-model="foodForm.unit" type="text" :placeholder="t('unit')" />
            </label>
            <label>
              <span>{{ t("Intake time") }}</span>
              <CustomDateTimePicker v-model="foodForm.intakeTime" />
            </label>

            <div class="form-actions full-width">
              <button class="primary-button" type="submit" :disabled="busy.food || !selectedUserId">
                {{ t(busy.food ? 'Saving...' : 'Log food') }}
              </button>
              <button class="secondary-button" type="button" @click="resetFoodForm">{{ t("Reset") }}</button>
            </div>
          </form>
        </article>
      </section>

      <aside class="stack">
        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Health") }}</p>
              <h2>{{ t("Profile & recent records") }}</h2>
            </div>
          </div>

          <div v-if="busy.users && !selectedUser" class="empty-state">{{ t("Loading profile...") }}</div>
          <div v-else-if="!selectedUser" class="empty-state"> {{ t("Your profile is not complete yet. Please add your personal information") }} </div>
          <div v-else-if="busy.records" class="empty-state">{{ t("Loading records...") }}</div>
          <div v-else class="timeline">
            <template v-if="healthRecords.length === 0">
              <div class="empty-state">{{ t("Your profile is not complete yet. Please add your personal information") }}</div>
            </template>
            <template v-else>
              <article v-for="(record, index) in healthRecords" :key="record.id" class="timeline-item combined-item">
                <header>
                  <strong>{{ t("Record") }} {{ t(index + 1) }}</strong>
                  <div class="record-header-actions">
                    <span>{{ t(formatDate(record.recordedAt)) }}</span>
                    <button
                      type="button"
                      class="delete-icon-button"
                      :aria-label="t('Delete record')"
                      @click="requestDeleteRecord(record.id)"
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
                    <p>{{ t("Age:") }} {{ t(selectedUser.age) }} {{ t("yrs") }}</p>
                    <p>{{ t("Gender:") }} {{ t(selectedUser.gender) }}</p>
                    <p>{{ t("Height:") }} {{ t(selectedUser.height) }} {{ t("cm") }}</p>
                    <p>{{ t("Weight:") }} {{ t(selectedUser.weight) }} {{ t("kg") }}</p>
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

        <article v-if="showEntryFoodModules" class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Nutrition") }}</p>
              <h2>{{ t("Summary") }}</h2>
            </div>
            <select v-model="statsType">
              <option value="day">{{ t("day") }}</option>
              <option value="week">{{ t("week") }}</option>
              <option value="month">{{ t("month") }}</option>
            </select>
          </div>

          <div class="summary-food-meta" v-if="selectedFood">
            <p>{{ t("Calories / 100g:") }} {{ t(selectedFood.calories) }}</p>
            <p>{{ t("Nutrients:") }} {{ selectedFood.nutrients || 'No nutrients info' }}</p>
          </div>
          <p v-else-if="foodLibrary.length === 0" class="food-hint"> {{ t("Food library is empty or not loaded. You can still type a food name manually.") }} </p>

          <div ref="chartRef" class="chart"></div>

          <div v-if="!selectedUserId" class="empty-state compact">{{ t("Save your profile to load nutrition stats.") }}</div>
          <div v-else-if="busy.stats" class="empty-state compact">{{ t("Loading nutrition stats...") }}</div>
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

        <article v-if="showEntryFoodModules" class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Food history") }}</p>
              <h2>{{ t("Latest intake logs") }}</h2>
            </div>
          </div>

          <div v-if="!selectedUserId" class="empty-state">{{ t("Save your profile to see food logs.") }}</div>
          <div v-else-if="busy.intakes" class="empty-state">{{ t("Loading intake logs...") }}</div>
          <div v-else-if="foodIntakes.length === 0" class="empty-state">{{ t("No food intake records yet.") }}</div>
          <div v-else class="timeline compact-list">
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
      </aside>
    </div>

    <div v-if="showDeleteConfirm" class="confirm-overlay">
      <div class="confirm-modal" @click.stop>
        <p class="confirm-title">{{ t("Delete this health record?") }}</p>
        <p class="confirm-copy">{{ t("This action cannot be undone.") }}</p>
        <div class="confirm-actions">
          <button
            type="button"
            class="confirm-cancel-button"
            :disabled="busy.deleting"
            @click="cancelDeleteRecord"
          > {{ t("Cancel") }} </button>
          <button
            type="button"
            class="confirm-delete-button"
            :disabled="busy.deleting"
            @click="confirmDeleteRecord"
          >
            {{ t(busy.deleting ? 'Deleting...' : 'Confirm') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteIntakeConfirm" class="confirm-overlay">
      <div class="confirm-modal" @click.stop>
        <p class="confirm-title">{{ t("Delete this intake log?") }}</p>
        <p class="confirm-copy">{{ t("This action cannot be undone.") }}</p>
        <div class="confirm-actions">
          <button
            type="button"
            class="confirm-cancel-button"
            :disabled="busy.deletingIntake"
            @click="cancelDeleteIntake"
          > {{ t("Cancel") }} </button>
          <button
            type="button"
            class="confirm-delete-button"
            :disabled="busy.deletingIntake"
            @click="confirmDeleteIntake"
          >
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

import {
  createFoodIntake,
  deleteFoodIntake,
  getFoodIntakeByUserId,
  getFoodLibrary,
  getFoodLibraryByName,
  getNutritionStats,
} from '../api/food'
import { createHealthRecord, deleteHealthRecord, getHealthRecordsByUserId } from '../api/healthRecords'
import type { FoodIntake, FoodLibrary, HealthRecord, NutritionStats, User } from '../api/types'
import { createUser, getUsers, updateUser } from '../api/users'
import CustomDatePicker from '../components/CustomDatePicker.vue'
import CustomDateTimePicker from '../components/CustomDateTimePicker.vue'
import { getAccountId } from '../utils/auth'
import { setProfileComplete, setProfileOnboardingSkipped } from '../utils/profileOnboarding'

type BusyState = {
  user: boolean
  users: boolean
  health: boolean
  records: boolean
  deleting: boolean
  deletingIntake: boolean
  food: boolean
  intakes: boolean
  stats: boolean
}

const STORAGE_KEY = 'selectedUserId'
const showEntryFoodModules = false

const chartRef = ref<HTMLDivElement | null>(null)
let nutritionChart: echarts.ECharts | null = null

const users = ref<User[]>([])
const healthRecords = ref<HealthRecord[]>([])
const foodLibrary = ref<FoodLibrary[]>([])
const foodIntakes = ref<FoodIntake[]>([])
const nutritionStats = ref<NutritionStats[]>([])
const selectedUserId = ref<number | null>(null)
const selectedIntakeId = ref<number | null>(null)
const selectedSummaryPeriod = ref<string | null>(null)
const statusMessage = ref('')
const statsType = ref<'day' | 'week' | 'month'>('week')
const showDeleteConfirm = ref(false)
const pendingDeleteRecordId = ref<number | null>(null)
const showDeleteIntakeConfirm = ref(false)
const pendingDeleteIntakeId = ref<number | null>(null)

const busy = reactive<BusyState>({
  user: false,
  users: false,
  health: false,
  records: false,
  deleting: false,
  deletingIntake: false,
  food: false,
  intakes: false,
  stats: false,
})

const userForm = reactive({
  age: null as number | null,
  gender: '' as '' | 'male' | 'female',
  height: null as number | null,
  weight: null as number | null,
})

const healthForm = reactive({
  systolic: null as number | null,
  diastolic: null as number | null,
  fbg: null as number | null,
  heartRate: null as number | null,
  oxyhemoglobin: null as number | null,
  recordedAt: '',
})

const foodForm = reactive({
  foodName: '',
  amount: null as number | null,
  unit: '',
  intakeTime: '',
})

const selectedFood = computed(() =>
  foodLibrary.value.find((item) => item.foodName === foodForm.foodName) ?? null,
)

const selectedUser = computed(() => users.value.find((item) => item.id === selectedUserId.value) ?? null)
const selectedIntake = computed(
  () => foodIntakes.value.find((item) => item.id === selectedIntakeId.value) ?? null,
)
const selectedSummaryStat = computed(
  () => nutritionStats.value.find((item) => item.period === selectedSummaryPeriod.value) ?? null,
)

const isSubmittingProfileHealth = computed(() => busy.user || busy.health)

function setStatus(message: string) {
  statusMessage.value = message
  window.setTimeout(() => {
    if (statusMessage.value === message) {
      statusMessage.value = ''
    }
  }, 3000)
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
    return 'Not provided'
  }

  return new Intl.DateTimeFormat(dateLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
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

function displayValue(value?: number | string) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  return value
}

function toOptionalNumber(value: number | null) {
  if (value === null || Number.isNaN(Number(value))) {
    return undefined
  }
  return Number(value)
}

function toApiLocalDateTime(value?: string) {
  const parsed = value ? new Date(value) : new Date()
  const year = parsed.getFullYear()
  const month = String(parsed.getMonth() + 1).padStart(2, '0')
  const day = String(parsed.getDate()).padStart(2, '0')
  const hour = String(parsed.getHours()).padStart(2, '0')
  const minute = String(parsed.getMinutes()).padStart(2, '0')
  const second = String(parsed.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

function resetUserForm() {
  userForm.age = null
  userForm.gender = ''
  userForm.height = null
  userForm.weight = null
}

function resetHealthForm() {
  healthForm.systolic = null
  healthForm.diastolic = null
  healthForm.fbg = null
  healthForm.heartRate = null
  healthForm.oxyhemoglobin = null
  healthForm.recordedAt = ''
}

function resetFoodForm() {
  foodForm.foodName = ''
  foodForm.amount = null
  foodForm.unit = ''
  foodForm.intakeTime = ''
}

function resetProfileAndHealthForm() {
  resetUserForm()
  resetHealthForm()
}

function requestDeleteRecord(recordId?: number) {
  if (!recordId) {
    setStatus('Invalid record ID.')
    return
  }

  pendingDeleteRecordId.value = recordId
  showDeleteConfirm.value = true
}

function cancelDeleteRecord() {
  showDeleteConfirm.value = false
  pendingDeleteRecordId.value = null
}

function requestDeleteIntake(intakeId?: number) {
  if (!intakeId) {
    setStatus('Invalid intake record ID.')
    return
  }
  pendingDeleteIntakeId.value = intakeId
  showDeleteIntakeConfirm.value = true
}

function cancelDeleteIntake() {
  showDeleteIntakeConfirm.value = false
  pendingDeleteIntakeId.value = null
}

function selectUser(userId: number) {
  selectedUserId.value = userId
  selectedIntakeId.value = null
  localStorage.setItem(STORAGE_KEY, String(userId))
  void loadHealthRecords()
  void loadFoodData()
}

async function loadUsers() {
  busy.users = true
  try {
    const data = await getUsers()
    users.value = data

    if (!selectedUserId.value && data.length > 0) {
      const storedUserId = Number(localStorage.getItem(STORAGE_KEY))
      const matchedUser = data.find((item) => item.id === storedUserId)
      const userToSelect = matchedUser ?? data[0] ?? null
      if (userToSelect?.id) {
        selectUser(userToSelect.id)
      }
    }

    if (selectedUserId.value && !data.some((item) => item.id === selectedUserId.value)) {
      selectedUserId.value = null
      selectedIntakeId.value = null
      localStorage.removeItem(STORAGE_KEY)
      healthRecords.value = []
      foodIntakes.value = []
      nutritionStats.value = []
      updateChart()
    }
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to load users')
  } finally {
    busy.users = false
  }
}

async function loadFoodLibraryData() {
  try {
    foodLibrary.value = await getFoodLibrary()
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to load food library')
  }
}

async function loadHealthRecords() {
  if (!selectedUserId.value) {
    return
  }

  busy.records = true
  try {
    healthRecords.value = await getHealthRecordsByUserId(selectedUserId.value)
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to load health records')
  } finally {
    busy.records = false
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
    nutritionStats.value = await getNutritionStats(selectedUserId.value, statsType.value)
    if (selectedSummaryPeriod.value && !nutritionStats.value.some((item) => item.period === selectedSummaryPeriod.value)) {
      selectedSummaryPeriod.value = null
    }
    updateChart()
  } catch (error) {
    nutritionStats.value = []
    updateChart()
    setStatus(error instanceof Error ? error.message : 'Failed to load nutrition stats')
  } finally {
    busy.stats = false
  }
}

async function loadFoodIntakes() {
  if (!selectedUserId.value) {
    return
  }

  busy.intakes = true
  try {
    foodIntakes.value = await getFoodIntakeByUserId(selectedUserId.value)
    if (selectedIntakeId.value && !foodIntakes.value.some((item) => item.id === selectedIntakeId.value)) {
      selectedIntakeId.value = null
    }
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to load food logs')
  } finally {
    busy.intakes = false
  }
}

async function loadFoodData() {
  await Promise.all([loadFoodIntakes(), loadNutritionStats()])
}

async function refreshProfileAndRecords() {
  await loadUsers()
  await loadHealthRecords()
}

async function confirmDeleteRecord() {
  if (!pendingDeleteRecordId.value) {
    cancelDeleteRecord()
    return
  }

  busy.deleting = true
  try {
    await deleteHealthRecord(pendingDeleteRecordId.value)
    await Promise.all([loadHealthRecords(), loadFoodData()])
    setStatus('Health record deleted successfully.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to delete health record')
  } finally {
    busy.deleting = false
    cancelDeleteRecord()
  }
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
    setStatus(error instanceof Error ? error.message : 'Failed to delete food intake')
  } finally {
    busy.deletingIntake = false
    cancelDeleteIntake()
  }
}

async function handleSubmitProfileAndHealth() {
  busy.user = true
  busy.health = true
  try {
    let userId = selectedUserId.value
    const gender = userForm.gender === 'male' || userForm.gender === 'female' ? userForm.gender : null

    if (!gender) {
      throw new Error('Please select gender.')
    }

    if (userId) {
      await updateUser(userId, {
        age: Number(userForm.age),
        gender,
        height: Number(userForm.height),
        weight: Number(userForm.weight),
      })
    } else {
      const created = await createUser({
        age: Number(userForm.age),
        gender,
        height: Number(userForm.height),
        weight: Number(userForm.weight),
      })
      if (!created.id) {
        throw new Error('Failed to save profile.')
      }
      userId = created.id
      selectedUserId.value = created.id
      localStorage.setItem(STORAGE_KEY, String(created.id))
    }

    if (!userId) {
      throw new Error('Failed to resolve profile ID.')
    }

    const accountId = getAccountId()
    setProfileComplete(accountId, true)
    setProfileOnboardingSkipped(accountId, false)

    await createHealthRecord(userId, {
      systolic: Number(healthForm.systolic),
      diastolic: Number(healthForm.diastolic),
      fbg: toOptionalNumber(healthForm.fbg),
      heartRate: toOptionalNumber(healthForm.heartRate),
      oxyhemoglobin: toOptionalNumber(healthForm.oxyhemoglobin),
      recordedAt: healthForm.recordedAt ? new Date(`${healthForm.recordedAt}T00:00:00`).toISOString() : undefined,
    })

    resetProfileAndHealthForm()
    await loadUsers()
    await Promise.all([loadHealthRecords(), loadFoodData()])
    setStatus('Profile and health record saved successfully.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to save profile and health record')
  } finally {
    busy.user = false
    busy.health = false
  }
}

async function handleCreateFoodIntake() {
  if (!selectedUserId.value) {
    setStatus('Save your profile before logging food intake.')
    return
  }

  busy.food = true
  try {
    let matchedFood = selectedFood.value
    if (!matchedFood && foodForm.foodName.trim()) {
      try {
        matchedFood = await getFoodLibraryByName(foodForm.foodName.trim())
      } catch {
        matchedFood = null
      }
    }
    const calories = matchedFood ? Math.round((matchedFood.calories * Number(foodForm.amount)) / 100) : 0

    await createFoodIntake({
      userId: selectedUserId.value,
      foodName: foodForm.foodName,
      amount: Number(foodForm.amount),
      unit: foodForm.unit || 'g',
      calories,
      nutrients: matchedFood?.nutrients || '',
      intakeTime: toApiLocalDateTime(foodForm.intakeTime),
    })
    selectedIntakeId.value = null
    resetFoodForm()
    await loadFoodData()
    setStatus('Food intake saved successfully.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to create food intake')
  } finally {
    busy.food = false
  }
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

  nutritionChart.setOption({
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
            : 'Nutrition ratio',
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
  }, { notMerge: true })
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

watch(locale, updateChart)

watch(statsType, () => {
  selectedIntakeId.value = null
  selectedSummaryPeriod.value = null
  void loadNutritionStats()
})

onMounted(async () => {
  await Promise.all([loadUsers(), loadFoodLibraryData()])
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
.page {
  display: grid;
  gap: 1.5rem;
}

.hero {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 24px;
  background:
    radial-gradient(circle at top left, rgba(255, 209, 102, 0.35), transparent 35%),
    linear-gradient(135deg, #f2efe6 0%, #d7ebe7 42%, #c2dde4 100%);
  border: 1px solid rgba(31, 122, 140, 0.12);
}

.eyebrow {
  margin: 0 0 0.35rem;
  color: #7d5a50;
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
  max-width: 48rem;
  margin: 0.85rem 0 0;
  line-height: 1.65;
  color: #2d4d52;
}

.hero-cards {
  display: grid;
  gap: 0.75rem;
}

.summary-card {
  display: grid;
  gap: 0.35rem;
  padding: 1rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
  backdrop-filter: blur(8px);
  box-shadow: 0 14px 28px rgba(17, 59, 68, 0.08);
}

.summary-card span {
  color: #4f6d73;
  font-size: 0.9rem;
}

.summary-card strong {
  color: #12343b;
  font-size: 1.35rem;
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
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.95fr);
  gap: 1.5rem;
}

.stack {
  display: grid;
  gap: 1rem;
}

.panel {
  align-content: start;
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

.module-stack {
  display: grid;
  gap: 1rem;
}

.module-title {
  margin: 0;
  color: #173f46;
  font-size: 1rem;
}

.module-copy {
  margin: 0.3rem 0 0;
  color: #5e7479;
  font-size: 0.9rem;
}

.field-group-title {
  display: grid;
  gap: 0.2rem;
  margin-top: 0.35rem;
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

.form-grid input,
.form-grid select,
.card-header select {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #ffffff;
  color: #173f46;
  font: inherit;
}

.form-grid input:focus,
.form-grid select:focus,
.card-header select:focus {
  outline: 2px solid rgba(31, 122, 140, 0.18);
  border-color: #1f7a8c;
}

.full-width {
  grid-column: 1 / -1;
}

.food-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: #4f6d73;
  font-size: 0.9rem;
}

.food-hint {
  margin: 0;
  color: #8a5a44;
  font-size: 0.9rem;
}

.summary-food-meta {
  margin: 0 0 0.75rem;
  padding: 0.75rem 0.85rem;
  border-radius: 12px;
  border: 1px solid #d8e5e1;
  background: #f4f9f8;
  color: #3f666c;
  display: grid;
  gap: 0.35rem;
}

.summary-food-meta p {
  margin: 0;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
}

.primary-button,
.secondary-button,
.ghost-button {
  border: 0;
  border-radius: 14px;
  font: inherit;
}

.primary-button,
.secondary-button,
.ghost-button {
  padding: 0.85rem 1.1rem;
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

.primary-button:disabled,
.ghost-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.timeline {
  display: grid;
  gap: 0.75rem;
}

.timeline-item {
  width: 100%;
  padding: 0.95rem 1rem;
  text-align: left;
  background: #f7faf8;
  border: 1px solid #dde8e2;
}

.timeline-item strong {
  color: #183f46;
}

.timeline-item span,
.timeline-item p {
  margin: 0;
  color: #587176;
}

.timeline-item header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.4rem;
}

.record-header-actions {
  display: flex;
  align-items: center;
  gap: 0.55rem;
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

.combined-item {
  background: #f3f8f7;
  border-color: #d4e2de;
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

.compact-list {
  max-height: 280px;
  overflow: auto;
}

.compact-item {
  padding: 0.8rem 0.9rem;
}

.summary-row {
  cursor: pointer;
  transition:
    transform 0.15s ease,
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.summary-row:hover {
  transform: translateY(-1px);
  border-color: #c4d9d3;
}

.summary-row.active {
  border-color: #1f7a8c;
  background: #e7f3f1;
}

.intake-row {
  cursor: pointer;
  transition:
    transform 0.15s ease,
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.intake-row:hover {
  transform: translateY(-1px);
  border-color: #c4d9d3;
}

.intake-row.active {
  border-color: #1f7a8c;
  background: #e7f3f1;
}

.empty-state {
  padding: 1.2rem;
  border-radius: 16px;
  background: #f4f7f5;
  color: #698186;
  text-align: center;
}

.empty-state.compact {
  padding-top: 0.75rem;
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

@media (max-width: 1024px) {
  .hero,
  .layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .combined-grid {
    grid-template-columns: 1fr;
  }

  .card-header,
  .timeline-item header,
  .form-actions {
    flex-direction: column;
  }

  .chart {
    height: 300px;
  }
}
</style>
