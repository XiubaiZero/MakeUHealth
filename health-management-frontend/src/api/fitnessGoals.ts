import apiClient from './client'
import type { FitnessGoal, GoalDashboardData, WeeklyProgress } from './types'

export async function createGoal(payload: {
  goalType: string
  currentValue: number
  targetValue: number
  targetDate: string
}) {
  const { data } = await apiClient.post<FitnessGoal>('/fitness-goals', payload)
  return data
}

export async function calculateGoal(payload: {
  currentValue: number
  targetValue: number
  targetDate: string
}) {
  const { data } = await apiClient.post<{
    totalWeeks: number
    weeklyChange: number
    totalChange: number
  }>('/fitness-goals/calculate', payload)
  return data
}

export async function getDashboardDataByType(goalType: string) {
  const { data } = await apiClient.get<GoalDashboardData>(`/fitness-goals/dashboard/${goalType}`)
  return data
}

export async function getDashboardData() {
  const { data } = await apiClient.get<GoalDashboardData>('/fitness-goals/dashboard')
  return data
}

export async function getProgressByGoalType(goalType: string) {
  const { data } = await apiClient.get<WeeklyProgress[]>(`/fitness-goals/progress/${goalType}`)
  return data
}

export async function recordProgress(goalId: number, currentValue: number) {
  const { data } = await apiClient.post<WeeklyProgress>(`/fitness-goals/goal/${goalId}/progress`, {
    currentValue,
  })
  return data
}

export async function updateThisWeekProgress(goalId: number, currentValue: number) {
  const { data } = await apiClient.put<WeeklyProgress>(`/fitness-goals/goal/${goalId}/progress`, {
    currentValue,
  })
  return data
}
