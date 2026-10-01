import { getUsers } from '../api/users'
import { getAccountId } from './auth'

const PROFILE_COMPLETE_PREFIX = 'profile-complete:account:'
const PROFILE_ONBOARDING_SKIPPED_PREFIX = 'profile-onboarding-skipped:account:'

function completeKey(accountId: number) {
  return `${PROFILE_COMPLETE_PREFIX}${accountId}`
}

function skippedKey(accountId: number) {
  return `${PROFILE_ONBOARDING_SKIPPED_PREFIX}${accountId}`
}

export function isProfileCompleteCached(accountId: number | null) {
  if (!accountId) {
    return false
  }
  return localStorage.getItem(completeKey(accountId)) === '1'
}

export function setProfileComplete(accountId: number | null, complete: boolean) {
  if (!accountId) {
    return
  }
  localStorage.setItem(completeKey(accountId), complete ? '1' : '0')
}

export function isProfileOnboardingSkipped(accountId: number | null) {
  if (!accountId) {
    return false
  }
  return localStorage.getItem(skippedKey(accountId)) === '1'
}

export function setProfileOnboardingSkipped(accountId: number | null, skipped: boolean) {
  if (!accountId) {
    return
  }
  localStorage.setItem(skippedKey(accountId), skipped ? '1' : '0')
}

export async function refreshProfileCompletion(accountIdInput?: number | null) {
  const accountId = accountIdInput ?? getAccountId()
  if (!accountId) {
    return { accountId: null, complete: false }
  }

  try {
    const users = await getUsers()
    const complete = users.length > 0
    setProfileComplete(accountId, complete)
    if (complete) {
      setProfileOnboardingSkipped(accountId, false)
    }
    return { accountId, complete }
  } catch {
    return { accountId, complete: isProfileCompleteCached(accountId) }
  }
}
