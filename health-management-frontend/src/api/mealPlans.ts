import apiClient from './client'
import type { DailyMealTarget, FoodIntake, MealIntakeGroup, MealType, NutritionStats, User } from './types'

type ApiError = Error & {
    status?: number
}

function shouldFallbackToLegacyMealQuery(error: unknown) {
    const requestError = error as ApiError | undefined
    if (!requestError) {
        return false
    }
    return requestError.status === 404 || requestError.status === 405
}

function emptyMealGroups(): MealIntakeGroup[] {
    return [
        { mealType: 'breakfast', actualCalories: 0, items: [] },
        { mealType: 'lunch', actualCalories: 0, items: [] },
        { mealType: 'dinner', actualCalories: 0, items: [] },
        { mealType: 'snack', actualCalories: 0, items: [] },
    ]
}

export async function getDailyMealTarget(date: string) {
    const { data } = await apiClient.get<DailyMealTarget | null>('/meal-targets', {
        params: { date },
    })
    return data
}

export async function saveDailyMealTarget(payload: DailyMealTarget) {
    const { data } = await apiClient.post<DailyMealTarget>('/meal-targets', payload)
    return data
}

export async function getFoodIntakeByMeal(date: string) {
    try {
        const { data } = await apiClient.get<MealIntakeGroup[]>(`/food-intake/by-meal`, {
            params: { date },
        })
        return data
    } catch (error) {
        if (!shouldFallbackToLegacyMealQuery(error)) {
            throw error
        }

        const { data: users } = await apiClient.get<User[]>('/users')
        const currentUser = users.find((item) => typeof item.id === 'number')
        if (!currentUser?.id) {
            return emptyMealGroups()
        }

        const { data: intakes } = await apiClient.get<FoodIntake[]>(`/food-intake/user/${currentUser.id}`)
        const grouped = emptyMealGroups().reduce<Record<MealType, MealIntakeGroup>>((acc, item) => {
            acc[item.mealType] = item
            return acc
        }, {
            breakfast: { mealType: 'breakfast', actualCalories: 0, items: [] },
            lunch: { mealType: 'lunch', actualCalories: 0, items: [] },
            dinner: { mealType: 'dinner', actualCalories: 0, items: [] },
            snack: { mealType: 'snack', actualCalories: 0, items: [] },
        })

        intakes
            .filter((item) => item.intakeTime?.startsWith(date))
            .forEach((item) => {
                const type = (item.mealType || 'snack') as MealType
                const targetGroup = grouped[type] || grouped.snack
                targetGroup.items.push(item)
                targetGroup.actualCalories += Number(item.calories || 0)
            })

        return [grouped.breakfast, grouped.lunch, grouped.dinner, grouped.snack]
    }
}

export async function getDetailedNutritionStats(userId: number, type: 'day' | 'week' | 'month') {
    const { data } = await apiClient.get<NutritionStats[]>('/food-intake/stats/detailed', {
        params: { userId, type },
    })
    return data
}
