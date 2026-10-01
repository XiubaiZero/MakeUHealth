type Schedule = {
  reminderTime?: string
  repeatPattern?: string
  enabled?: boolean
}

export function parseReminderDateTime(value?: string): Date | null {
  if (!value) return null
  // ISO timestamps without an offset use the browser's local timezone.
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function monthlyOccurrence(start: Date, months: number) {
  const result = new Date(start)
  result.setDate(1)
  result.setMonth(start.getMonth() + months)
  const lastDay = new Date(result.getFullYear(), result.getMonth() + 1, 0).getDate()
  result.setDate(Math.min(start.getDate(), lastDay))
  return result
}

/** Return only the latest due occurrence, catching up once after the page was closed. */
export function getDueOccurrenceTime(reminder: Schedule, now: Date): Date | null {
  const start = parseReminderDateTime(reminder.reminderTime)
  if (!start || reminder.enabled === false || Number.isNaN(now.getTime()) || now < start) return null
  const repeat = reminder.repeatPattern || 'none'
  if (repeat === 'none') return start

  if (repeat === 'daily' || repeat === 'weekly') {
    // Calendar-day arithmetic avoids 23/25-hour DST days shifting the recurrence.
    const startDay = Date.UTC(start.getFullYear(), start.getMonth(), start.getDate())
    const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())
    const interval = repeat === 'weekly' ? 7 : 1
    let days = Math.floor((today - startDay) / 86400000 / interval) * interval
    const occurrence = new Date(start)
    occurrence.setDate(start.getDate() + days)
    if (occurrence > now) {
      days -= interval
      occurrence.setTime(start.getTime())
      occurrence.setDate(start.getDate() + days)
    }
    return occurrence >= start ? occurrence : null
  }

  if (repeat === 'monthly') {
    let months = (now.getFullYear() - start.getFullYear()) * 12 + now.getMonth() - start.getMonth()
    let occurrence = monthlyOccurrence(start, months)
    if (occurrence > now) occurrence = monthlyOccurrence(start, --months)
    return occurrence >= start ? occurrence : null
  }
  return null
}

export function reminderAcknowledgementStorageKey(accountId: number) {
  return `reminder-trigger-acknowledged:${accountId}`
}
