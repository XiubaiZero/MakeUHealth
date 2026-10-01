import apiClient from './client'
import type { AuthRequest, AuthResponse } from './types'

export async function registerAuth(payload: AuthRequest) {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', payload)
  return data
}

export async function loginAuth(payload: AuthRequest) {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', payload)
  return data
}

