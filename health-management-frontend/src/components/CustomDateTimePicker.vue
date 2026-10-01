<template>
  <div ref="rootRef" class="picker-root">
    <button class="picker-trigger" type="button" @click="toggleOpen">
      <span>{{ t(displayValue) }}</span>
      <span class="picker-icon">{{ t("TIME") }}</span>
    </button>

    <div v-if="isOpen" class="picker-popover">
      <div class="picker-toolbar">
        <button class="nav-button" type="button" @click="moveMonth(-1)">&lt;</button>
        <div class="toolbar-selects">
          <select v-model="displayMonth">
            <option v-for="month in months" :key="month.value" :value="month.value">
              {{ t(month.label) }}
            </option>
          </select>
          <select v-model="displayYear">
            <option v-for="year in yearOptions" :key="year" :value="String(year)">
              {{ t(year) }}
            </option>
          </select>
        </div>
        <button class="nav-button" type="button" @click="moveMonth(1)">&gt;</button>
      </div>

      <div class="weekday-row">
        <span v-for="day in weekdayLabels" :key="day">{{ t(day) }}</span>
      </div>

      <div class="calendar-grid">
        <button
          v-for="day in calendarDays"
          :key="day.key"
          class="day-cell"
          :class="{
            muted: !day.inCurrentMonth,
            selected: day.value === dateValue,
            today: day.isToday,
          }"
          type="button"
          @click="dateValue = day.value"
        >
          {{ t(day.label) }}
        </button>
      </div>

      <div class="time-panel">
        <div class="time-block">
          <span>{{ t("Hour") }}</span>
          <select v-model="hourValue">
            <option v-for="hour in 24" :key="hour - 1" :value="String(hour - 1).padStart(2, '0')">
              {{ t(String(hour - 1).padStart(2, '0')) }}
            </option>
          </select>
        </div>
        <div class="time-block">
          <span>{{ t("Minute") }}</span>
          <select v-model="minuteValue">
            <option v-for="minute in 60" :key="minute - 1" :value="String(minute - 1).padStart(2, '0')">
              {{ t(String(minute - 1).padStart(2, '0')) }}
            </option>
          </select>
        </div>
      </div>

      <div class="picker-footer">
        <button class="footer-button" type="button" @click="selectNow">{{ t("Now") }}</button>
        <div class="footer-actions">
          <button class="footer-button" type="button" @click="clearValue">{{ t("Clear") }}</button>
          <button class="footer-button primary" type="button" @click="applyValue">{{ t("Apply") }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { t, dateLocale } from '../i18n'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

type CalendarCell = {
  key: string
  label: number
  value: string
  inCurrentMonth: boolean
  isToday: boolean
}

const props = withDefaults(
  defineProps<{
    modelValue: string
    minDate?: string
    startYear?: number
    endYear?: number
  }>(),
  {
    minDate: '',
    startYear: new Date().getFullYear() - 5,
    endYear: new Date().getFullYear() + 10,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const months = [
  { value: '01', label: 'January' },
  { value: '02', label: 'February' },
  { value: '03', label: 'March' },
  { value: '04', label: 'April' },
  { value: '05', label: 'May' },
  { value: '06', label: 'June' },
  { value: '07', label: 'July' },
  { value: '08', label: 'August' },
  { value: '09', label: 'September' },
  { value: '10', label: 'October' },
  { value: '11', label: 'November' },
  { value: '12', label: 'December' },
]

const weekdayLabels = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']

const rootRef = ref<HTMLElement | null>(null)
const isOpen = ref(false)

const now = new Date()
const nowDateValue = formatDateValue(now.getFullYear(), now.getMonth() + 1, now.getDate())
const initialDate = parseDatePart(props.modelValue) ?? parseDatePart(props.minDate) ?? now
const displayYear = ref(String(initialDate.getFullYear()))
const displayMonth = ref(String(initialDate.getMonth() + 1).padStart(2, '0'))
const dateValue = ref(nowDateValue)
const hourValue = ref('12')
const minuteValue = ref('00')

const yearOptions = computed(() => {
  const years: number[] = []
  for (let year = props.startYear; year <= props.endYear; year += 1) {
    years.push(year)
  }
  return years
})

const displayValue = computed(() => {
  if (!props.modelValue) {
    return 'Select date and time'
  }

  const date = new Date(props.modelValue)
  return new Intl.DateTimeFormat(dateLocale(), {
    month: 'long',
    day: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
})

const calendarDays = computed<CalendarCell[]>(() => {
  const year = Number(displayYear.value)
  const month = Number(displayMonth.value)
  const firstDay = new Date(year, month - 1, 1)
  const startDayOfWeek = firstDay.getDay()
  const daysInCurrentMonth = new Date(year, month, 0).getDate()
  const daysInPrevMonth = new Date(year, month - 1, 0).getDate()
  const cells: CalendarCell[] = []

  for (let index = startDayOfWeek - 1; index >= 0; index -= 1) {
    const dayNumber = daysInPrevMonth - index
    const cellDate = new Date(year, month - 2, dayNumber)
    cells.push(createCell(cellDate, false))
  }

  for (let day = 1; day <= daysInCurrentMonth; day += 1) {
    const cellDate = new Date(year, month - 1, day)
    cells.push(createCell(cellDate, true))
  }

  const remaining = 42 - cells.length
  for (let day = 1; day <= remaining; day += 1) {
    const cellDate = new Date(year, month, day)
    cells.push(createCell(cellDate, false))
  }

  return cells
})

function formatDateValue(year: number, month: number, day: number) {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function parseDatePart(value: string) {
  const part = value.split('T')[0] ?? ''
  if (!part || !/^\d{4}-\d{2}-\d{2}$/.test(part)) {
    return null
  }

  const parts = part.split('-').map(Number)
  const year = parts[0]
  const month = parts[1]
  const day = parts[2]
  if (year === undefined || month === undefined || day === undefined) {
    return null
  }
  return new Date(year, month - 1, day)
}

function createCell(date: Date, inCurrentMonth: boolean): CalendarCell {
  const value = formatDateValue(date.getFullYear(), date.getMonth() + 1, date.getDate())
  return {
    key: `${value}-${inCurrentMonth ? 'current' : 'edge'}`,
    label: date.getDate(),
    value,
    inCurrentMonth,
    isToday: value === nowDateValue,
  }
}

function syncFromModel(value: string) {
  if (!value) {
    dateValue.value = nowDateValue
    hourValue.value = '12'
    minuteValue.value = '00'
    return
  }

  const [datePart, timePart = '12:00'] = value.split('T')
  const [hour = '12', minute = '00'] = timePart.split(':')
  dateValue.value = datePart ?? nowDateValue
  hourValue.value = hour
  minuteValue.value = minute

  const date = parseDatePart(dateValue.value)
  if (date) {
    displayYear.value = String(date.getFullYear())
    displayMonth.value = String(date.getMonth() + 1).padStart(2, '0')
  }
}

function toggleOpen() {
  isOpen.value = !isOpen.value
}

function closeOpen(event: MouseEvent) {
  if (!rootRef.value) {
    return
  }

  if (!rootRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

function moveMonth(offset: number) {
  const next = new Date(Number(displayYear.value), Number(displayMonth.value) - 1 + offset, 1)
  displayYear.value = String(next.getFullYear())
  displayMonth.value = String(next.getMonth() + 1).padStart(2, '0')
}

function applyValue() {
  emit('update:modelValue', `${dateValue.value}T${hourValue.value}:${minuteValue.value}`)
  isOpen.value = false
}

function clearValue() {
  emit('update:modelValue', '')
}

function selectNow() {
  const current = new Date()
  dateValue.value = formatDateValue(current.getFullYear(), current.getMonth() + 1, current.getDate())
  hourValue.value = String(current.getHours()).padStart(2, '0')
  minuteValue.value = String(current.getMinutes()).padStart(2, '0')
}

watch(
  () => props.modelValue,
  (value) => {
    syncFromModel(value)
  },
  { immediate: true },
)

onMounted(() => {
  document.addEventListener('mousedown', closeOpen)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', closeOpen)
})
</script>

<style scoped>
.picker-root {
  position: relative;
}

.picker-trigger {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #fff;
  color: #173f46;
  font: inherit;
  cursor: pointer;
}

.picker-icon {
  color: #5f7a7f;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.picker-popover {
  position: absolute;
  z-index: 20;
  top: calc(100% + 0.45rem);
  left: 0;
  width: min(100%, 24rem);
  padding: 0.9rem;
  border-radius: 18px;
  border: 1px solid #d6e2dc;
  background: #fffdf8;
  box-shadow: 0 22px 48px rgba(16, 46, 52, 0.18);
}

.picker-toolbar {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.6rem;
  align-items: center;
  margin-bottom: 0.75rem;
}

.toolbar-selects {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 0.5rem;
}

.toolbar-selects select,
.time-block select {
  width: 100%;
  padding: 0.55rem 0.7rem;
  border-radius: 12px;
  border: 1px solid #c9d6cf;
  background: #fff;
  color: #173f46;
  font: inherit;
}

.nav-button,
.footer-button,
.day-cell {
  font: inherit;
}

.nav-button,
.footer-button {
  border: 0;
  cursor: pointer;
}

.nav-button {
  width: 2.3rem;
  height: 2.3rem;
  border-radius: 999px;
  background: #eef5f3;
  color: #214f56;
}

.weekday-row,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0.3rem;
}

.weekday-row {
  margin-bottom: 0.45rem;
}

.weekday-row span {
  text-align: center;
  color: #61797d;
  font-size: 0.82rem;
  font-weight: 700;
}

.day-cell {
  height: 2.35rem;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #173f46;
  cursor: pointer;
}

.day-cell.muted {
  color: #9ab0b2;
}

.day-cell.today {
  box-shadow: inset 0 0 0 1px #84aeb5;
}

.day-cell.selected {
  background: linear-gradient(135deg, #1f7a8c, #3aa17e);
  color: #fff;
}

.time-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  margin-top: 0.85rem;
}

.time-block {
  display: grid;
  gap: 0.35rem;
}

.time-block span {
  color: #61797d;
  font-size: 0.82rem;
  font-weight: 700;
}

.picker-footer {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  margin-top: 0.85rem;
}

.footer-actions {
  display: flex;
  gap: 0.55rem;
}

.footer-button {
  padding: 0.45rem 0.7rem;
  border-radius: 10px;
  background: #eef5f3;
  color: #214f56;
}

.footer-button.primary {
  background: linear-gradient(135deg, #1f7a8c, #3aa17e);
  color: #fff;
}

@media (max-width: 720px) {
  .picker-popover {
    width: 100%;
  }

  .time-panel {
    grid-template-columns: 1fr;
  }

  .picker-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .footer-actions {
    justify-content: space-between;
  }
}
</style>
