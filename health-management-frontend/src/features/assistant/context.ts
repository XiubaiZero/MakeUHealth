import type { FoodIntake, HealthRecord } from '../../api/types'
import type { AssistantContext, AssistantContextInput } from './types'

export function countRecentFoodIntakes(foodIntakes: readonly FoodIntake[], now: number) {
  const sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000

  return foodIntakes.filter((item) => {
    if (!item.intakeTime) {
      return false
    }
    return new Date(item.intakeTime).getTime() >= sevenDaysAgo
  }).length
}

export function findLatestHealthRecord(healthRecords: readonly HealthRecord[]): HealthRecord | null {
  if (healthRecords.length === 0) {
    return null
  }

  const sorted = [...healthRecords].sort((a, b) => {
    const timeA = a.recordedAt ? new Date(a.recordedAt).getTime() : 0
    const timeB = b.recordedAt ? new Date(b.recordedAt).getTime() : 0
    return timeB - timeA
  })
  return sorted[0] ?? null
}

export function buildAssistantContext(input: AssistantContextInput): AssistantContext {
  const {
    profile, latestRecord, last7DaysFoodCount, fitnessDashboards,
    selectedGoalType, selectedGoalLabel, goalTypeOptions,
  } = input
  const currentDashboard = fitnessDashboards[selectedGoalType] ?? {}
  const currentGoal = currentDashboard.activeGoal ?? null
  const latestGoalProgress = currentDashboard.latestProgress ?? null

  return {
    hasProfile: Boolean(profile),
    age: profile?.age,
    gender: profile?.gender,
    height: profile?.height,
    weight: profile?.weight,
    latestRecordDate: latestRecord?.recordedAt,
    latestSystolic: latestRecord?.systolic,
    latestDiastolic: latestRecord?.diastolic,
    latestFbg: latestRecord?.fbg,
    latestHeartRate: latestRecord?.heartRate,
    latestOxyhemoglobin: latestRecord?.oxyhemoglobin,
    last7DaysFoodCount,
    activeGoalType: currentGoal?.goalType,
    activeGoalStatus: currentGoal?.status,
    activeGoalCurrentValue: currentGoal?.currentValue,
    activeGoalTargetValue: currentGoal?.targetValue,
    activeGoalWeeklyChange: currentGoal?.weeklyChange,
    activeGoalTargetDate: currentGoal?.targetDate,
    latestProgressValue: latestGoalProgress?.currentValue,
    latestProgressPercentage: latestGoalProgress?.progressPercentage,
    remainingGoalWeeks: currentDashboard.remainingWeeks,
    selectedGoalType,
    selectedGoalLabel,
    allGoalSnapshots: goalTypeOptions.map((item) => {
      const dashboard = fitnessDashboards[item.value]
      const goal = dashboard.activeGoal
      const progress = dashboard.latestProgress

      return {
        goalType: item.value,
        status: goal?.status || null,
        currentValue: goal?.currentValue ?? null,
        targetValue: goal?.targetValue ?? null,
        weeklyChange: goal?.weeklyChange ?? null,
        targetDate: goal?.targetDate ?? null,
        latestProgressValue: progress?.currentValue ?? null,
        latestProgressPercentage: progress?.progressPercentage ?? null,
        remainingWeeks: dashboard.remainingWeeks ?? null,
      }
    }),
  }
}
