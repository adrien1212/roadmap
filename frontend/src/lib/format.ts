/** Display helpers shared by the editor and the Public page. Dates are ISO days ("2026-03-01"). */

import { formatDate, type Dated } from '#/lib/dates'

const MONTH_DAY = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' })
const MONTH_DAY_YEAR = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' })

/** An ISO day as a UTC midnight Date, so formatting never shifts it by a timezone. */
export function parseDay(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

export function formatDay(iso: string, withYear = true): string {
  return (withYear ? MONTH_DAY_YEAR : MONTH_DAY).format(parseDay(iso))
}

/**
 * An Item's dates with their Precision: "Mar 1 – May 31, 2026", "Mar – Q3 2026", "Q4 2026 – Q1 2027",
 * "From Q3 2026", "By Week of Mar 9, 2026" or "No dates". The year is said once when both share it.
 */
export function formatDates({ startDate, startPrecision, endDate, endPrecision }: Dated): string {
  if (startDate && endDate) {
    const sameYear = startDate.slice(0, 4) === endDate.slice(0, 4)
    return `${formatDate(startDate, startPrecision, !sameYear)} – ${formatDate(endDate, endPrecision)}`
  }
  if (startDate) return `From ${formatDate(startDate, startPrecision)}`
  if (endDate) return `By ${formatDate(endDate, endPrecision)}`
  return 'No dates'
}

/** A publishedAt instant as a calendar date in the viewer's timezone, e.g. "Oct 4, 2026". */
export function formatInstant(instant: string): string {
  return new Date(instant).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

/** "today", "yesterday" or "N days ago", counted in calendar days. */
export function daysAgo(instant: string, now = new Date()): string {
  const then = new Date(instant)
  const startOf = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const days = Math.round((startOf(now) - startOf(then)) / 86_400_000)
  return days <= 0 ? 'today' : days === 1 ? 'yesterday' : `${days} days ago`
}

export const plural = (n: number, word: string) => `${n} ${word}${n === 1 ? '' : 's'}`
