<template>
  <div ref="rootRef" class="picker-root">
    <button class="picker-trigger" type="button" @click="toggleOpen">
      <span>{{ t(displayValue) }}</span>
      <span class="picker-icon">{{ t("CAL") }}</span>
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
            selected: day.value === modelValue,
            disabled: day.disabled,
            today: day.isToday,
          }"
          type="button"
          :disabled="day.disabled"
          @click="selectDay(day.value)"
        >
          {{ t(day.label) }}
        </button>
      </div>

      <div class="picker-footer">
        <button class="footer-button" type="button" @click="selectToday">{{ t("Today") }}</button>
        <button class="footer-button" type="button" @click="clearValue">{{ t("Clear") }}</button>
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
  disabled: boolean
  isToday: boolean
}

const props = withDefaults(
  defineProps<{
    modelValue: string
    min?: string
    startYear?: number
    endYear?: number
  }>(),
  {
    min: '',
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

const today = new Date()
const todayValue = formatDateValue(today.getFullYear(), today.getMonth() + 1, today.getDate())

const initialDate = parseDate(props.modelValue) ?? parseDate(props.min) ?? today
const displayYear = ref(String(initialDate.getFullYear()))
const displayMonth = ref(String(initialDate.getMonth() + 1).padStart(2, '0'))

const yearOptions = computed(() => {
  const years: number[] = []
  for (let year = props.startYear; year <= props.endYear; year += 1) {
    years.push(year)
  }
  return years
})

const displayValue = computed(() => {
  if (!props.modelValue) {
    return 'Select date'
  }

  const date = parseDate(props.modelValue)
  if (!date) {
    return 'Select date'
  }

  return new Intl.DateTimeFormat(dateLocale(), {
    month: 'long',
    day: '2-digit',
    year: 'numeric',
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

function parseDate(value: string) {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return null
  }

  const parts = value.split('-').map(Number)
  const year = parts[0]
  const month = parts[1]
  const day = parts[2]
  if (year === undefined || month === undefined || day === undefined) {
    return null
  }
  return new Date(year, month - 1, day)
}

function formatDateValue(year: number, month: number, day: number) {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function createCell(date: Date, inCurrentMonth: boolean): CalendarCell {
  const value = formatDateValue(date.getFullYear(), date.getMonth() + 1, date.getDate())
  return {
    key: `${value}-${inCurrentMonth ? 'current' : 'edge'}`,
    label: date.getDate(),
    value,
    inCurrentMonth,
    disabled: Boolean(props.min) && value < props.min,
    isToday: value === todayValue,
  }
}

function syncDisplayDate(value: string) {
  const date = parseDate(value) ?? parseDate(props.min) ?? today
  displayYear.value = String(date.getFullYear())
  displayMonth.value = String(date.getMonth() + 1).padStart(2, '0')
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

function selectDay(value: string) {
  emit('update:modelValue', value)
  isOpen.value = false
}

function selectToday() {
  if (props.min && todayValue < props.min) {
    emit('update:modelValue', props.min)
    syncDisplayDate(props.min)
    return
  }

  emit('update:modelValue', todayValue)
  syncDisplayDate(todayValue)
  isOpen.value = false
}

function clearValue() {
  emit('update:modelValue', '')
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      syncDisplayDate(value)
    }
  },
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
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.picker-popover {
  position: absolute;
  z-index: 20;
  top: calc(100% + 0.45rem);
  left: 0;
  width: min(100%, 22rem);
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
.nav-button,
.footer-button,
.day-cell {
  font: inherit;
}

.toolbar-selects select {
  width: 100%;
  padding: 0.55rem 0.7rem;
  border-radius: 12px;
  border: 1px solid #c9d6cf;
  background: #fff;
  color: #173f46;
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

.day-cell.disabled {
  color: #c8d4d5;
  cursor: not-allowed;
}

.picker-footer {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.footer-button {
  padding: 0.45rem 0.7rem;
  border-radius: 10px;
  background: #eef5f3;
  color: #214f56;
}
@media (max-width: 720px) {
  .picker-popover {
    width: 100%;
  }
}
</style>
