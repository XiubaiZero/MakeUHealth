import apiClient from './client'
import { locale, t } from '../i18n'

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

const SCOPE_KEYWORDS_EN = [
  'health',
  'body',
  'diet',
  'nutrition',
  'food',
  'meal',
  'weight',
  'blood pressure',
  'heart',
  'sleep',
  'exercise',
  'calorie',
  'protein',
  'fat',
  'carb',
  'bmi',
  'sugar',
  'glucose',
  'hydration',
  'fitness',
  'workout',
  'training',
  'gym',
  'strength',
  'cardio',
  'muscle',
  'muscle gain',
  'fat loss',
  'fat reduction',
  'weight loss',
  'exercise plan',
  'fitness plan',
  'daily routine',
]

const SCOPE_KEYWORDS_ZH = [
  '\u5065\u5eb7',
  '\u8eab\u4f53',
  '\u996e\u98df',
  '\u8425\u517b',
  '\u4f53\u91cd',
  '\u8840\u538b',
  '\u5fc3\u7387',
  '\u8840\u7cd6',
  '\u7761\u7720',
  '\u8fd0\u52a8',
  '\u953b\u70bc',
  '\u5065\u8eab',
  '\u51cf\u8102',
  '\u589e\u808c',
  '\u4f53\u8102',
  '\u5851\u5f62',
  '\u8bad\u7ec3',
  '\u4f5c\u606f',
  '\u4ee3\u8c22',
  '\u996e\u98df\u8ba1\u5212',
  '\u8bad\u7ec3\u8ba1\u5212',
  '\u5065\u8eab\u8ba1\u5212',
]

const CAPABILITY_HINTS_EN = [
  'what can you do',
  'how can you help',
  'what can you help me with',
  'your capabilities',
  'your functions',
  'what do you support',
  'what can i ask',
]

const CAPABILITY_HINTS_ZH = [
  '\u4f60\u80fd\u505a\u4ec0\u4e48',
  '\u4f60\u53ef\u4ee5\u505a\u4ec0\u4e48',
  '\u80fd\u5e2e\u6211\u4ec0\u4e48',
  '\u4f60\u6709\u4ec0\u4e48\u529f\u80fd',
  '\u80fd\u4e3a\u6211\u505a\u4e9b\u4ec0\u4e48',
  '\u4f60\u652f\u6301\u4ec0\u4e48',
]

const CAPABILITY_REGEX_EN = [
  /\bwhat\b[\s\S]{0,30}\bcan\b[\s\S]{0,30}\byou\b[\s\S]{0,30}\b(do|help|support)\b/,
  /\bhow\b[\s\S]{0,30}\bcan\b[\s\S]{0,30}\byou\b[\s\S]{0,30}\bhelp\b/,
  /\bwhat\b[\s\S]{0,30}\bcan\b[\s\S]{0,30}\bi\b[\s\S]{0,30}\bask\b[\s\S]{0,30}\byou\b/,
]

const CAPABILITY_OBJECT_MARKERS_ZH = [
  '\u505a\u4ec0\u4e48',
  '\u505a\u4e9b\u4ec0\u4e48',
  '\u505a\u4ec0\u4e48\u4e8b',
  '\u505a\u54ea\u4e9b',
  '\u54ea\u4e9b\u529f\u80fd',
  '\u4ec0\u4e48\u529f\u80fd',
  '\u6709\u4ec0\u4e48\u529f\u80fd',
  '\u652f\u6301\u4ec0\u4e48',
  '\u652f\u6301\u54ea\u4e9b',
  '\u80fd\u529b',
  '\u53ef\u4ee5\u95ee\u4ec0\u4e48',
  '\u95ee\u4f60\u4ec0\u4e48',
  '\u80fd\u5e2e\u6211\u4ec0\u4e48',
  '\u4e3a\u6211\u505a\u4ec0\u4e48',
  '\u4e3a\u6211\u505a\u4e9b\u4ec0\u4e48',
]

