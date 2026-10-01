import apiClient from './client'
import type { Reminder } from './types'
import { getAccountId } from '../utils/auth'

type ApiError = Error & {
  status?: number
}

function isMethodNotAllowed(error: unknown) {
  return (error as ApiError | undefined)?.status === 405
}

export async function createReminder(payload: Reminder) {
  const { data } = await apiClient.post<Reminder>('/reminders', payload)
  return data
}

export async function getMyReminders() {
  try {
    const { data } = await apiClient.get<Reminder[]>('/reminders')
    return data
  } catch (error) {
    if (!isMethodNotAllowed(error)) {
      throw error
    }
    const accountId = getAccountId()
    if (!accountId) {
      throw error
    }
    const { data } = await apiClient.get<Reminder[]>(`/reminders/user/${accountId}`)
    return data
  }
}

export async function updateReminder(reminderId: number, payload: Reminder) {
  const { data } = await apiClient.put<Reminder>(`/reminders/${reminderId}`, payload)
  return data
}

export async function deleteReminder(reminderId: number) {
  await apiClient.delete(`/reminders/${reminderId}`)
}

export async function clearMyReminders() {
  try {
    await apiClient.delete('/reminders')
  } catch (error) {
    if (!isMethodNotAllowed(error)) {
      throw error
    }
    const accountId = getAccountId()
    if (!accountId) {
      throw error
    }
    await apiClient.delete(`/reminders/user/${accountId}`)
  }
}
