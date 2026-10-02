import apiClient from '../../api/client'
import { locale } from '../../i18n'
import type { AssistantContext } from './types'

export function buildAssistantRequest(question: string, context: AssistantContext) {
  return {
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
  }
}

export async function requestRemoteAssistantReply(question: string, context: AssistantContext): Promise<string | undefined> {
  const endpoint = import.meta.env.VITE_ASSISTANT_API_URL || '/assistant/chat'
  const { data } = await apiClient.post<{ answer?: string }>(
    endpoint,
    buildAssistantRequest(question, context),
    {
      timeout: 60000,
    },
  )
  return data?.answer
}
