import apiClient from '../../../api/client'
import type { Conversation, ConversationApi, ConversationPage, GenerationTask, History } from './types'

const endpoint = '/assistant/conversations'
export const conversationApi: ConversationApi = {
  async list(offset = 0) { return (await apiClient.get<ConversationPage>(endpoint, { params: { offset, limit: 20 } })).data },
  async create() { return (await apiClient.post<Conversation>(endpoint)).data },
  async history(id, before) { return (await apiClient.get<History>(`${endpoint}/${id}/messages`, { params: { before, limit: 80 } })).data },
  async rename(id, expectedRevision, title) { return (await apiClient.patch<Conversation>(`${endpoint}/${id}`, { expectedRevision, title })).data },
  async remove(id, expectedRevision) { await apiClient.delete(`${endpoint}/${id}`, { params: { expectedRevision } }) },
  async removeMessages(id, expectedRevision, messageIds) { await apiClient.post(`${endpoint}/${id}/messages/delete`, { expectedRevision, messageIds }) },
  async send(id, request) { return (await apiClient.post<GenerationTask>(`${endpoint}/${id}/turns`, request, { timeout: 60000 })).data },
  async task(id, requestId) { return (await apiClient.get<GenerationTask>(`${endpoint}/${id}/turns/${requestId}`)).data },
  async importHistory(messages, language) { return (await apiClient.post<Conversation>(`${endpoint}/import`, { messages, language })).data },
}
