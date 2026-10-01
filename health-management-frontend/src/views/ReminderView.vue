<template>
  <div class="reminder-page">
    <section class="hero">
      <div>
        <p class="eyebrow">{{ t("Reminders") }}</p>
        <h1>{{ t("Your account, your reminder feed") }}</h1>
        <p class="hero-copy"> {{ t("This page only manages reminders for the currently signed-in account. When a reminder becomes due, the page will open a confirmation popup for the user.") }} </p>
      </div>
      <div class="summary-grid">
        <article>
          <span>{{ t("Total") }}</span>
          <strong>{{ t(reminders.length) }}</strong>
        </article>
        <article>
          <span>{{ t("Enabled") }}</span>
          <strong>{{ t(enabledCount) }}</strong>
        </article>
      </div>
    </section>

    <p v-if="statusMessage" class="status-banner">{{ t(statusMessage) }}</p>

    <div class="layout">
      <section class="stack">
        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t(editingReminderId ? 'Edit reminder' : 'Create reminder') }}</p>
              <h2>{{ t(editingReminderId ? 'Update reminder' : 'New reminder') }}</h2>
            </div>
          </div>

          <form class="form-grid" @submit.prevent="handleSaveReminder">
            <label>
              <span>{{ t("Reminder type") }}</span>
              <select v-model="reminderForm.reminderType">
                <option v-for="item in reminderTypes" :key="item.value" :value="item.value">
                  {{ t(item.label) }}
                </option>
              </select>
            </label>
            <label>
              <span>{{ t("Repeat") }}</span>
              <select v-model="reminderForm.repeatPattern">
                <option value="none">{{ t("None") }}</option>
                <option value="daily">{{ t("Daily") }}</option>
                <option value="weekly">{{ t("Weekly") }}</option>
                <option value="monthly">{{ t("Monthly") }}</option>
              </select>
            </label>
            <label class="full-width">
              <span>{{ t("Reminder time") }}</span>
              <CustomDateTimePicker v-model="reminderForm.reminderTime" />
            </label>
            <label class="full-width">
              <span>{{ t("Note") }}</span>
              <textarea v-model.trim="reminderForm.note" rows="4" :placeholder="t('Optional reminder note')"></textarea>
            </label>

            <div class="form-actions full-width">
              <button class="primary-button" type="submit" :disabled="busy.saving || !canSaveReminder">
                {{ t(busy.saving ? 'Saving...' : editingReminderId ? 'Update reminder' : 'Create reminder') }}
              </button>
              <button class="secondary-button" type="button" @click="resetReminderForm">{{ t("Reset") }}</button>
            </div>
          </form>
        </article>
      </section>

      <aside class="stack">
        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Schedule") }}</p>
              <h2>{{ t("Reminder list") }}</h2>
            </div>
            <div class="header-actions">
              <button class="ghost-button" type="button" :disabled="!canManage" @click="loadReminders()"> {{ t("Reload") }} </button>
              <button
                class="danger-button"
                type="button"
                :disabled="busy.deleting || reminders.length === 0"
                @click="handleClearReminders"
              > {{ t("Clear") }} </button>
            </div>
          </div>

          <div v-if="!canManage" class="empty-state">{{ t("Please sign in again to manage reminders.") }}</div>
          <div v-else-if="busy.reminders" class="empty-state">{{ t("Loading reminders...") }}</div>
          <div v-else-if="reminders.length === 0" class="empty-state">{{ t("No reminders yet.") }}</div>
          <div v-else class="reminder-list">
            <article
              v-for="reminder in reminders"
              :key="reminder.id"
              class="reminder-item"
              :class="{ disabled: !reminder.enabled, selected: selectedReminderId === reminder.id }"
            >
              <header>
                <div>
                  <span class="type-badge">{{ t(getTypeMeta(reminder.reminderType).label) }}</span>
                  <strong>{{ t(formatDateTime(reminder.reminderTime)) }}</strong>
                </div>
                <div class="item-actions">
                  <button class="icon-button" type="button" :title="t('Edit')" @click="startEdit(reminder)">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="m5 16.9-.8 3 3-.8L17.6 8.7l-2.2-2.2L5 16.9Zm14-9.6 1.1-1.1a1.5 1.5 0 0 0 0-2.1l-.2-.2a1.5 1.5 0 0 0-2.1 0l-1.1 1.1L19 7.3Z" />
                    </svg>
                  </button>
                  <button class="delete-icon-button" type="button" :title="t('Delete')" @click="handleDeleteReminder(reminder.id)">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M9 3h6l1 2h4v2H4V5h4l1-2Zm1 7h2v8h-2v-8Zm4 0h2v8h-2v-8ZM7 10h2v8H7v-8Zm-1 10h12l1-13H5l1 13Z" />
                    </svg>
                  </button>
                </div>
              </header>
              <p>{{ reminder.note || t("No note") }}</p>
              <footer>
                <span>{{ t(formatRepeat(reminder.repeatPattern)) }}</span>
                <button
                  class="switch-button"
                  :class="{ on: reminder.enabled !== false }"
                  type="button"
                  :disabled="isToggling(reminder.id)"
                  :aria-label="t('Toggle reminder')"
                  @click="toggleReminder(reminder)"
                >
                  <span class="switch-thumb"></span>
                </button>
              </footer>
            </article>
          </div>
        </article>

        <article class="card">
          <div class="card-header">
            <div>
              <p class="eyebrow">{{ t("Overview") }}</p>
              <h2>{{ t("By type") }}</h2>
            </div>
          </div>

          <div class="type-summary">
            <article v-for="item in reminderTypes" :key="item.value">
              <span>{{ t(item.label) }}</span>
              <strong>{{ t(countByType(item.value)) }}</strong>
            </article>
          </div>
        </article>
      </aside>
    </div>

  </div>
