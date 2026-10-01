<template>
  <div class="meal-plan-page">
    <section class="hero">
      <div>
        <p class="eyebrow">{{ t("Meal planning") }}</p>
        <h1>{{ t("Plan every meal for fat loss") }}</h1>
        <p class="hero-copy"> {{ t("Set calorie targets for each meal, track actual intake, and stay within your daily budget.") }} </p>
      </div>
      <div class="today-summary">
        <article class="summary-card">
          <span>{{ t("Daily target") }}</span>
          <strong>{{ t(totalTarget) }} {{ t("kcal") }}</strong>
        </article>
        <article class="summary-card">
          <span>{{ t("Consumed") }}</span>
          <strong :class="totalActual > totalTarget ? 'over-budget' : 'on-budget'">
            {{ t(totalActual) }} {{ t("kcal") }} </strong>
        </article>
        <article class="summary-card">
          <span>{{ t("Remaining") }}</span>
          <strong>{{ t(totalRemaining) }} {{ t("kcal") }}</strong>
        </article>
      </div>
    </section>

    <p v-if="statusMessage" class="status-banner" :class="{ error: statusIsError }">
      {{ t(statusMessage) }}
    </p>

    <section class="card target-card">
      <div class="card-header">
        <div>
          <p class="eyebrow">{{ t("Daily budget") }}</p>
          <h2>{{ t(formatDate(todayStr)) }}</h2>
        </div>
        <button class="ghost-button" type="button" @click="showTargetForm = !showTargetForm">
          {{ t(showTargetForm ? 'Collapse' : 'Edit targets') }}
        </button>
      </div>

      <form v-if="showTargetForm" class="form-grid" @submit.prevent="handleSaveTargets">
        <label>
          <span>{{ t("Breakfast target (kcal)") }}</span>
          <input v-model.number="targetForm.breakfastTarget" min="0" type="number" required />
        </label>
        <label>
          <span>{{ t("Lunch target (kcal)") }}</span>
          <input v-model.number="targetForm.lunchTarget" min="0" type="number" required />
        </label>
        <label>
          <span>{{ t("Dinner target (kcal)") }}</span>
          <input v-model.number="targetForm.dinnerTarget" min="0" type="number" required />
        </label>
        <label>
          <span>{{ t("Snack target (kcal)") }}</span>
          <input v-model.number="targetForm.snackTarget" min="0" type="number" required />
        </label>
        <div class="form-actions full-width">
          <button class="primary-button" type="submit" :disabled="busy.targets">
            {{ t(busy.targets ? 'Saving...' : 'Save targets') }}
          </button>
        </div>
      </form>

      <div v-else class="target-display">
        <article v-for="m in mealTypeList" :key="m.value" class="target-pill">
          <span>{{ t(m.label) }}</span>
          <strong>{{ t((dailyTarget as any)?.[m.targetKey] ?? 0) }} {{ t("kcal") }}</strong>
        </article>
      </div>
    </section>

    <div class="meals-grid">
      <article v-for="meal in mealTypeList" :key="meal.value" class="card meal-card">
        <div class="meal-header">
          <div>
            <h3>{{ t(meal.label) }}</h3>
            <p class="meal-budget"> {{ t("Target") }} <strong>{{ t((dailyTarget as any)?.[meal.targetKey] ?? 0) }}</strong> {{ t("· Actual") }} <strong>{{ t(mealActuals[meal.value]) }}</strong> {{ t("· Left") }} <strong>{{ t(mealLeft(meal.value)) }}</strong>
            </p>
          </div>
          <div class="meal-progress">
            <div class="progress-bar">
              <div
                class="progress-fill"
                :class="{ over: mealActuals[meal.value] > ((dailyTarget as any)?.[meal.targetKey] ?? 0) }"
                :style="{ width: `${Math.min(mealPercent(meal.value), 100)}%` }"
              ></div>
            </div>
            <small>{{ t(mealPercent(meal.value)) }}%</small>
          </div>
        </div>

        <div class="meal-items">
          <div v-if="mealItems[meal.value].length === 0" class="empty-state compact">{{ t("No foods logged.") }}</div>
          <div v-else class="item-list">
            <div v-for="item in mealItems[meal.value]" :key="item.id" class="item-row">
              <span>{{ item.foodName }} {{ t(item.amount) }}{{ t(item.unit || 'g') }}</span>
              <span>{{ t(displayValue(item.calories)) }} {{ t("kcal") }}</span>
            </div>
          </div>
        </div>

        <form class="meal-add-form" @submit.prevent="handleAddFood(meal.value)">
          <label class="food-select">
            <span>{{ t("Food") }}</span>
            <select v-model="addForms[meal.value].foodName" required>
              <option value="" disabled>{{ t("Select food") }}</option>
              <option v-for="food in foodLibrary" :key="food.id" :value="food.foodName">
                {{ food.foodName }} ({{ t(food.calories) }} {{ t("kcal/100g)") }} </option>
            </select>
          </label>
          <label>
            <span>{{ t("Amount (g)") }}</span>
            <input v-model.number="addForms[meal.value].amount" min="1" type="number" required />
          </label>
          <label>
            <span>{{ t("Time") }}</span>
            <input type="datetime-local" v-model="addForms[meal.value].intakeTime" :max="maxIntakeTime" required />
          </label>
          <button class="primary-button" type="submit" :disabled="busy.add">{{ t("Add") }}</button>
        </form>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { t, dateLocale } from '../i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import { createFoodIntake, getFoodLibrary } from '../api/food'
import { getDailyMealTarget, getFoodIntakeByMeal, saveDailyMealTarget } from '../api/mealPlans'
import type { FoodIntake, FoodLibrary, MealIntakeGroup, MealType } from '../api/types'

const mealTypeList = [
  { value: 'breakfast' as MealType, label: 'Breakfast', targetKey: 'breakfastTarget' },
  { value: 'lunch' as MealType, label: 'Lunch', targetKey: 'lunchTarget' },
  { value: 'dinner' as MealType, label: 'Dinner', targetKey: 'dinnerTarget' },
  { value: 'snack' as MealType, label: 'Snack', targetKey: 'snackTarget' },
]

const foodLibrary = ref<FoodLibrary[]>([])
const todayIntakes = ref<FoodIntake[]>([])
const dailyTarget = ref<any>(null)
const showTargetForm = ref(false)
const statusMessage = ref('')
const statusIsError = ref(false)
const busy = reactive({ targets: false, add: false })

const todayStr = computed(() => {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
})

const targetForm = reactive({
  breakfastTarget: 400,
  lunchTarget: 600,
  dinnerTarget: 500,
  snackTarget: 200,
})

const addForms = reactive<Record<MealType, { foodName: string; amount: number; intakeTime: string }>>({
  breakfast: { foodName: '', amount: 100, intakeTime: '' },
  lunch: { foodName: '', amount: 100, intakeTime: '' },
  dinner: { foodName: '', amount: 100, intakeTime: '' },
  snack: { foodName: '', amount: 100, intakeTime: '' },
})

const maxIntakeTime = computed(() => {
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
})

const totalTarget = computed(() => {
  if (!dailyTarget.value) {
    return 0
  }
  return dailyTarget.value.breakfastTarget + dailyTarget.value.lunchTarget + dailyTarget.value.dinnerTarget + dailyTarget.value.snackTarget
})

const mealItems = computed(() => {
  const groups: Record<MealType, FoodIntake[]> = { breakfast: [], lunch: [], dinner: [], snack: [] }
  for (const item of todayIntakes.value) {
    const type = item.mealType || 'snack'
    groups[type].push(item)
  }
  return groups
})

const mealActuals = computed(() => {
  const totals: Record<MealType, number> = { breakfast: 0, lunch: 0, dinner: 0, snack: 0 }
  for (const [type, items] of Object.entries(mealItems.value)) {
    totals[type as MealType] = items.reduce((sum, item) => sum + Number(item.calories || 0), 0)
  }
  return totals
})

const totalActual = computed(() => Object.values(mealActuals.value).reduce((a, b) => a + b, 0))
const totalRemaining = computed(() => Math.max(0, totalTarget.value - totalActual.value))

function mealLeft(type: MealType) {
  const target = (dailyTarget.value?.[mealTypeList.find((m) => m.value === type)?.targetKey as string] as number) || 0
  return Math.max(0, target - mealActuals.value[type])
}

function mealPercent(type: MealType) {
  const target = (dailyTarget.value?.[mealTypeList.find((m) => m.value === type)?.targetKey as string] as number) || 0
  if (!target) {
    return 0
  }
  return Math.round((mealActuals.value[type] / target) * 100)
}

function setStatus(message: string, isError = false) {
  statusMessage.value = message
  statusIsError.value = isError
  setTimeout(() => {
    statusMessage.value = ''
  }, 3200)
}

function displayValue(value?: number | string) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  return new Intl.DateTimeFormat(dateLocale(), { year: 'numeric', month: 'short', day: '2-digit' }).format(new Date(value))
}

