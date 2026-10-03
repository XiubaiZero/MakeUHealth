<template>
  <div id="app">
    <div v-if="showNavigation" class="app-shell">
      <aside class="sidebar">
        <div class="sidebar-brand">
          <span>{{ t("HMS") }}</span>
        </div>

        <nav class="sidebar-nav">
          <router-link class="sidebar-link" :class="{ active: route.path === '/health' }" to="/health">
            <span class="link-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path d="M12 3 3 10.2V21h6.5v-5.5h5V21H21V10.2L12 3Z" />
              </svg>
            </span>
            <span class="link-label">{{ t("Overview") }}</span>
          </router-link>

          <router-link class="sidebar-link" :class="{ active: route.path === '/health-entry' }" to="/health-entry">
            <span class="link-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path d="M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Zm2 4h8v2H8V8Zm0 4h5v2H8v-2Z" />
              </svg>
            </span>
            <span class="link-label">{{ t("Entry") }}</span>
          </router-link>

          <router-link class="sidebar-link" :class="{ active: route.path === '/meal-plan' }" to="/meal-plan">
            <span class="link-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path d="M4 5h16v2H4V5Zm2 4h12v2H6V9Zm-1 4h14v2H5v-2Zm2 4h10v2H7v-2Z" />
              </svg>
            </span>
            <span class="link-label">{{ t("Meal Plan") }}</span>
          </router-link>

          <router-link
            class="sidebar-link"
            :class="{ active: route.path === '/fitness-goals' }"
            to="/fitness-goals"
          >
            <span class="link-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path
                  d="M7 4a2 2 0 1 0 0 4h1v8H7a2 2 0 1 0 0 4h2.2a2 2 0 0 0 2-2V8h1.6v10a2 2 0 0 0 2 2H17a2 2 0 1 0 0-4h-1V8h1a2 2 0 1 0 0-4h-2.2a2 2 0 0 0-2 2v1H11V6a2 2 0 0 0-2-2H7Z"
                />
              </svg>
            </span>
            <span class="link-label">{{ t("Fitness") }}</span>
          </router-link>

          <router-link
            class="sidebar-link"
            :class="{ active: route.path === '/reminders' }"
            to="/reminders"
          >
            <span class="link-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path
                  d="M12 3a6 6 0 0 0-6 6v3.3L4.4 16A1.4 1.4 0 0 0 5.7 18h12.6a1.4 1.4 0 0 0 1.3-2L18 12.3V9a6 6 0 0 0-6-6Zm0 19a3 3 0 0 0 2.8-2h-5.6A3 3 0 0 0 12 22Z"
                />
              </svg>
            </span>
            <span class="link-label">{{ t("Reminders") }}</span>
          </router-link>

          <router-link
            class="sidebar-link"
            :class="{ active: route.path === '/smart-assistant' }"
            to="/smart-assistant"
          >
            <span class="link-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path
                  d="M7 3a3 3 0 0 0-3 3v8a3 3 0 0 0 3 3h2.2l-.6 2.5a1 1 0 0 0 1.6 1L14 17h3a3 3 0 0 0 3-3V6a3 3 0 0 0-3-3H7Zm2.3 6.8a1.2 1.2 0 1 1 0 2.4 1.2 1.2 0 0 1 0-2.4Zm5.4 0a1.2 1.2 0 1 1 0 2.4 1.2 1.2 0 0 1 0-2.4Z"
                />
              </svg>
            </span>
            <span class="link-label">{{ t("Assistant") }}</span>
          </router-link>
        </nav>

        <button type="button" class="settings-button" :aria-label="t('Open settings')" aria-haspopup="dialog" :aria-expanded="showSettings" @click="showSettings = true">
          <span class="link-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path
                d="M19.4 13a7.7 7.7 0 0 0 0-2l2-1.5-2-3.5-2.3.9a7.7 7.7 0 0 0-1.7-1L15 3h-4l-.4 2.9a7.7 7.7 0 0 0-1.7 1L6.6 6l-2 3.5 2 1.5a7.7 7.7 0 0 0 0 2l-2 1.5 2 3.5 2.3-.9a7.7 7.7 0 0 0 1.7 1L11 21h4l.4-2.9a7.7 7.7 0 0 0 1.7-1l2.3.9 2-3.5-2-1.5ZM13 16a4 4 0 1 1 0-8 4 4 0 0 1 0 8Z"
              />
            </svg>
          </span>
          <span class="link-label">{{ t("Settings") }}</span>
        </button>
      </aside>

      <main class="app-main">
        <router-view />
      </main>
    </div>

    <main v-else class="app-main auth-main">
      <router-view />
    </main>

    <button v-if="!showNavigation" class="auth-settings-button" type="button" :aria-label="t('Open settings')" aria-haspopup="dialog" :aria-expanded="showSettings" @click="showSettings = true">{{ t("文 / A") }}</button>
    <SettingsDialog :open="showSettings" :authenticated="loggedIn" :signing-out="signingOut" :assistant-settings-visible="route.name === 'smart-assistant'" @close="showSettings = false" @logout="handleLogout" />
    <GlobalReminderAlert v-if="loggedIn" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import GlobalReminderAlert from './components/GlobalReminderAlert.vue'
