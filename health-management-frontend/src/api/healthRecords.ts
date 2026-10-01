import apiClient from './client'
import type { HealthRecord } from './types'

export async function getHealthRecordsByUserId(userId: number) {
  const { data } = await apiClient.get<HealthRecord[]>(`/health-records/user/${userId}`)
  return data
}

export async function createHealthRecord(userId: number, payload: HealthRecord) {
  const { data } = await apiClient.post<HealthRecord>(`/health-records/user/${userId}`, payload)
  return data
}

export async function deleteHealthRecord(recordId: number) {
  await apiClient.delete(`/health-records/${recordId}`)
}
