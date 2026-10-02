import type { ImportedMessage } from './types'

export function readLegacyHistory(storage: Pick<Storage, 'getItem'>, scope: string): ImportedMessage[] {
  if (scope === 'guest') return []
  try {
    const parsed: unknown = JSON.parse(storage.getItem(`smart-assistant-chat-history-v2:${scope}`) || 'null')
    if (!Array.isArray(parsed)) return []
    return parsed.filter(item => typeof item?.id === 'number' && (item.role === 'user' || item.role === 'assistant') && typeof item.content === 'string' && item.content.trim())
      .map(item => ({ role: item.role, content: item.content, suggestionPrompts: Array.isArray(item.suggestionPrompts) ? item.suggestionPrompts.filter((p: unknown): p is string => typeof p === 'string' && !!p.trim()).slice(0, 6) : null }))
  } catch { return [] }
}

export function legacyImportAvailable(storage: Pick<Storage, 'getItem'>, scope: string) {
  const messages = readLegacyHistory(storage, scope)
  try { return messages.length > 0 && storage.getItem(`assistant-imported-v1:${scope}`) !== JSON.stringify(messages) }
  catch { return messages.length > 0 }
}

export function markLegacyImported(storage: Pick<Storage, 'setItem'>, scope: string, messages: ImportedMessage[]) {
  try { storage.setItem(`assistant-imported-v1:${scope}`, JSON.stringify(messages)) } catch { /* Server still deduplicates retries. */ }
}