function toApiLocalDateTime(value: string) {
  if (!value) {
    return value
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
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

async function loadFoodLibraryData() {
  try {
    foodLibrary.value = await getFoodLibrary()
  } catch {
    setStatus('Failed to load food library', true)
  }
}

async function loadDailyTarget() {
  try {
    const data = await getDailyMealTarget(todayStr.value)
    if (data) {
      dailyTarget.value = data
      targetForm.breakfastTarget = data.breakfastTarget
      targetForm.lunchTarget = data.lunchTarget
      targetForm.dinnerTarget = data.dinnerTarget
      targetForm.snackTarget = data.snackTarget
    } else {
      dailyTarget.value = null
    }
  } catch {
    setStatus('Failed to load daily targets', true)
  }
}

async function loadTodayIntakes() {
  try {
    const grouped = await getFoodIntakeByMeal(todayStr.value)
    todayIntakes.value = grouped.flatMap((group: MealIntakeGroup) => group.items || [])
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to load food logs', true)
  }
}

async function handleSaveTargets() {
  busy.targets = true
  try {
    await saveDailyMealTarget({
      targetDate: todayStr.value,
      breakfastTarget: Number(targetForm.breakfastTarget),
      lunchTarget: Number(targetForm.lunchTarget),
      dinnerTarget: Number(targetForm.dinnerTarget),
      snackTarget: Number(targetForm.snackTarget),
    })
    showTargetForm.value = false
    await loadDailyTarget()
    setStatus('Targets saved.')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Save failed', true)
  } finally {
    busy.targets = false
  }
}

async function handleAddFood(mealType: MealType) {
  const form = addForms[mealType]
  if (!form.foodName || !form.intakeTime) {
    return
  }

  if (new Date(form.intakeTime) > new Date()) {
    setStatus('Intake time cannot be in the future', true)
    return
  }

  busy.add = true
  try {
    const food = foodLibrary.value.find((f) => f.foodName === form.foodName)
    const calories = food ? Math.round((food.calories * Number(form.amount)) / 100) : 0

    await createFoodIntake({
      foodName: form.foodName,
      amount: Number(form.amount),
      unit: 'g',
      calories,
      nutrients: food?.nutrients || '',
      intakeTime: toApiLocalDateTime(form.intakeTime),
      mealType,
    })

    form.foodName = ''
    form.amount = 100
    form.intakeTime = ''
    await loadTodayIntakes()
    setStatus(`${mealType} logged.`)
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to add food', true)
  } finally {
    busy.add = false
  }
}

onMounted(async () => {
  await loadFoodLibraryData()
  await loadDailyTarget()
  await loadTodayIntakes()
})
</script>

<style scoped>
.meal-plan-page { display: grid; gap: 1.5rem; }

.hero {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 24px;
  background:
    radial-gradient(circle at left top, rgba(255,209,102,0.35), transparent 35%),
    linear-gradient(135deg, #f2efe6 0%, #d7ebe7 42%, #c2dde4 100%);
  border: 1px solid rgba(31,122,140,0.12);
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
.card h2,
.card h3 {
  margin: 0;
  color: #12343b;
}

.hero-copy {
  max-width: 48rem;
  margin: 0.85rem 0 0;
  line-height: 1.65;
  color: #2d4d52;
}

.today-summary { display: grid; gap: 0.75rem; }

.summary-card {
  display: grid;
  gap: 0.35rem;
  padding: 1rem;
  border-radius: 18px;
  background: rgba(255,255,255,0.74);
  backdrop-filter: blur(8px);
  box-shadow: 0 14px 28px rgba(17,59,68,0.08);
}

.summary-card span { color: #4f6d73; font-size: 0.9rem; }
.summary-card strong { color: #12343b; font-size: 1.35rem; }
.over-budget { color: #c34242 !important; }
.on-budget { color: #2f855a !important; }

.status-banner {
  margin: 0;
  padding: 0.85rem 1rem;
  border-radius: 14px;
  background: #e9f6f1;
  color: #1f5f58;
  border: 1px solid #b7e1d2;
}

.status-banner.error {
  background: #ffeaea;
  color: #8f2f2f;
  border-color: #f3bcbc;
}

.card {
  padding: 1.25rem;
  border-radius: 22px;
  background: #fffdf8;
  border: 1px solid #e1e7df;
  box-shadow: 0 18px 40px rgba(20,55,60,0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.selector select {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #fff;
  color: #173f46;
  font: inherit;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.form-grid label { display: grid; gap: 0.45rem; }
.form-grid span { color: #34565d; font-size: 0.92rem; font-weight: 600; }

.form-grid input,
.meal-add-form input,
.meal-add-form select {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #fff;
  color: #173f46;
  font: inherit;
}

.full-width { grid-column: 1 / -1; }
.form-actions { display: flex; gap: 0.75rem; }

.primary-button,
.ghost-button {
  padding: 0.85rem 1.1rem;
  border: 0;
  border-radius: 14px;
  font: inherit;
  cursor: pointer;
}

.primary-button { background: linear-gradient(135deg, #1f7a8c, #3aa17e); color: #fff; }
.primary-button:disabled { opacity: 0.55; cursor: not-allowed; }
.ghost-button { background: #edf5f4; color: #24555f; }

.target-display { display: flex; flex-wrap: wrap; gap: 0.6rem; }

.target-pill {
  display: grid;
  gap: 0.25rem;
  padding: 0.7rem 1rem;
  border-radius: 14px;
  background: #f3f7f4;
  border: 1px solid #dbe8e4;
}

.target-pill span { color: #587176; font-size: 0.82rem; }
.target-pill strong { color: #183f46; font-size: 1.1rem; }

.meals-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
.meal-card { display: grid; gap: 0.9rem; }
.meal-header h3 { margin: 0; color: #1b4a52; font-size: 1.1rem; }
.meal-budget { margin: 0.35rem 0 0; color: #4f6d73; font-size: 0.88rem; }
.meal-budget strong { color: #183f46; }

.meal-progress { display: grid; gap: 0.35rem; margin-top: 0.4rem; }
.progress-bar { height: 10px; border-radius: 999px; background: #dce7e3; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 999px; background: linear-gradient(90deg, #1f7a8c, #3aa17e); }
.progress-fill.over { background: #e53e3e; }
.meal-progress small { text-align: right; color: #587176; font-size: 0.8rem; }

.item-list { display: grid; gap: 0.4rem; }
.item-row {
  display: flex;
  justify-content: space-between;
  padding: 0.55rem 0.7rem;
  border-radius: 10px;
  background: #f7faf8;
  border: 1px solid #e1ebe7;
  font-size: 0.9rem;
}

.item-row span:first-child { color: #35565d; }
.item-row span:last-child { color: #587176; font-weight: 600; }

.meal-add-form {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr auto;
  gap: 0.6rem;
  align-items: end;
}

.meal-add-form label { display: grid; gap: 0.35rem; }
.meal-add-form span { color: #34565d; font-size: 0.85rem; font-weight: 600; }

.empty-state.compact {
  padding: 0.8rem;
  border-radius: 12px;
  background: #f4f7f5;
  color: #698186;
  text-align: center;
  font-size: 0.9rem;
}

@media (max-width: 1024px) {
  .hero,
  .meals-grid {
    grid-template-columns: 1fr;
  }

  .meal-add-form {
    grid-template-columns: 1fr;
  }
}
</style>