function isHealthScopeQuestion(question: string) {
  const normalized = question.toLowerCase()
  const matchedEnglish = SCOPE_KEYWORDS_EN.some((keyword) => normalized.includes(keyword))
  if (matchedEnglish) {
    return true
  }
  return SCOPE_KEYWORDS_ZH.some((keyword) => question.includes(keyword))
}

function isCapabilityQuestion(question: string) {
  const normalized = question.toLowerCase()
  const matchedEnglish = CAPABILITY_HINTS_EN.some((keyword) => normalized.includes(keyword))
  if (matchedEnglish) {
    return true
  }
  const matchedRegexEnglish = CAPABILITY_REGEX_EN.some((pattern) => pattern.test(normalized))
  if (matchedRegexEnglish) {
    return true
  }

  const normalizedZh = question.replace(/[\s，。？！、,.!?；;：:]/g, '')
  const matchedZhHint = CAPABILITY_HINTS_ZH.some((keyword) => normalizedZh.includes(keyword))
  if (matchedZhHint) {
    return true
  }

  const hasSubject = normalizedZh.includes('\u4f60')
  const hasAbilityVerb =
    normalizedZh.includes('\u80fd') ||
    normalizedZh.includes('\u53ef\u4ee5') ||
    normalizedZh.includes('\u4f1a') ||
    normalizedZh.includes('\u652f\u6301')
  const hasCapabilityObject = CAPABILITY_OBJECT_MARKERS_ZH.some((marker) => normalizedZh.includes(marker))

  return hasSubject && hasAbilityVerb && hasCapabilityObject
}

const CAPABILITY_EXTRA_HINTS_EN = [
  'can you help me',
  'could you help me',
  'what features do you have',
  'what functions do you have',
  'what are your capabilities',
  'how can i use you',
  'how to use you',
]

const CAPABILITY_EXTRA_HINTS_ZH = [
  '\u4f60\u80fd\u4e3a\u6211\u505a\u4ec0\u4e48',
  '\u4f60\u53ef\u4ee5\u4e3a\u6211\u505a\u4ec0\u4e48',
  '\u4f60\u80fd\u5e2e\u6211\u505a\u4ec0\u4e48',
  '\u4f60\u53ef\u4ee5\u5e2e\u6211\u505a\u4ec0\u4e48',
  '\u4f60\u53ef\u4ee5\u600e\u4e48\u5e2e\u6211',
  '\u6211\u53ef\u4ee5\u95ee\u4f60\u4ec0\u4e48',
  '\u80fd\u95ee\u4f60\u4ec0\u4e48',
  '\u4f60\u6709\u54ea\u4e9b\u529f\u80fd',
  '\u4f60\u6709\u54ea\u4e9b\u80fd\u529b',
  '\u4f60\u652f\u6301\u54ea\u4e9b',
]

const CAPABILITY_EXTRA_REGEX_EN = [
  /\b(can|could)\b[\s\S]{0,20}\byou\b[\s\S]{0,20}\b(help|assist|support)\b[\s\S]{0,20}\bme\b/,
  /\bwhat\b[\s\S]{0,30}\b(features?|functions?|capabilities)\b[\s\S]{0,30}\bdo\b[\s\S]{0,30}\byou\b[\s\S]{0,30}\b(have|support)\b/,
]

const CAPABILITY_EXTRA_REGEX_ZH = [
  /(?:\u4f60|\u60a8|ai|\u52a9\u624b|\u667a\u80fd\u52a9\u624b)[\s\S]{0,12}(?:\u80fd|\u53ef\u4ee5|\u4f1a|\u652f\u6301|\u5e2e)[\s\S]{0,20}(?:\u505a\u4ec0\u4e48|\u505a\u54ea\u4e9b|\u4ec0\u4e48\u529f\u80fd|\u54ea\u4e9b\u529f\u80fd|\u4ec0\u4e48\u80fd\u529b|\u652f\u6301\u4ec0\u4e48|\u53ef\u4ee5\u95ee\u4ec0\u4e48)/,
  /(?:\u80fd|\u53ef\u4ee5|\u4f1a)[\s\S]{0,6}(?:\u4e3a\u6211|\u5e2e\u6211)[\s\S]{0,12}(?:\u505a\u4ec0\u4e48|\u505a\u54ea\u4e9b|\u63d0\u4f9b\u4ec0\u4e48)/,
]

