import apiClient from '../../../api/client'

export type ConversationMemory = { enabled: boolean; revision: number; contentRevision: number; hasSummary: boolean; coveredSequence: number }
export type MemoryItem = { id: string; category: string; content: string; status: 'pending' | 'confirmed'; revision: number; sourceConversationId: string | null; sources: { messageId: string; evidence: string }[] }
export type MemoryState = { settings: { enabled: boolean; revision: number; capacity: number; pendingCapacity: number }; items: MemoryItem[] }
export type ExtractionJob = { requestId: string; status: string; error: string | null }
export type MemoryChange = { expectedRevision: number; category: string; content: string; confirmed?: boolean; replaceId?: string; replaceRevision?: number }
const endpoint = '/assistant'
export const memoryApi = {
  async conversation(id: string) { return (await apiClient.get<ConversationMemory>(`${endpoint}/conversations/${id}/memory`)).data },
  async toggle(id: string, expectedRevision: number, enabled: boolean) {
    return (await apiClient.patch<ConversationMemory>(`${endpoint}/conversations/${id}/memory`, { expectedRevision, enabled })).data
  },
  async state() { return (await apiClient.get<MemoryState>(`${endpoint}/memory`)).data },
  async accountToggle(expectedRevision: number, enabled: boolean) { return (await apiClient.patch(`${endpoint}/memory`, { expectedRevision, enabled })).data },
  async add(change: MemoryChange) { return (await apiClient.post<MemoryItem>(`${endpoint}/memory/items`, change)).data },
  async change(id: string, change: MemoryChange) { return (await apiClient.patch<MemoryItem>(`${endpoint}/memory/items/${id}`, change)).data },
  async remove(item: MemoryItem) { await apiClient.delete(`${endpoint}/memory/items/${item.id}`, { params: { expectedRevision: item.revision } }) },
  async clear(expectedRevision: number) { await apiClient.delete(`${endpoint}/memory`, { params: { expectedRevision } }) },
  async extract(id: string, language: string, requestId: string) {
    const conversation = (await apiClient.get<{ conversation: { revision: number } }>(`${endpoint}/conversations/${id}/messages`)).data.conversation
    return (await apiClient.post<ExtractionJob>(`${endpoint}/conversations/${id}/memory/extractions`, { requestId, expectedRevision: conversation.revision, language })).data
  },
  async extraction(id: string, requestId: string) { return (await apiClient.get<ExtractionJob>(`${endpoint}/conversations/${id}/memory/extractions/${requestId}`)).data },
}
