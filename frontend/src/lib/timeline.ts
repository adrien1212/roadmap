import { formatDate, periodEnd, type Dated } from '#/lib/dates'

/**
 * Where an Item sits on a one-year timeline. Positions are percentages of the year's width.
 * Fuzzy sides are the ends of a Week, Month or Quarter date, drawn faded; cut sides are clipped by the year.
 */
export type Placement =
  | { kind: 'bar'; left: number; right: number; cutLeft: boolean; cutRight: boolean; fuzzyLeft: boolean; fuzzyRight: boolean }
  | { kind: 'point'; x: number }
  | { kind: 'outside'; side: 'before' | 'after'; label: string }
  | { kind: 'none' }

const DAY = 86_400_000
const utc = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number)
  return Date.UTC(y, m - 1, d)
}

/**
 * Both dates → a bar from the first day of the start period to the end of the end period's last
 * day, clipped to the year (cut flags mark a clipped side). One Day date → a point in the middle
 * of that day; one Week, Month or Quarter date → a band over its period. No date → none.
 * Entirely before / after the year → outside, with the date to show.
 */
export function place(item: Dated, year: number): Placement {
  const yearStart = Date.UTC(year, 0, 1)
  const yearEnd = Date.UTC(year + 1, 0, 1)
  const pos = (t: number) => ((t - yearStart) / (yearEnd - yearStart)) * 100

  const span = (
    first: string,
    firstPrecision: Dated['startPrecision'],
    last: string,
    lastPrecision: Dated['endPrecision'],
  ): Placement => {
    const start = utc(first)
    const end = utc(periodEnd(last, lastPrecision)) + DAY
    if (end <= yearStart) return { kind: 'outside', side: 'before', label: formatDate(last, lastPrecision) }
    if (start >= yearEnd) return { kind: 'outside', side: 'after', label: formatDate(first, firstPrecision) }
    const cutLeft = start < yearStart
    const cutRight = end > yearEnd
    return {
      kind: 'bar',
      left: Math.max(0, pos(start)),
      right: Math.min(100, pos(end)),
      cutLeft,
      cutRight,
      fuzzyLeft: !cutLeft && firstPrecision !== 'day',
      fuzzyRight: !cutRight && lastPrecision !== 'day',
    }
  }

  if (item.startDate && item.endDate) return span(item.startDate, item.startPrecision, item.endDate, item.endPrecision)
  const only = item.startDate ?? item.endDate
  if (!only) return { kind: 'none' }
  const precision = item.startDate ? item.startPrecision : item.endPrecision
  if (precision !== 'day') return span(only, precision, only, precision)
  const day = utc(only)
  if (day < yearStart) return { kind: 'outside', side: 'before', label: formatDate(only, 'day') }
  if (day >= yearEnd) return { kind: 'outside', side: 'after', label: formatDate(only, 'day') }
  return { kind: 'point', x: pos(day + DAY / 2) }
}

/** Where today falls in `year` (percent), or null when today is in another year. */
export function todayPosition(year: number, now: Date): number | null {
  if (now.getFullYear() !== year) return null
  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) + DAY / 2
  return ((today - Date.UTC(year, 0, 1)) / (Date.UTC(year + 1, 0, 1) - Date.UTC(year, 0, 1))) * 100
}

export const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
