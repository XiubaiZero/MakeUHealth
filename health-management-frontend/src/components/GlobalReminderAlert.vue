<template>
  <div v-if="currentTriggeredItem" class="reminder-modal-overlay">
    <div class="reminder-modal" @click.stop>
      <p class="eyebrow">{{ t("Reminder alert") }}</p>
      <h2>{{ t(getTypeLabel(currentTriggeredItem.reminder.reminderType)) }} {{ t("reminder triggered") }}</h2>
      <p class="modal-copy">
        {{ currentTriggeredItem.reminder.note || t("Your scheduled reminder is due now.") }}
      </p>
      <div class="modal-meta">
        <span>{{ t(formatDateTime(currentTriggeredItem.occurrenceTime)) }}</span>
        <span>{{ t(formatRepeat(currentTriggeredItem.reminder.repeatPattern)) }}</span>
      </div>
      <p v-if="confirmationError" role="alert">{{ t(confirmationError) }}</p>
      <div class="modal-actions">
        <button class="primary-button" type="button" :disabled="busyConfirming" @click="confirmTriggeredReminder">
          {{ t(busyConfirming ? 'Confirming...' : 'Confirm') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { t, dateLocale } from '../i18n'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { getMyReminders, updateReminder } from '../api/reminders'
import type { Reminder } from '../api/types'
import { getAccountId, isAuthenticated } from '../utils/auth'
import { getDueOccurrenceTime, parseReminderDateTime, reminderAcknowledgementStorageKey } from '../utils/reminderSchedule'

type TriggeredReminderItem = { occurrenceKey: string; occurrenceTime: string; reminder: Reminder }
const POLL_INTERVAL_MS = 15000
let activeAccountId = getAccountId()
let disposed = false
let loading = false
let pollTimer: number | null = null
const currentTriggeredItem = ref<TriggeredReminderItem | null>(null)
const triggeredQueue = ref<TriggeredReminderItem[]>([])
const pendingTriggerKeys = new Set<string>()
const acknowledgedTriggerMap = ref<Record<string, string>>(loadAcknowledgedTriggerMap())
const busyConfirming = ref(false)
const confirmationError = ref('')

function getTypeLabel(type?: Reminder['reminderType']) {
  const labels: Record<string, string> = {
    medication: 'Medication', meal: 'Meal', exercise: 'Exercise',
    checkup: 'Checkup', sleep: 'Sleep', custom: 'Custom',
  }
  return labels[type || 'custom'] || 'Custom'
}

function loadAcknowledgedTriggerMap(): Record<string, string> {
  if (activeAccountId === null) return {}
  try {
    const raw = localStorage.getItem(reminderAcknowledgementStorageKey(activeAccountId))
    const parsed: unknown = raw ? JSON.parse(raw) : null
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
    return Object.fromEntries(Object.entries(parsed).filter(([, value]) => typeof value === 'string'))
  } catch { return {} }
}

function formatDateTime(value?: string) {
  const parsed = parseReminderDateTime(value)
  if (!parsed) return 'Not set'
  return new Intl.DateTimeFormat(dateLocale(), {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(parsed)
}

function formatRepeat(value?: string) {
  return !value || value === 'none' ? 'No repeat' : value.charAt(0).toUpperCase() + value.slice(1)
}

function resetAccountState(accountId: number | null) {
  activeAccountId = accountId
  triggeredQueue.value = []
  currentTriggeredItem.value = null
  pendingTriggerKeys.clear()
  acknowledgedTriggerMap.value = loadAcknowledgedTriggerMap()
  confirmationError.value = ''
}

async function loadReminders() {
  const accountId = isAuthenticated() ? getAccountId() : null
  if (accountId !== activeAccountId) resetAccountState(accountId)
  if (disposed || accountId === null || loading) return
  loading = true
  try {
    const reminders = await getMyReminders()
    if (disposed || getAccountId() !== accountId || activeAccountId !== accountId) return
    const now = new Date()
    const dueItems: TriggeredReminderItem[] = reminders.flatMap((reminder) => {
      const occurrence = getDueOccurrenceTime(reminder, now)
      if (!occurrence || !reminder.id) return []
      const occurrenceTime = occurrence.toISOString()
      return [{ reminder, occurrenceTime, occurrenceKey: reminder.id + ':' + occurrenceTime }]
    })
    const validKeys = new Set(dueItems.map((item) => item.occurrenceKey))
    // Drop alerts disabled, deleted or rescheduled since the last poll.
    triggeredQueue.value = triggeredQueue.value.filter((item) => validKeys.has(item.occurrenceKey))
    if (!busyConfirming.value && currentTriggeredItem.value && !validKeys.has(currentTriggeredItem.value.occurrenceKey)) {
      currentTriggeredItem.value = null
      confirmationError.value = ''
    }
    pendingTriggerKeys.clear()
    for (const item of triggeredQueue.value) pendingTriggerKeys.add(item.occurrenceKey)
    if (currentTriggeredItem.value) pendingTriggerKeys.add(currentTriggeredItem.value.occurrenceKey)
    for (const item of dueItems) {
      if (acknowledgedTriggerMap.value[item.occurrenceKey] || pendingTriggerKeys.has(item.occurrenceKey)) continue
      pendingTriggerKeys.add(item.occurrenceKey)
      triggeredQueue.value.push(item)
    }
    triggeredQueue.value.sort((a, b) => a.occurrenceTime.localeCompare(b.occurrenceTime))
    if (!currentTriggeredItem.value) currentTriggeredItem.value = triggeredQueue.value.shift() ?? null
  } catch {
    // Retry on the next poll without displaying a banner on every page.
  } finally { loading = false }
}

async function confirmTriggeredReminder() {
  const currentItem = currentTriggeredItem.value
  const accountId = activeAccountId
  if (!currentItem || busyConfirming.value || accountId === null) return
  if (getAccountId() !== accountId) {
    resetAccountState(getAccountId())
    return
  }
  busyConfirming.value = true
  confirmationError.value = ''
  try {
    if ((currentItem.reminder.repeatPattern || 'none') === 'none' && currentItem.reminder.id) {
      await updateReminder(currentItem.reminder.id, { ...currentItem.reminder, enabled: false })
    }
    if (disposed || getAccountId() !== accountId || activeAccountId !== accountId) return
    // Persist only after the server update succeeds, so failures remain retryable.
    const acknowledged = { ...acknowledgedTriggerMap.value, [currentItem.occurrenceKey]: new Date().toISOString() }
    localStorage.setItem(reminderAcknowledgementStorageKey(accountId), JSON.stringify(acknowledged))
    acknowledgedTriggerMap.value = acknowledged
    pendingTriggerKeys.delete(currentItem.occurrenceKey)
    currentTriggeredItem.value = triggeredQueue.value.shift() ?? null
  } catch {
    if (!disposed && getAccountId() === accountId && activeAccountId === accountId) {
      confirmationError.value = 'Could not confirm this reminder. Please try again.'
    }
  } finally { busyConfirming.value = false }
}

function handleStorageChange() {
  if (getAccountId() !== activeAccountId) resetAccountState(getAccountId())
  acknowledgedTriggerMap.value = loadAcknowledgedTriggerMap()
  triggeredQueue.value = triggeredQueue.value.filter((item) => !acknowledgedTriggerMap.value[item.occurrenceKey])
  if (currentTriggeredItem.value && acknowledgedTriggerMap.value[currentTriggeredItem.value.occurrenceKey]) {
    pendingTriggerKeys.delete(currentTriggeredItem.value.occurrenceKey)
    currentTriggeredItem.value = triggeredQueue.value.shift() ?? null
  }
  void loadReminders()
}

onMounted(() => {
  window.addEventListener('storage', handleStorageChange)
  void loadReminders()
  pollTimer = window.setInterval(() => { void loadReminders() }, POLL_INTERVAL_MS)
})
onBeforeUnmount(() => {
  disposed = true
  window.removeEventListener('storage', handleStorageChange)
  if (pollTimer !== null) window.clearInterval(pollTimer)
})
</script>

<style scoped>
.reminder-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(14, 29, 32, 0.34);
  display: grid;
  place-items: center;
  padding: 1rem;
}

.reminder-modal {
  width: min(560px, 100%);
  background: #fffdf8;
  border: 1px solid #dce9e4;
  border-radius: 28px;
  padding: 1.6rem;
  box-shadow: 0 24px 54px rgba(20, 45, 49, 0.24);
}

.eyebrow {
  margin: 0 0 0.35rem;
  color: #8a5a44;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.75rem;
  font-weight: 700;
}

.reminder-modal h2 {
  margin: 0;
  color: #12343b;
}

.modal-copy {
  margin: 0.8rem 0 1rem;
  color: #4f6d73;
}

.modal-meta {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  border: 1px solid #d7e4df;
  border-radius: 18px;
  padding: 0.85rem 1rem;
  color: #486069;
  background: #f7fbf9;
}

.modal-actions {
  margin-top: 1rem;
  display: flex;
  justify-content: flex-end;
}

.primary-button {
  border: none;
  border-radius: 16px;
  padding: 0.82rem 1.2rem;
  font-weight: 700;
  background: linear-gradient(135deg, #4faaa4, #3d9089);
  color: #fff;
  cursor: pointer;
}

.primary-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
