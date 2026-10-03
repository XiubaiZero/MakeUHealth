import type { AssistantContext } from '../features/assistant/types'
import { buildUnavailableReply, localFallbackAnswer } from '../features/assistant/localReplies'
import { requestRemoteAssistantReply } from '../features/assistant/transport'

export type { AssistantContext, AssistantGoalSnapshot } from '../features/assistant/types'

export async function requestAssistantReply(question: string, context: AssistantContext): Promise<string> {
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
