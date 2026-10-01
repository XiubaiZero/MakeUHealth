import { loginAuth, registerAuth } from '../api/auth'

type AccountType = 'email' | 'phone'

type ValidationResult =
  | {
      valid: true
      normalized: string
      accountType: AccountType
    }
  | {
      valid: false
      message: string
    }

type AuthResult = {
  success: boolean
  message: string
}

type AuthSession = {
  accountId?: number
  account: string
  accountType: AccountType
  token: string
  loginAt: string
}

const AUTH_SESSION_KEY = 'health-management-auth-session'

function normalizePhone(value: string) {
  return value.replace(/[\s-]/g, '')
}

function parseJwtSubject(token: string) {
  try {
    const parts = token.split('.')
    if (parts.length < 2) {
      return null
    }
    const payload = JSON.parse(atob(parts[1] || '')) as { sub?: string }
    const accountId = Number(payload.sub)
    return Number.isFinite(accountId) ? accountId : null
  } catch {
    return null
  }
}

function parseSession(): AuthSession | null {
  const rawSession = localStorage.getItem(AUTH_SESSION_KEY)
  if (!rawSession) {
    return null
  }

  try {
    const parsed = JSON.parse(rawSession) as Partial<AuthSession>
    if (!parsed.account || !parsed.accountType || !parsed.token) {
      return null
    }

    return {
      account: parsed.account,
      accountId: parsed.accountId,
      accountType: parsed.accountType,
      token: parsed.token,
      loginAt: parsed.loginAt || new Date().toISOString(),
    }
  } catch {
    return null
  }
}

export function validateAccount(rawAccount: string): ValidationResult {
  const account = rawAccount.trim()
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  const loosePhonePattern = /^\+?[0-9][0-9\s-]{6,18}$/

  if (!account) {
    return {
      valid: false,
      message: 'Please enter your email or phone number.',
    }
  }

  if (emailPattern.test(account)) {
    return {
      valid: true,
      normalized: account.toLowerCase(),
      accountType: 'email',
    }
  }

  if (loosePhonePattern.test(account)) {
    const normalizedPhone = normalizePhone(account)
    if (/^\+?[0-9]{7,15}$/.test(normalizedPhone)) {
      return {
        valid: true,
        normalized: normalizedPhone,
        accountType: 'phone',
      }
    }
  }

  return {
    valid: false,
    message: 'Please use a valid email format or phone number format.',
  }
}

export async function registerAccount(rawAccount: string, password: string): Promise<AuthResult> {
  const validation = validateAccount(rawAccount)
  if (!validation.valid) {
    return {
      success: false,
      message: validation.message,
    }
  }

  if (password.length < 6) {
    return {
      success: false,
      message: 'Password must contain at least 6 characters.',
    }
  }

  try {
    const response = await registerAuth({
      account: rawAccount.trim(),
      password,
    })

    return {
      success: true,
      message: response.message || 'Registration successful.',
    }
  } catch (error) {
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Registration failed.',
    }
  }
}

export async function loginAccount(rawAccount: string, password: string): Promise<AuthResult> {
  const validation = validateAccount(rawAccount)
  if (!validation.valid) {
    return {
      success: false,
      message: validation.message,
    }
  }

  try {
    const response = await loginAuth({
      account: rawAccount.trim(),
      password,
    })

    const token = response.token || `token-${Date.now()}`
    const accountId = response.accountId ?? parseJwtSubject(token) ?? undefined
    const accountType = response.accountType ?? validation.accountType
    const account = response.account || validation.normalized
    localStorage.setItem(
      AUTH_SESSION_KEY,
      JSON.stringify({
        accountId,
        account,
        accountType,
        token,
        loginAt: new Date().toISOString(),
      } satisfies AuthSession),
    )

    return {
      success: true,
      message: response.message || 'Login successful.',
    }
  } catch (error) {
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Login failed.',
    }
  }
}

export function isAuthenticated() {
  return parseSession() !== null
}

export function getAuthToken() {
  return parseSession()?.token || null
}

export function getAccountId() {
  return parseSession()?.accountId || null
}

export function getAuthAccountId() {
  return getAccountId()
}

export function getAuthStorageScope() {
  const session = parseSession()
  if (!session) {
    return 'guest'
  }

  if (session.accountId) {
    return `account-id-${session.accountId}`
  }

  return `account-${session.accountType}-${session.account.toLowerCase()}`
}

export function clearAuthSession() {
  localStorage.removeItem(AUTH_SESSION_KEY)
}
