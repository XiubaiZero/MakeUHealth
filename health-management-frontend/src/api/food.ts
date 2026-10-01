import apiClient from './client'
import type { FoodIntake, FoodLibrary, NutritionStats } from './types'

type ApiError = Error & {
  status?: number
}

function shouldFallbackToPathStats(error: unknown) {
  const requestError = error as ApiError | undefined
  if (!requestError) {
    return false
  }
  if (requestError.status === 404 || requestError.status === 405 || requestError.status === 500) {
    return true
  }
  return requestError.message.toLowerCase().includes('network error')
}

export async function getFoodLibrary() {
  const { data } = await apiClient.get<FoodLibrary[]>('/food-library')
  return data
}

export async function getFoodLibraryByName(foodName: string) {
  const { data } = await apiClient.get<FoodLibrary>('/food-library/by-name', {
    params: { foodName },
  })
  return data
}

export async function searchFoodLibrary(keyword: string) {
  const { data } = await apiClient.get<FoodLibrary[]>('/food-library/search/keyword', {
    params: { keyword },
  })
  return data
}

export async function createFoodIntake(payload: FoodIntake) {
  const { data } = await apiClient.post<FoodIntake>('/food-intake', payload)
  return data
}

export async function getFoodIntakeByUserId(userId: number) {
  const { data } = await apiClient.get<FoodIntake[]>(`/food-intake/user/${userId}`)
  return data
}

export async function deleteFoodIntake(intakeId: number) {
  await apiClient.delete(`/food-intake/${intakeId}`)
}

export async function getNutritionStats(userId: number, type: 'day' | 'week' | 'month') {
  try {
    const { data } = await apiClient.get<NutritionStats[]>('/food-intake/stats', {
      params: { userId, type },
    })
    return data
  } catch (error) {
    if (!shouldFallbackToPathStats(error)) {
      throw error
    }
    const { data } = await apiClient.get<NutritionStats[]>(`/food-intake/stats/${userId}/${type}`)
    return data
  }
}

export async function getNutritionStatsByRange(
  userId: number,
  type: 'day' | 'week' | 'month',
  start: string,
  end: string,
) {
  const { data } = await apiClient.get<NutritionStats[]>(`/food-intake/stats/${userId}/${type}/range`, {
    params: { start, end },
  })
  return data
}

export async function getDetailedNutritionStats(userId: number, type: 'day' | 'week' | 'month') {
  const { data } = await apiClient.get<NutritionStats[]>('/food-intake/stats/detailed', {
    params: { userId, type },
  })
  return data
}