</template>

<script setup lang="ts">
import { t, dateLocale } from '../i18n'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { clearMyReminders, createReminder, deleteReminder, getMyReminders, updateReminder } from '../api/reminders'
import type { Reminder } from '../api/types'
import CustomDateTimePicker from '../components/CustomDateTimePicker.vue'
import { getAccountId } from '../utils/auth'

type ReminderType = Reminder['reminderType']
type RepeatPattern = NonNullable<Reminder['repeatPattern']>

const POLL_INTERVAL_MS = 15000

const reminderTypes = [
  { value: 'medication' as ReminderType, label: 'Medication' },
  { value: 'meal' as ReminderType, label: 'Meal' },
  { value: 'exercise' as ReminderType, label: 'Exercise' },
  { value: 'checkup' as ReminderType, label: 'Checkup' },
  { value: 'sleep' as ReminderType, label: 'Sleep' },
  { value: 'custom' as ReminderType, label: 'Custom' },
]

const route = useRoute()
const accountId = ref<number | null>(getAccountId())
const reminders = ref<Reminder[]>([])
const editingReminderId = ref<number | null>(null)
const selectedReminderId = ref<number | null>(null)
const statusMessage = ref('')

const busy = reactive({
  reminders: false,
  saving: false,
  deleting: false,
})

const reminderForm = reactive({
  reminderType: 'medication' as ReminderType,
  reminderTime: '',
  repeatPattern: 'none' as RepeatPattern,
  note: '',
  enabled: true,
})

const togglingIds = ref<number[]>([])
let pollTimer: number | null = null

const canManage = computed(() => accountId.value !== null)
const enabledCount = computed(() => reminders.value.filter((item) => item.enabled !== false).length)
const canSaveReminder = computed(() => Boolean(canManage.value && reminderForm.reminderTime))

function setStatus(message: string) {
  statusMessage.value = message
  window.setTimeout(() => {
    if (statusMessage.value === message) {
      statusMessage.value = ''
    }
  }, 3000)
}

