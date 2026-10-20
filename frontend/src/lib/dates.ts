/**
 * Item dates and their Precision (SPEC-fuzzy-dates.md). A date is stored as the first day of its
 * period ("2026-07-01" + quarter = Q3 2026); its last day is computed here, as on the backend.
 */

export type Precision = 'day' | 'week' | 'month' | 'quarter'

export const PRECISION_LABEL: Record<Precision, string> = {
  day: 'Day',
  week: 'Week',
  month: 'Month',
  quarter: 'Quarter',
}

export const PRECISIONS = Object.keys(PRECISION_LABEL) as Precision[]

/** The dates of an Item, as the editor and the Public page receive them. */
export interface Dated {
  startDate: string | null
  startPrecision: Precision
  endDate: string | null
  endPrecision: Precision
}

const DAY = 86_400_000

const toUtc = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number)
  return Date.UTC(y, m - 1, d)
}

const toIso = (time: number) => new Date(time).toISOString().slice(0, 10)

/** The first day of the period that contains `iso`: its Monday, the 1st of its month or of its quarter. */
export function periodStart(iso: string, precision: Precision): string {
  const date = new Date(toUtc(iso))
  const y = date.getUTCFullYear()
  const m = date.getUTCMonth()
  switch (precision) {
    case 'day':
      return iso
    case 'week':
      // getUTCDay: 0 is Sunday; weeks run Monday to Sunday.
      return toIso(date.getTime() - ((date.getUTCDay() + 6) % 7) * DAY)
    case 'month':
      return toIso(Date.UTC(y, m, 1))
    case 'quarter':
      return toIso(Date.UTC(y, m - (m % 3), 1))
  }
}

/** The last day of the period starting on `first`. */
export function periodEnd(first: string, precision: Precision): string {
  const date = new Date(toUtc(first))
  const y = date.getUTCFullYear()
  const m = date.getUTCMonth()
  switch (precision) {
    case 'day':
      return first
    case 'week':
      return toIso(date.getTime() + 6 * DAY)
    case 'month':
      return toIso(Date.UTC(y, m + 1, 0))
    case 'quarter':
      return toIso(Date.UTC(y, m + 3, 0))
  }
}

const MONTH = new Intl.DateTimeFormat('en-US', { month: 'short', timeZone: 'UTC' })
const MONTH_DAY = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' })

/** "Mar 9, 2026", "Week of Mar 9, 2026", "Mar 2026" or "Q3 2026"; without the year when `withYear` is false. */
export function formatDate(first: string, precision: Precision, withYear = true): string {
  const date = new Date(toUtc(first))
  const year = withYear ? `${precision === 'day' || precision === 'week' ? ',' : ''} ${date.getUTCFullYear()}` : ''
  switch (precision) {
    case 'day':
      return `${MONTH_DAY.format(date)}${year}`
    case 'week':
      return `Week of ${MONTH_DAY.format(date)}${year}`
    case 'month':
      return `${MONTH.format(date)}${year}`
    case 'quarter':
      return `Q${Math.floor(date.getUTCMonth() / 3) + 1}${year}`
  }
}

/** Both dates set and the end period finishing before the start period begins. */
export function endsBeforeStart(dated: Dated): boolean {
  if (!dated.startDate || !dated.endDate) return false
  return periodEnd(dated.endDate, dated.endPrecision) < dated.startDate
}