const CAPABILITY_EXTRA_SUBJECT_MARKERS_ZH = [
  '\u4f60',
  '\u60a8',
  'ai',
  '\u52a9\u624b',
  '\u667a\u80fd\u52a9\u624b',
]

const CAPABILITY_EXTRA_SELF_REF_MARKERS_ZH = [
  '\u4e3a\u6211',
  '\u5e2e\u6211',
  '\u6211\u53ef\u4ee5\u95ee\u4f60',
  '\u95ee\u4f60',
  '\u5411\u4f60',
]

const CAPABILITY_EXTRA_ABILITY_MARKERS_ZH = [
  '\u80fd',
  '\u53ef\u4ee5',
  '\u4f1a',
  '\u652f\u6301',
  '\u5e2e',
  '\u534f\u52a9',
]

const CAPABILITY_EXTRA_OBJECT_MARKERS_ZH = [
  '\u4e3a\u6211\u505a\u4ec0\u4e48',
  '\u4e3a\u6211\u505a\u4e9b\u4ec0\u4e48',
  '\u5e2e\u6211\u505a\u4ec0\u4e48',
  '\u53ef\u4ee5\u600e\u4e48\u5e2e\u6211',
  '\u505a\u54ea\u4e9b',
  '\u54ea\u4e9b\u529f\u80fd',
  '\u4ec0\u4e48\u529f\u80fd',
  '\u6709\u4ec0\u4e48\u529f\u80fd',
  '\u6709\u54ea\u4e9b\u529f\u80fd',
  '\u6709\u54ea\u4e9b\u80fd\u529b',
  '\u652f\u6301\u4ec0\u4e48',
  '\u652f\u6301\u54ea\u4e9b',
  '\u80fd\u529b',
  '\u53ef\u4ee5\u95ee\u4ec0\u4e48',
  '\u80fd\u95ee\u4ec0\u4e48',
  '\u95ee\u4f60\u4ec0\u4e48',
  '\u80fd\u5e2e\u6211\u4ec0\u4e48',
  '\u63d0\u4f9b\u4ec0\u4e48\u5e2e\u52a9',
]

