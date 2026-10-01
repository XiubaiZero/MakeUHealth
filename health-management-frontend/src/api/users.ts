import apiClient from './client'
import type { User } from './types'

export async function getUsers() {
  const { data } = await apiClient.get<User[]>('/users')
  return data
}

export async function createUser(payload: User) {
  const { data } = await apiClient.post<User>('/users', payload)
  return data
}

export async function updateUser(userId: number, payload: User) {
  const { data } = await apiClient.put<User>(`/users/${userId}`, payload)
  return data
}
