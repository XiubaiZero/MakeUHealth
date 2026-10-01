<template>
  <div class="auth-page">
    <section class="auth-hero">
      <p class="eyebrow">{{ t("Secure access") }}</p>
      <h1>{{ t("Sign in to continue") }}</h1>
      <p class="hero-copy"> {{ t("Use your registered email or phone number and password to access the health input platform.") }} </p>
    </section>

    <article class="auth-card">
      <header>
        <p class="eyebrow">{{ t("Login") }}</p>
        <h2>{{ t("Welcome back") }}</h2>
      </header>

      <form class="auth-form" @submit.prevent="handleLogin">
        <label class="field">
          <span>{{ t("Account (Email or phone number)") }}</span>
          <input
            v-model="account"
            type="text"
            :placeholder="t('Enter your email or phone')"
            autocomplete="username"
            required
          />
          <small>{{ t("Accepted formats: email (name@example.com) or phone (+1234567890).") }}</small>
        </label>

        <label class="field">
          <span>{{ t("Password") }}</span>
          <div class="password-wrap">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              :placeholder="t('Enter your password')"
              autocomplete="current-password"
              required
            />
            <button
              type="button"
              class="visibility-toggle"
              :aria-label="t(showPassword ? 'Hide password' : 'Show password')"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M12 5c5.5 0 9.5 5 10 6-.5 1-4.5 6-10 6S2.5 12 2 11c.5-1 4.5-6 10-6Zm0 2c-3.6 0-6.6 2.7-7.7 4 .9 1.1 4 4 7.7 4s6.8-2.9 7.7-4c-1.1-1.3-4.1-4-7.7-4Zm0 2.2a1.8 1.8 0 1 1 0 3.6 1.8 1.8 0 0 1 0-3.6Z"
                />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M4.7 3.3 3.3 4.7l3 3C4.4 9.2 2.7 10.9 2 12c.5 1 4.5 6 10 6 2 0 3.7-.7 5.1-1.6l2.2 2.2 1.4-1.4L4.7 3.3Zm7.3 12.7c-3.6 0-6.6-2.7-7.7-4 .5-.6 1.6-1.8 3.1-2.8l1.7 1.7a3.8 3.8 0 0 0 4.9 4.9l1.7 1.7c-1.1.5-2.3.8-3.7.8Zm2.6-4.2a2.6 2.6 0 0 1-2.4 2.4l2.4-2.4Zm6.7-.8c-.5-1-4.5-6-10-6-.9 0-1.8.1-2.6.4l1.7 1.7c.3-.1.6-.1.9-.1 3.6 0 6.6 2.7 7.7 4-.4.4-1 .9-1.7 1.5l1.4 1.4A15.8 15.8 0 0 0 22 12c-.1-.2-.3-.5-.7-1Z"
                />
              </svg>
            </button>
          </div>
        </label>

        <button class="primary-button" type="submit">{{ t("Sign in") }}</button>

        <p class="switch-hint"> {{ t("New here?") }} <router-link to="/register">{{ t("Create an account") }}</router-link>
        </p>
      </form>
    </article>

    <Modal :show="showErrorModal" :message="errorMessage" @confirm="showErrorModal = false" />
  </div>
</template>

<script setup lang="ts">
import { t } from '../i18n'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import Modal from '../components/Modal.vue'
import { loginAccount } from '../utils/auth'

const router = useRouter()
const route = useRoute()

const account = ref('')
const password = ref('')
const showPassword = ref(false)
const showErrorModal = ref(false)
const errorMessage = ref('')

function openError(message: string) {
  errorMessage.value = message
  showErrorModal.value = true
}

async function handleLogin() {
  const result = await loginAccount(account.value, password.value)
  if (!result.success) {
    openError(result.message)
    return
  }

  const redirectPath = typeof route.query.redirect === 'string' ? route.query.redirect : '/health'
  await router.push(redirectPath)
}
</script>

<style scoped>
.auth-page {
  width: min(100%, 540px);
  display: grid;
  gap: 1rem;
}

.auth-hero,
.auth-card {
  width: min(100%, 540px);
  padding: 1.4rem;
  border-radius: 22px;
  border: 1px solid rgba(18, 52, 59, 0.1);
  box-shadow: 0 18px 40px rgba(20, 55, 60, 0.08);
}

.auth-hero {
  background:
    radial-gradient(circle at left top, rgba(255, 209, 102, 0.35), transparent 40%),
    linear-gradient(135deg, #f2efe6 0%, #d7ebe7 45%, #c2dde4 100%);
}

.auth-card {
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

.auth-form {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

.field {
  display: grid;
  gap: 0.45rem;
}

.field span {
  color: #34565d;
  font-size: 0.92rem;
  font-weight: 600;
}

.field small {
  color: #698186;
  font-size: 0.8rem;
}

input {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #ffffff;
  color: #173f46;
  font: inherit;
}

input:focus {
  outline: 2px solid rgba(31, 122, 140, 0.18);
  border-color: #1f7a8c;
}

.password-wrap {
  position: relative;
}

.password-wrap input {
  padding-right: 2.8rem;
}

.password-wrap input[type='password']::-ms-reveal,
.password-wrap input[type='password']::-ms-clear {
  display: none;
}

.password-wrap input[type='password']::-webkit-credentials-auto-fill-button {
  visibility: hidden;
  pointer-events: none;
  position: absolute;
  right: 0;
}

.visibility-toggle {
  position: absolute;
  top: 50%;
  right: 0.65rem;
  transform: translateY(-50%);
  border: 0;
  background: transparent;
  width: 1.7rem;
  height: 1.7rem;
  padding: 0;
  cursor: pointer;
  color: #335c63;
}

.visibility-toggle svg {
  width: 100%;
  height: 100%;
  fill: currentColor;
}

.primary-button {
  padding: 0.85rem 1.1rem;
  border: 0;
  border-radius: 14px;
  font: inherit;
  cursor: pointer;
  background: linear-gradient(135deg, #1f7a8c, #3aa17e);
  color: #fff;
}

.switch-hint {
  margin: 0;
  color: #587176;
  text-align: center;
}

.switch-hint a {
  color: #1f7a8c;
  font-weight: 600;
  text-decoration: none;
}

.switch-hint a:hover {
  text-decoration: underline;
}

@media (max-width: 720px) {
  .auth-page {
    padding: 0 0.35rem;
  }
}
</style>
