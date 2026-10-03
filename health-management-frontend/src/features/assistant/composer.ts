export type ComposerKey = Pick<KeyboardEvent, 'key' | 'ctrlKey' | 'shiftKey' | 'altKey' | 'metaKey' | 'isComposing' | 'repeat' | 'keyCode'>
export function composerKeyAction(event: ComposerKey, enabled: boolean, composing: boolean): 'native' | 'send' | 'newline' | 'ignore' {
  if (event.key !== 'Enter' || !enabled || composing || event.isComposing || event.keyCode === 229) return 'native'
  if (event.altKey || event.metaKey) return 'native'
  if (event.repeat) return 'ignore'
  if (event.ctrlKey) return 'newline'
  if (event.shiftKey) return 'native'
  return 'send'
}
export function insertComposerNewline(text: string, start: number, end: number) {
  return { text: text.slice(0, start) + '\n' + text.slice(end), caret: start + 1 }
}
export function composerSendAction(text: string, blocked: boolean, editing: boolean, confirmed: boolean): 'blocked' | 'blank' | 'confirm' | 'send' {
  if (blocked) return 'blocked'
  if (!text.trim()) return 'blank'
  if (editing && !confirmed) return 'confirm'
  return 'send'
}
