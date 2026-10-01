<template>
  <div class="onboarding-page">
    <section class="onboarding-hero">
      <p class="eyebrow">{{ t("Welcome") }}</p>
      <h1>{{ t("Complete your personal profile") }}</h1>
      <p class="hero-copy"> {{ t("This helps us provide personalized health tracking and recommendations. You can skip this step for now and complete it later.") }} </p>
    </section>

    <article class="onboarding-card">
      <h2>{{ t("Personal profile") }}</h2>

      <p v-if="statusMessage" class="status-banner">{{ t(statusMessage) }}</p>

      <form class="form-grid" @submit.prevent="handleSave">
        <label>
          <span>{{ t("Age") }}</span>
          <input v-model.number="form.age" min="1" type="number" :placeholder="t('age')" required />
        </label>
        <label>
          <span>{{ t("Gender") }}</span>
          <select v-model="form.gender" required>
            <option disabled value="">{{ t("gender") }}</option>
            <option value="male">{{ t("male") }}</option>
            <option value="female">{{ t("female") }}</option>
          </select>
        </label>
        <label>
          <span>{{ t("Height (cm)") }}</span>
          <input v-model.number="form.height" min="1" step="0.1" type="number" :placeholder="t('height')" required />
        </label>
        <label>
          <span>{{ t("Weight (kg)") }}</span>
          <input v-model.number="form.weight" min="1" step="0.1" type="number" :placeholder="t('weight')" required />
        </label>

        <div class="form-actions">
          <button class="primary-button" type="submit" :disabled="busy.saving">
            {{ t(busy.saving ? 'Saving...' : 'Save and continue') }}
          </button>
          <button class="secondary-button" type="button" :disabled="busy.saving" @click="handleSkip"> {{ t("Skip for now") }} </button>
        </div>
      </form>
    </article>
  </div>
</template>

<script setup lang="ts">
import { t } from '../i18n'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { createUser } from '../api/users'
import { getAccountId } from '../utils/auth'
import { setProfileComplete, setProfileOnboardingSkipped } from '../utils/profileOnboarding'

const route = useRoute()
const router = useRouter()

const statusMessage = ref('')
const busy = reactive({
  saving: false,
})

const form = reactive({
  age: null as number | null,
  gender: '' as '' | 'male' | 'female',
  height: null as number | null,
  weight: null as number | null,
})

function resolveRedirectPath() {
  return typeof route.query.redirect === 'string' && route.query.redirect.trim() ? route.query.redirect : '/health'
}

function setStatus(message: string) {
  statusMessage.value = message
}

async function handleSave() {
  if (form.age === null || form.height === null || form.weight === null || !form.gender) {
    setStatus('Please complete all profile fields.')
    return
  }

  busy.saving = true
  try {
    const saved = await createUser({
      age: Number(form.age),
      gender: form.gender,
      height: Number(form.height),
      weight: Number(form.weight),
    })

    if (!saved.id) {
      throw new Error('Failed to save profile.')
    }

    const accountId = getAccountId()
    setProfileComplete(accountId, true)
    setProfileOnboardingSkipped(accountId, false)
    await router.replace(resolveRedirectPath())
  } catch (error) {
    setStatus(error instanceof Error ? error.message : 'Failed to save profile.')
  } finally {
    busy.saving = false
  }
}

async function handleSkip() {
  const accountId = getAccountId()
  setProfileComplete(accountId, false)
  setProfileOnboardingSkipped(accountId, true)
  await router.replace(resolveRedirectPath())
}
</script>

<style scoped>
.onboarding-page {
  width: min(100%, 720px);
  display: grid;
  gap: 1rem;
}

.onboarding-hero,
.onboarding-card {
  padding: 1.4rem;
  border-radius: 22px;
  border: 1px solid rgba(18, 52, 59, 0.1);
  box-shadow: 0 18px 40px rgba(20, 55, 60, 0.08);
}

.onboarding-hero {
  background:
    radial-gradient(circle at left top, rgba(255, 209, 102, 0.35), transparent 40%),
    linear-gradient(135deg, #f2efe6 0%, #d7ebe7 45%, #c2dde4 100%);
}

.onboarding-card {
  background: #fffdf8;
}

.eyebrow {
  margin: 0 0 0.35rem;
  color: #8a5a44;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.75rem;
  font-weight: 700;
}

h1,
h2 {
  margin: 0;
  color: #12343b;
}

.hero-copy {
  margin: 0.8rem 0 0;
  color: #30565a;
  line-height: 1.65;
}

.status-banner {
  margin-top: 1rem;
  margin-bottom: 0;
  padding: 0.85rem 1rem;
  border-radius: 14px;
  background: #fff6d6;
  color: #7d5a00;
  border: 1px solid #f1d57a;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

label {
  display: grid;
  gap: 0.45rem;
}

label span {
  color: #34565d;
  font-size: 0.92rem;
  font-weight: 600;
}

input,
select {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #ffffff;
  color: #173f46;
  font: inherit;
}

input:focus,
select:focus {
  outline: 2px solid rgba(31, 122, 140, 0.18);
  border-color: #1f7a8c;
}

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 0.75rem;
}

.primary-button,
.secondary-button {
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

.primary-button:disabled,
.secondary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .onboarding-page {
    padding: 0 0.35rem;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
