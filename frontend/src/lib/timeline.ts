/** Where an Item sits on a one-year timeline. Positions are percentages of the year's width. */
export type Placement =
  | { kind: 'bar'; left: number; right: number; cutLeft: boolean; cutRight: boolean }
  | { kind: 'point'; x: number }
  | { kind: 'outside'; side: 'before' | 'after'; date: string }
  | { kind: 'none' }

const DAY = 86_400_000
const utc = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number)
  return Date.UTC(y, m - 1, d)
}

/**
 * Both dates → a bar from the start of the first day to the end of the last, clipped to
 * the year (cut flags mark a clipped side). One date → a point in the middle of that day.
 * No date → none. Entirely before / after the year → outside, with the date to show.
 */
export function place(item: { startDate: string | null; endDate: string | null }, year: number): Placement {
  const yearStart = Date.UTC(year, 0, 1)
  const yearEnd = Date.UTC(year + 1, 0, 1)
  const pos = (t: number) => ((t - yearStart) / (yearEnd - yearStart)) * 100

  if (item.startDate && item.endDate) {
    const start = utc(item.startDate)
    const end = utc(item.endDate) + DAY
    if (end <= yearStart) return { kind: 'outside', side: 'before', date: item.endDate }
    if (start >= yearEnd) return { kind: 'outside', side: 'after', date: item.startDate }
    return {
      kind: 'bar',
      left: Math.max(0, pos(start)),
      right: Math.min(100, pos(end)),
      cutLeft: start < yearStart,
      cutRight: end > yearEnd,
    }
  }
  const only = item.startDate ?? item.endDate
  if (!only) return { kind: 'none' }
  const day = utc(only)
  if (day < yearStart) return { kind: 'outside', side: 'before', date: only }
  if (day >= yearEnd) return { kind: 'outside', side: 'after', date: only }
  return { kind: 'point', x: pos(day + DAY / 2) }
}

/** Where today falls in `year` (percent), or null when today is in another year. */
export function todayPosition(year: number, now: Date): number | null {
  if (now.getFullYear() !== year) return null
  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) + DAY / 2
  return ((today - Date.UTC(year, 0, 1)) / (Date.UTC(year + 1, 0, 1) - Date.UTC(year, 0, 1))) * 100
}

export const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
