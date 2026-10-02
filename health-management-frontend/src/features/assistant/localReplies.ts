import { locale, t } from '../../i18n'
import type { AssistantContext } from './types'
import { isCapabilityQuestionLoose, isHealthScopeQuestion } from './rules'

export function buildCapabilityResponse() {
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

export function formatGoalType(goalType?: string) {
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

export function buildGoalSummary(context: AssistantContext) {
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

export function localFallbackAnswer(question: string, context: AssistantContext) {
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

export function buildUnavailableReply(question: string, context: AssistantContext) {
  return `${localFallbackAnswer(question, context)} ${locale.value === 'zh-CN' ? '（服务暂不可用，当前使用本地参考回答。）' : '(API is unavailable, currently using local fallback.)'}`
}
