import type { AssistantContext } from '../features/assistant/types'
import { isCapabilityQuestionLoose } from '../features/assistant/rules'
import { buildCapabilityResponse, buildUnavailableReply, localFallbackAnswer } from '../features/assistant/localReplies'
import { requestRemoteAssistantReply } from '../features/assistant/transport'

export type { AssistantContext, AssistantGoalSnapshot } from '../features/assistant/types'

export async function requestAssistantReply(question: string, context: AssistantContext): Promise<string> {
  if (isCapabilityQuestionLoose(question)) {
    return buildCapabilityResponse()
  }

  const mode = (import.meta.env.VITE_ASSISTANT_MODE || 'api').toLowerCase()
  if (mode === 'api') {
    try {
      const answer = await requestRemoteAssistantReply(question, context)
      if (answer) return answer
    } catch {
      return buildUnavailableReply(question, context)
    }
  }

  return localFallbackAnswer(question, context)
}