import SettingsDialog from './components/SettingsDialog.vue'
import { assistantPreferences } from './features/assistant/preferences'
import { t } from './i18n'
import { clearAuthSession, isAuthenticated } from './utils/auth'

const route = useRoute()
const router = useRouter()
const loggedIn = ref(false)
const showSettings = ref(false)
const signingOut = ref(false)

const showNavigation = computed(() => {
  const fullScreenPage = route.name === 'login' || route.name === 'register' || route.name === 'profile-onboarding'
  return loggedIn.value && !fullScreenPage
})

watch(
  () => route.fullPath,
  () => {
    loggedIn.value = isAuthenticated()
    assistantPreferences.checkScope()
  },
  { immediate: true },
)

async function handleLogout() {
  if (signingOut.value) return
  signingOut.value = true
  try {
    clearAuthSession()
    assistantPreferences.reset()
    loggedIn.value = false
    showSettings.value = false
    await router.push('/login')
  } finally { signingOut.value = false }
}
</script>

<style>
#app {
  min-height: 100vh;
  background: var(--light-bg);
}

.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  height: 100dvh;
  align-self: start;
  padding: 18px 10px 14px;
  border-top-right-radius: 36px;
  border-bottom-right-radius: 36px;
  background: linear-gradient(180deg, #54bcbd 0%, #50b1b7 48%, #5bbec4 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  box-shadow: 6px 0 24px rgba(24, 67, 75, 0.12);
}

.sidebar-brand {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.28);
  display: grid;
  place-items: center;
  color: #ffffff;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.sidebar-nav {
  width: 100%;
  display: grid;
  gap: 10px;
  margin-top: 8px;
}

.sidebar-link,
.settings-button {
  width: 100%;
  border: 0;
  background: transparent;
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 12px 8px;
  border-radius: 14px;
  color: rgba(241, 251, 252, 0.58);
  cursor: pointer;
  transform: translateY(0);
  transition: color 0.2s ease, background-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
  text-decoration: none;
  font: inherit;
  appearance: none;
}

.sidebar-link:hover,
.sidebar-link.active,
.settings-button:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-3px);
  box-shadow: 0 8px 16px rgba(21, 69, 77, 0.16);
}

.settings-button {
  margin-top: auto;
  margin-bottom: 6px;
  line-height: 1.2;
  width: 64px;
  padding: 10px 6px;
  color: #fff;
  background: rgba(255, 255, 255, .16);
}

.auth-settings-button { position: fixed; z-index: 10; right: 20px; top: 20px; border: 1px solid #d4e4dc; border-radius: 12px; padding: 10px 14px; background: #fffdf8; color: #347b75; font-weight: 700; cursor: pointer; box-shadow: 0 4px 16px rgba(20,45,49,.07); }
.settings-button:focus-visible, .auth-settings-button:focus-visible { outline: 3px solid #12343b; outline-offset: 3px; }

.link-icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  color: currentColor;
}

.link-icon svg {
  width: 100%;
  height: 100%;
  fill: currentColor;
}

.link-label {
  font-size: 0.74rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-align: center;
}

.app-main {
  min-width: 0;
  padding: 20px;
}

.auth-main {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--light-bg);
}

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .sidebar {
    border-radius: 0 28px 28px 0;
  }

  .link-label {
    font-size: 0.68rem;
  }
}

@media (max-width: 760px) {
  .app-shell {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .sidebar {
    padding: max(12px, env(safe-area-inset-top)) 6px max(10px, env(safe-area-inset-bottom));
    border-radius: 0 24px 24px 0;
    gap: 12px;
  }

  .sidebar-brand {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
    border-radius: 12px;
    font-size: 0.8rem;
  }

  .sidebar-nav {
    flex: 1;
    min-height: 0;
    margin-top: 0;
    gap: 6px;
    align-content: start;
    overflow-y: auto;
    overscroll-behavior: contain;
    scrollbar-width: thin;
  }

  .sidebar-link,
  .settings-button {
    min-height: 54px;
    padding: 9px 3px;
    gap: 5px;
    border-radius: 12px;
  }

  .sidebar-link.active {
    transform: none;
  }

  .settings-button {
    flex-shrink: 0;
    width: 100%;
    margin: 0;
  }

  .link-icon {
    width: 18px;
    height: 18px;
  }

  .link-label {
    font-size: 0.625rem;
    line-height: 1.35;
    letter-spacing: 0;
    overflow-wrap: anywhere;
  }

  .app-main {
    padding: 12px 10px;
  }

  .app-main input,
  .app-main select,
  .app-main textarea {
    min-width: 0;
    max-width: 100%;
  }

  .auth-main {
    padding: 20px;
  }
}

@media (hover: none) {
  .sidebar-link:hover,
  .settings-button:hover {
    transform: none;
  }
}

@media (max-height: 700px) {
  .sidebar-brand,
  .settings-button {
    flex-shrink: 0;
  }

  .sidebar-nav {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    align-content: start;
    overscroll-behavior: contain;
    scrollbar-width: thin;
  }
}
</style>
