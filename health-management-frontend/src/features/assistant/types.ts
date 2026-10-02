import type { GoalDashboardData, HealthRecord, User } from '../../api/types'

export type AssistantGoalSnapshot = {
  goalType: string
  status?: string | null
  currentValue?: number | string | null
  targetValue?: number | string | null
  weeklyChange?: number | string | null
  targetDate?: string | null
  latestProgressValue?: number | string | null
  latestProgressPercentage?: number | string | null
  remainingWeeks?: number | string | null
}

export type AssistantContext = {
  hasProfile: boolean
  age?: number | string
  gender?: string
  height?: number | string
  weight?: number | string
  latestRecordDate?: string
  latestSystolic?: number | string
  latestDiastolic?: number | string
  latestFbg?: number | string
  latestHeartRate?: number | string
  latestOxyhemoglobin?: number | string
  last7DaysFoodCount: number
  activeGoalType?: string
  activeGoalStatus?: string
  activeGoalCurrentValue?: number | string
  activeGoalTargetValue?: number | string
  activeGoalWeeklyChange?: number | string
  activeGoalTargetDate?: string
  latestProgressValue?: number | string
  latestProgressPercentage?: number | string
  remainingGoalWeeks?: number | string
  selectedGoalType?: string
  selectedGoalLabel?: string
  allGoalSnapshots?: AssistantGoalSnapshot[]
}

export type ChatMessage = {
  id: number
  role: 'assistant' | 'user'
  content: string
  suggestionPrompts?: string[]
}

export type GoalTypeValue = 'muscle_gain' | 'weight_loss' | 'fat_loss'

export type AssistantContextInput = {
  profile: User | null
  latestRecord: HealthRecord | null
  last7DaysFoodCount: number
  fitnessDashboards: Record<GoalTypeValue, GoalDashboardData>
  selectedGoalType: GoalTypeValue
  selectedGoalLabel: string
  goalTypeOptions: ReadonlyArray<{ value: GoalTypeValue; label: string }>
}
