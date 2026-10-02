import apiClient from '../../../api/client'

export type ConversationMemory = { enabled: boolean; revision: number; contentRevision: number; hasSummary: boolean; coveredSequence: number }
const endpoint = '/assistant'
export const memoryApi = {
  async conversation(id: string) { return (await apiClient.get<ConversationMemory>(`${endpoint}/conversations/${id}/memory`)).data },
  async toggle(id: string, expectedRevision: number, enabled: boolean) {
    return (await apiClient.patch<ConversationMemory>(`${endpoint}/conversations/${id}/memory`, { expectedRevision, enabled })).data
  },
}