function normalizeIntentText(question: string) {
  return question
    .toLowerCase()
    .normalize('NFKC')
    .replace(/[\s,.!?;:'"()[\]{}<>`~@#$%^&*_+=|\\/，。！？；：、“”‘’（）【】《》…—-]+/g, '')
}

function isCapabilityQuestionLoose(question: string) {
  if (isCapabilityQuestion(question)) {
    return true
  }

  const normalized = question.toLowerCase().normalize('NFKC')
  const matchedEnHint = CAPABILITY_EXTRA_HINTS_EN.some((keyword) => normalized.includes(keyword))
  if (matchedEnHint) {
    return true
  }
  const matchedEnRegex = CAPABILITY_EXTRA_REGEX_EN.some((pattern) => pattern.test(normalized))
  if (matchedEnRegex) {
    return true
  }

  const normalizedZh = normalizeIntentText(question)
  const matchedZhHint = CAPABILITY_EXTRA_HINTS_ZH.some((keyword) => normalizedZh.includes(keyword))
  if (matchedZhHint) {
    return true
  }
  const matchedZhRegex = CAPABILITY_EXTRA_REGEX_ZH.some((pattern) => pattern.test(normalizedZh))
  if (matchedZhRegex) {
    return true
  }

  const hasSubject = CAPABILITY_EXTRA_SUBJECT_MARKERS_ZH.some((marker) => normalizedZh.includes(marker))
  const hasSelfReference = CAPABILITY_EXTRA_SELF_REF_MARKERS_ZH.some((marker) => normalizedZh.includes(marker))
  const hasAbilityVerb = CAPABILITY_EXTRA_ABILITY_MARKERS_ZH.some((marker) => normalizedZh.includes(marker))
  const hasCapabilityObject = CAPABILITY_EXTRA_OBJECT_MARKERS_ZH.some((marker) => normalizedZh.includes(marker))

  return hasCapabilityObject && hasAbilityVerb && (hasSubject || hasSelfReference)
}

function buildCapabilityResponse() {
  if (locale.value === 'zh-CN') {
    return '我可以结合你保存的健康数据提供健康和健身建议。\n\n我能帮助你：\n1. 解释个人档案及血压、血糖、心率、血氧等指标。\n2. 根据饮食记录和目标提供饮食、饮水建议。\n3. 为增肌、减重和减脂提供训练与作息建议。\n4. 比较目标进度，建议每周调整。\n5. 制定结合饮食、运动、睡眠和恢复的日常安排。\n\n可以试着问：\n根据最新健康数据，我本周应重点关注什么？\n请为我的减脂目标制定 7 天运动与饮食计划。\n\n我不提供医学诊断或急救服务。持续不适或指标异常时，请咨询医生。'
  }
  return `I can help you with health and fitness guidance based on your saved data.

What I can do:
1. Explain your profile and latest health indicators (BP, FBG, heart rate, oxyhemoglobin) in plain language.
2. Give diet and hydration suggestions based on your food logs and goals.
3. Give training and routine suggestions for muscle gain, weight loss, and fat loss plans.
4. Compare your goal progress and suggest weekly adjustments.
5. Suggest practical daily plans that combine meals, workouts, sleep, and recovery.

Try asking:
1. "Based on my latest data, what should I focus on this week?"
2. "Create a daily routine for my fat-loss plan."
3. "How should I adjust training for my weight-loss goal?"
4. "Compare my muscle-gain and fat-loss progress and suggest priorities."
5. "Give me a 7-day checklist for diet, workout, and sleep."

Limitations:
1. I do not provide diagnosis or emergency care.
2. For persistent symptoms or abnormal values, consult a licensed clinician.`
}

function formatGoalType(goalType?: string) {
  if (!goalType) {
    return 'none'
  }
  if (goalType === 'weight_loss') {
    return 'weight loss'
  }
  if (goalType === 'muscle_gain') {
    return 'muscle gain'
  }
  if (goalType === 'fat_loss') {
    return 'fat loss'
  }
  return goalType
}

function buildGoalSummary(context: AssistantContext) {
  const snapshots = context.allGoalSnapshots || []
  if (snapshots.length === 0) {
    return `Fitness goal: ${formatGoalType(context.activeGoalType)} (status: ${context.activeGoalStatus ?? '-'}, current: ${context.activeGoalCurrentValue ?? '-'}, target: ${context.activeGoalTargetValue ?? '-'}, remaining weeks: ${context.remainingGoalWeeks ?? '-'}).`
  }

  const joined = snapshots
    .map((item) => {
      const label = formatGoalType(item.goalType)
      return `${label} [status: ${item.status ?? '-'}, current: ${item.currentValue ?? '-'}, target: ${item.targetValue ?? '-'}, progress: ${item.latestProgressPercentage ?? '-'}%]`
    })
    .join('; ')

  return `Fitness goals: ${joined}.`
}

function localFallbackAnswer(question: string, context: AssistantContext) {
  if (isCapabilityQuestionLoose(question)) {
    return buildCapabilityResponse()
  }

  if (!isHealthScopeQuestion(question)) {
    return locale.value === 'zh-CN' ? '我只能回答身体健康、饮食营养和健身计划相关的问题，请提出这些范围内的问题。' : 'I can only answer questions about body health, diet nutrition, and fitness planning. Please ask a question in this scope.'
  }

  if (!context.hasProfile) {
    return locale.value === 'zh-CN' ? '请先完善个人档案并提交至少一条健康记录，以便提供个性化健康建议。' : 'Please complete your profile and submit at least one health record first. Then I can provide personalized health suggestions.'
  }

  if (locale.value === 'zh-CN') {
    const goals = (context.allGoalSnapshots || []).map((goal) => {
      const label = goal.goalType === 'muscle_gain' ? '增肌' : goal.goalType === 'fat_loss' ? '减脂' : '减重'
      return `${label}：当前 ${goal.currentValue ?? '-'}，目标 ${goal.targetValue ?? '-'}，进度 ${goal.latestProgressPercentage ?? '-'}%`
    }).join('；')
    return `个人档案：${context.age ?? '-'} 岁，${t(context.gender)}，身高 ${context.height ?? '-'} cm，体重 ${context.weight ?? '-'} kg。\n最新健康指标：血压 ${context.latestSystolic ?? '-'} / ${context.latestDiastolic ?? '-'} mmHg，空腹血糖 ${context.latestFbg ?? '-'} mmol/L，心率 ${context.latestHeartRate ?? '-'} 次/分钟，血氧 ${context.latestOxyhemoglobin ?? '-'}%。\n最近 7 天有 ${context.last7DaysFoodCount} 条饮食记录。${goals ? '\n健身目标：' + goals : ''}\n建议保持均衡饮食、循序渐进训练、充足睡眠，并每周记录变化。如指标持续异常或身体不适，请咨询医生。`
  }

  const profileSummary = `Profile: age ${context.age ?? '-'}, gender ${context.gender ?? '-'}, height ${context.height ?? '-'} cm, weight ${context.weight ?? '-'} kg.`
  const healthSummary = `Latest health: BP ${context.latestSystolic ?? '-'} / ${context.latestDiastolic ?? '-'} mmHg, FBG ${context.latestFbg ?? '-'} mmol/L, heart rate ${context.latestHeartRate ?? '-'} bpm, oxyhemoglobin ${context.latestOxyhemoglobin ?? '-'}%.`
  const dietSummary = `Diet logs in last 7 days: ${context.last7DaysFoodCount}.`
  const fitnessSummary = buildGoalSummary(context)

  return `${profileSummary} ${healthSummary} ${dietSummary} ${fitnessSummary} Based on your question "${question}", combine balanced meals, structured training progression, recovery sleep, and weekly tracking. If any metric is consistently abnormal, please consult a licensed clinician.`
}

export async function requestAssistantReply(question: string, context: AssistantContext) {
  if (isCapabilityQuestionLoose(question)) {
    return buildCapabilityResponse()
  }

  const mode = (import.meta.env.VITE_ASSISTANT_MODE || 'api').toLowerCase()
  const endpoint = import.meta.env.VITE_ASSISTANT_API_URL || '/assistant/chat'

  if (mode === 'api') {
    try {
      const { data } = await apiClient.post<{ answer?: string }>(
        endpoint,
        {
          message: question,
          language: locale.value,
          context,
          constraints: [
            'Only answer questions related to body health, diet nutrition, and fitness planning.',
            'Use provided user health, nutrition, and fitness context as baseline for personalized advice.',
            'When fitness data includes multiple goal types (muscle gain, weight loss, fat loss), consider all of them if relevant.',
            'If question is out of scope, refuse politely.',
            locale.value === 'zh-CN' ? 'Respond in Simplified Chinese.' : 'Respond in English only, even when the user writes in another language.',
          ],
        },
        {
          timeout: 60000,
        },
      )

      if (data?.answer) {
        return data.answer
      }
    } catch {
      return `${localFallbackAnswer(question, context)} ${locale.value === 'zh-CN' ? '（服务暂不可用，当前使用本地参考回答。）' : '(API is unavailable, currently using local fallback.)'}`
    }
  }

  return localFallbackAnswer(question, context)
}
