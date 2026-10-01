import { createRouter, createWebHistory } from 'vue-router'
import FitnessGoalView from '../views/FitnessGoalView.vue'
import HealthOverviewView from '../views/HealthOverviewView.vue'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import MealPlanView from '../views/MealPlanView.vue'
import ProfileOnboardingView from '../views/ProfileOnboardingView.vue'
import ReminderView from '../views/ReminderView.vue'
import RegisterView from '../views/RegisterView.vue'
import SmartAssistantView from '../views/SmartAssistantView.vue'
import { getAccountId, isAuthenticated } from '../utils/auth'
import {
  isProfileOnboardingSkipped,
  refreshProfileCompletion,
} from '../utils/profileOnboarding'

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      publicOnly: true,
    },
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: {
      publicOnly: true,
    },
  },
  {
    path: '/profile-onboarding',
    name: 'profile-onboarding',
    component: ProfileOnboardingView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/health',
    name: 'health',
    component: HealthOverviewView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/health-entry',
    name: 'health-entry',
    component: HomeView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/fitness-goals',
    name: 'fitness-goals',
    component: FitnessGoalView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/meal-plan',
    name: 'meal-plan',
    component: MealPlanView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/reminders',
    name: 'reminders',
    component: ReminderView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/smart-assistant',
    name: 'smart-assistant',
    component: SmartAssistantView,
    meta: {
      requiresAuth: true,
    },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const loggedIn = isAuthenticated()

  if (to.meta.requiresAuth && !loggedIn) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (to.meta.publicOnly && loggedIn) {
    const accountId = getAccountId()
    if (accountId) {
      const completion = await refreshProfileCompletion(accountId)
      if (!completion.complete && !isProfileOnboardingSkipped(accountId)) {
        return { name: 'profile-onboarding' }
      }
    }
    return {
      name: 'health',
    }
  }

  if (!loggedIn) {
    return true
  }

  const accountId = getAccountId()
  const onboardingRoute = to.name === 'profile-onboarding'
  if (!to.meta.requiresAuth && !onboardingRoute) {
    return true
  }

  if (!accountId) {
    return true
  }

  const skipped = isProfileOnboardingSkipped(accountId)
  const completion = await refreshProfileCompletion(accountId)

  if (onboardingRoute && completion.complete) {
    const redirectPath = typeof to.query.redirect === 'string' ? to.query.redirect : '/health'
    return { path: redirectPath }
  }

  if (!onboardingRoute && to.meta.requiresAuth && !completion.complete && !skipped) {
    return {
      name: 'profile-onboarding',
      query: { redirect: to.fullPath },
    }
  }

  return true
})

export default router
