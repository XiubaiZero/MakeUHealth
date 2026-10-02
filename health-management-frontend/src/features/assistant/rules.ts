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

export function isHealthScopeQuestion(question: string) {
  const normalized = question.toLowerCase()
  const matchedEnglish = SCOPE_KEYWORDS_EN.some((keyword) => normalized.includes(keyword))
  if (matchedEnglish) {
    return true
  }
  return SCOPE_KEYWORDS_ZH.some((keyword) => question.includes(keyword))
}

export function isCapabilityQuestion(question: string) {
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

export function normalizeIntentText(question: string) {
  return question
    .toLowerCase()
    .normalize('NFKC')
    .replace(/[\s,.!?;:'"()[\]{}<>`~@#$%^&*_+=|\\/，。！？；：、“”‘’（）【】《》…—-]+/g, '')
}

export function isCapabilityQuestionLoose(question: string) {
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

const capabilityPromptHintsEn = [
  'what can you do',
  'how can you help',
  'what can you help me with',
  'your capabilities',
  'what can i ask',
]
const capabilityPromptHintsZh = [
  '你可以为我做些什么',
  '你可以做什么',
  '你能做什么',
  '你能帮我什么',
  '你能为我做什么',
  '你有什么功能',
  '你支持什么',
  '我可以问你什么',
]
export const capabilitySuggestionPrompts = [
  'Based on my latest health data, what should I focus on this week?',
  'Create a 7-day workout and meal plan for my fat-loss goal.',
  'Compare my muscle-gain and fat-loss progress and suggest priorities.',
  'How should I adjust my daily diet based on my weight and goal?',
  'Give me a practical weekly routine for training, sleep, and recovery.',
]

export function isCapabilityPromptQuestion(question: string) {
  if (!question) {
    return false
  }

  const normalizedEn = question.toLowerCase()
  if (capabilityPromptHintsEn.some((item) => normalizedEn.includes(item))) {
    return true
  }

  const normalizedZh = question.toLowerCase().replace(/\s+/g, '')
  if (capabilityPromptHintsZh.some((item) => normalizedZh.includes(item))) {
    return true
  }

  return (
    /(what|how).{0,25}(can|could).{0,15}you.{0,25}(do|help|support|assist)/i.test(question) ||
    /你.{0,10}(能|可以|会).{0,25}(做什么|帮我|功能|能力|支持|问你什么)/.test(normalizedZh)
  )
}

export function resolveSuggestionPrompts(question: string, reply: string) {
  if (isCapabilityPromptQuestion(question)) {
    return capabilitySuggestionPrompts
  }

  if (reply.toLowerCase().includes('try asking:') || reply.includes('可以试着问')) {
    return capabilitySuggestionPrompts
  }

  return undefined
}
