export type Conversation = { id: string; title: string | null; revision: number; createdAt: string; updatedAt: string }
export type ConversationMessage = {
  id: string; sequence: number; role: 'user' | 'assistant'; content: string
  language: string | null; source: string; suggestionPrompts?: string[] | null; createdAt: string | null
}
export type GenerationTask = {
  id: string; requestId: string; questionId: string; answerId: string | null
  status: 'queued' | 'running' | 'completed' | 'failed' | 'deleted'; error: string | null
}
export type ConversationPage = { items: Conversation[]; hasMore: boolean }
export type History = { conversation: Conversation; messages: ConversationMessage[]; nextBefore: number | null; activeTask: GenerationTask | null; failedTask: GenerationTask | null }
export type TurnInput = {
  message: string; language: string; context: unknown; constraints: string[]; mode: string
}
export type TurnRequest = TurnInput & { requestId: string; expectedRevision: number; editMessageId: string | null }
export type ImportedMessage = { role: 'user' | 'assistant'; content: string; suggestionPrompts: string[] | null }
export interface ConversationApi {
  list(offset?: number): Promise<ConversationPage>
  create(): Promise<Conversation>
  history(id: string, before?: number): Promise<History>
  rename(id: string, expectedRevision: number, title: string): Promise<Conversation>
  remove(id: string, expectedRevision: number): Promise<void>
  removeMessages(id: string, expectedRevision: number, messageIds: string[]): Promise<void>
  send(id: string, request: TurnRequest): Promise<GenerationTask>
  task(id: string, requestId: string): Promise<GenerationTask>
  importHistory(messages: ImportedMessage[], language: string): Promise<Conversation>
}