function getTypeMeta(type: ReminderType) {
  return reminderTypes.find((item) => item.value === type) ?? reminderTypes[0]!
}

function parseReminderDateTime(value?: string) {
  if (!value) {
    return null
  }

  const hasTimezone = /(?:Z|[+-]\d{2}:\d{2})$/.test(value)
  if (hasTimezone) {
    const zonedDate = new Date(value)
    return Number.isNaN(zonedDate.getTime()) ? null : zonedDate
  }

  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/)
  if (!match) {
    const parsedDate = new Date(value)
    return Number.isNaN(parsedDate.getTime()) ? null : parsedDate
  }

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const hour = Number(match[4])
  const minute = Number(match[5])
  const second = Number(match[6] ?? '0')
  return new Date(year, month - 1, day, hour, minute, second, 0)
}

function toApiLocalDateTime(value: string) {
  const parsed = parseReminderDateTime(value)
  if (!parsed) {
    return value
  }

  const year = parsed.getFullYear()
  const month = String(parsed.getMonth() + 1).padStart(2, '0')
  const day = String(parsed.getDate()).padStart(2, '0')
  const hour = String(parsed.getHours()).padStart(2, '0')
  const minute = String(parsed.getMinutes()).padStart(2, '0')
  const second = String(parsed.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

function formatDateTime(value?: string) {
  const parsed = parseReminderDateTime(value)
  if (!parsed) {
    return 'Not set'
  }

  return new Intl.DateTimeFormat(dateLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(parsed)
}

function formatRepeat(value?: string) {
  if (!value || value === 'none') {
    return 'No repeat'
  }

  return value.charAt(0).toUpperCase() + value.slice(1)
}

function toLocalDateTimeValue(value?: string) {
  const parsed = parseReminderDateTime(value)
  if (!parsed) {
    return ''
  }

  const year = parsed.getFullYear()
  const month = String(parsed.getMonth() + 1).padStart(2, '0')
  const day = String(parsed.getDate()).padStart(2, '0')
  const hour = String(parsed.getHours()).padStart(2, '0')
  const minute = String(parsed.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}`
}

function resetReminderForm() {
  editingReminderId.value = null
  reminderForm.reminderType = 'medication'
  reminderForm.reminderTime = ''
  reminderForm.repeatPattern = 'none'
  reminderForm.note = ''
  reminderForm.enabled = true
}

function startEdit(reminder: Reminder) {
  editingReminderId.value = reminder.id ?? null
  selectedReminderId.value = reminder.id ?? null
  reminderForm.reminderType = reminder.reminderType
  reminderForm.reminderTime = toLocalDateTimeValue(reminder.reminderTime)
  reminderForm.repeatPattern = reminder.repeatPattern || 'none'
  reminderForm.note = reminder.note || ''
  reminderForm.enabled = reminder.enabled !== false
}

function parseReminderIdQuery(value: unknown) {
  if (typeof value !== 'string') {
    return null
  }
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

function applyReminderSelectionFromQuery() {
  const reminderId = parseReminderIdQuery(route.query.reminderId)
  if (!reminderId) {
    selectedReminderId.value = null
    return
  }

  selectedReminderId.value = reminderId
  const matchedReminder = reminders.value.find((item) => item.id === reminderId)
  if (matchedReminder) {
    startEdit(matchedReminder)
  }
}

function countByType(type: ReminderType) {
  return reminders.value.filter((item) => item.reminderType === type).length
}

function toPayload(): Reminder {
  return {
    reminderType: reminderForm.reminderType,
    reminderTime: toApiLocalDateTime(reminderForm.reminderTime),
    repeatPattern: reminderForm.repeatPattern,
    note: reminderForm.note,
    enabled: reminderForm.enabled,
  }
}

function isToggling(reminderId?: number) {
  if (!reminderId) {
    return false
  }

  return togglingIds.value.includes(reminderId)
}

function markToggling(reminderId: number) {
  if (!togglingIds.value.includes(reminderId)) {
    togglingIds.value = [...togglingIds.value, reminderId]
  }
}

function unmarkToggling(reminderId: number) {
  togglingIds.value = togglingIds.value.filter((item) => item !== reminderId)
}


async function loadReminders(showLoading = true) {
  if (!canManage.value) {
    reminders.value = []
    return
  }

  if (showLoading) {
    busy.reminders = true
  }

  try {
    reminders.value = await getMyReminders()
    applyReminderSelectionFromQuery()
  } catch (error) {
    if (showLoading) {
      setStatus(error instanceof Error ? error.message : 'Failed to load reminders')
    }
  } finally {
    if (showLoading) {
      busy.reminders = false
    }
  }
}

async function handleSaveReminder() {
  if (!canManage.value) {
    setStatus('Please sign in again to manage reminders.')
    return
  }

  busy.saving = true
  try {
    const payload = toPayload()
    if (editingReminderId.value) {
      await updateReminder(editingReminderId.value, payload)
      setStatus('Reminder updated successfully.')
    } else {
      await createReminder(payload)
      setStatus('Reminder created successfully.')
    }
    resetReminderForm()
    await loadReminders(false)
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to save reminder')
  } finally {
    busy.saving = false
  }
}

async function toggleReminder(reminder: Reminder) {
  if (!reminder.id) {
    return
  }

  const previousEnabled = reminder.enabled !== false
  reminder.enabled = !previousEnabled
  markToggling(reminder.id)

  try {
    await updateReminder(reminder.id, {
      ...reminder,
      enabled: reminder.enabled,
    })
  } catch (error) {
    reminder.enabled = previousEnabled
    setStatus(error instanceof Error ? error.message : 'Failed to update reminder')
  } finally {
    unmarkToggling(reminder.id)
  }
}

async function handleDeleteReminder(reminderId?: number) {
  if (!reminderId) {
    setStatus('Invalid reminder ID.')
    return
  }

  busy.deleting = true
  try {
    await deleteReminder(reminderId)
    reminders.value = reminders.value.filter((item) => item.id !== reminderId)
    setStatus('Reminder deleted successfully.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to delete reminder')
  } finally {
    busy.deleting = false
  }
}

async function handleClearReminders() {
  if (!canManage.value) {
    return
  }

  busy.deleting = true
  try {
    await clearMyReminders()
    reminders.value = []
    resetReminderForm()
    setStatus('All reminders cleared.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to clear reminders')
  } finally {
    busy.deleting = false
  }
}

function startPolling() {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
  }

  pollTimer = window.setInterval(() => {
    void loadReminders(false)
  }, POLL_INTERVAL_MS)
}

function stopPolling() {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(async () => {
  await loadReminders()
  startPolling()
})

watch(
  () => route.query.reminderId,
  () => {
    applyReminderSelectionFromQuery()
  },
)

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.reminder-page {
  display: grid;
  gap: 1.5rem;
}

.hero {
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 24px;
  background:
    radial-gradient(circle at right top, rgba(255, 209, 102, 0.28), transparent 30%),
    linear-gradient(135deg, #f3efe5 0%, #d8ece6 48%, #c8e2e9 100%);
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
.card h2,
.reminder-modal h2 {
  margin: 0;
  color: #12343b;
}

.hero-copy {
  margin: 0.8rem 0 0;
  max-width: 44rem;
  color: #30565a;
  line-height: 1.65;
}

.summary-grid,
.type-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.summary-grid article,
.type-summary article {
  display: grid;
  gap: 0.35rem;
  padding: 1rem;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid #dde8e2;
}

.summary-grid span,
.type-summary span {
  color: #4f6d73;
  font-size: 0.9rem;
}

.summary-grid strong,
.type-summary strong {
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
  grid-template-columns: minmax(0, 1fr) minmax(340px, 1.1fr);
  gap: 1.5rem;
}

.stack {
  display: grid;
  gap: 1rem;
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

.header-actions {
  display: flex;
  gap: 0.55rem;
  flex-wrap: wrap;
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
.form-grid textarea {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #ffffff;
  color: #173f46;
  font: inherit;
}

.form-grid textarea {
  resize: none;
  min-height: 7rem;
}

.form-grid input:focus,
.form-grid select:focus,
.form-grid textarea:focus {
  outline: 2px solid rgba(31, 122, 140, 0.18);
  border-color: #1f7a8c;
}

.full-width {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
}

.primary-button,
.secondary-button,
.ghost-button,
.danger-button {
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

.danger-button {
  background: #f4e2df;
  color: #9b2c20;
}

.primary-button:disabled,
.ghost-button:disabled,
.danger-button:disabled,
.switch-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.switch-button {
  width: 54px;
  min-width: 54px;
  height: 30px;
  border: 0;
  border-radius: 999px;
  background: #cfd8d9;
  padding: 3px;
  position: relative;
  transition: background-color 0.22s ease;
  cursor: pointer;
}

.switch-button.on {
  background: linear-gradient(135deg, #1f7a8c, #3aa17e);
}

.switch-thumb {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ffffff;
  display: block;
  box-shadow: 0 4px 12px rgba(16, 46, 52, 0.2);
  transform: translateX(0);
  transition: transform 0.22s ease;
}

.switch-button.on .switch-thumb {
  transform: translateX(24px);
}

.empty-state {
  padding: 1.15rem;
  border-radius: 16px;
  background: #f4f7f5;
  color: #698186;
  text-align: center;
}

.reminder-list {
  display: grid;
  gap: 0.75rem;
  max-height: 620px;
  overflow: auto;
}

.reminder-item {
  display: grid;
  gap: 0.55rem;
  padding: 1rem;
  border-radius: 16px;
  background: #f7faf8;
  border: 1px solid #dde8e2;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.reminder-item.disabled {
  opacity: 0.62;
}

.reminder-item.selected {
  border-color: #1f7a8c;
  background: #e7f3f1;
}

.reminder-item header,
.reminder-item footer {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
}

.reminder-item header > div:first-child {
  display: grid;
  gap: 0.3rem;
}

.reminder-item strong {
  color: #183f46;
}

.reminder-item p,
.reminder-item footer {
  margin: 0;
  color: #587176;
}

.type-badge {
  width: fit-content;
  padding: 0.25rem 0.55rem;
  border-radius: 999px;
  background: #e5f2ef;
  color: #24555f;
  font-size: 0.78rem;
  font-weight: 700;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.icon-button,
.delete-icon-button {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #5f7a7f;
  background: #eef3f4;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-button svg,
.delete-icon-button svg {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

.icon-button:hover {
  color: #ffffff;
  background: #1f7a8c;
  transform: translateY(-1px);
}

.delete-icon-button:hover {
  color: #ffffff;
  background: #bd3124;
  transform: translateY(-1px);
}

.reminder-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(10, 24, 28, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  z-index: 2000;
}

.reminder-modal {
  width: min(92vw, 440px);
  display: grid;
  gap: 0.9rem;
  padding: 1.4rem;
  border-radius: 22px;
  background: #fffdf8;
  border: 1px solid #e1e7df;
  box-shadow: 0 22px 52px rgba(10, 24, 28, 0.2);
}

.modal-copy,
.modal-meta span {
  color: #587176;
}

.modal-copy {
  margin: 0;
  line-height: 1.6;
}

.modal-meta {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.9rem 1rem;
  border-radius: 16px;
  background: #f7faf8;
  border: 1px solid #dde8e2;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .hero,
  .layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .summary-grid,
  .type-summary,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .card-header,
  .form-actions,
  .reminder-item header,
  .reminder-item footer,
  .modal-meta {
    flex-direction: column;
  }

  .modal-actions {
    justify-content: stretch;
  }

  .modal-actions .primary-button {
    width: 100%;
  }
}
</style>
