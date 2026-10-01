export interface User {
  id?: number
  age: number | string
  gender: 'male' | 'female'
  height: number | string
  weight: number | string
  createdAt?: string
  updatedAt?: string
}

export type MealType = 'breakfast' | 'lunch' | 'dinner' | 'snack'

export interface HealthRecord {
  id?: number
  userId?: number
  systolic: number | string
  diastolic: number | string
  fbg?: number | string
  heartRate?: number | string
  oxyhemoglobin?: number | string
  ageSnapshot?: number | string
  genderSnapshot?: 'male' | 'female'
  heightSnapshot?: number | string
  weightSnapshot?: number | string
  recordedAt?: string
}

export interface FoodLibrary {
  id: number
  foodName: string
  calories: number
  nutrients: string
}

export interface FoodIntake {
  id?: number
  userId?: number
  foodName: string
  amount: number | string
  unit?: string
  calories?: number | string
  nutrients?: string
  intakeTime?: string
  mealType?: MealType
}

export interface NutritionStats {
  period: string
  calories: number
  nutrients: string
}

export interface FitnessGoal {
  id?: number
  userId?: number
  goalType: 'weight_loss' | 'muscle_gain' | 'fat_loss'
  currentValue: number
  targetValue: number
  targetDate: string
  weeklyChange?: number
  totalWeeks?: number
  status?: 'active' | 'completed' | 'archived'
  createdAt?: string
  updatedAt?: string | null
  completedAt?: string | null
}

export interface WeeklyProgress {
  id?: number
  userId?: number
  goalId?: number
  weekStart: string
  weekEnd: string
  currentValue: number
  weeklyChange: number
  progressPercentage: number
  isOnTrack: boolean
  createdAt?: string
}

export interface GoalDashboardData {
  activeGoal?: FitnessGoal | null
  latestProgress?: WeeklyProgress | null
  thisWeekProgress?: WeeklyProgress | null
  remainingWeeks?: number
}

export interface Reminder {
  id?: number
  accountId?: number
  userId?: number
  reminderType: 'medication' | 'meal' | 'exercise' | 'checkup' | 'sleep' | 'custom'
  reminderTime: string
  repeatPattern?: 'none' | 'daily' | 'weekly' | 'monthly'
  note?: string
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface DailyMealTarget {
  id?: number
  userId?: number
  targetDate: string
  breakfastTarget: number
  lunchTarget: number
  dinnerTarget: number
  snackTarget: number
}

export interface MealIntakeGroup {
  mealType: MealType
  actualCalories: number
  items: FoodIntake[]
}

export interface AuthRequest {
  account: string
  password: string
}

export interface AuthResponse {
  accountId?: number
  account: string
  accountType: 'email' | 'phone'
  token?: string
  message?: string
}
