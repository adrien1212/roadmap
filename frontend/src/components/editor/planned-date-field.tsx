import { Input } from '#/components/ui/input'
import { Label } from '#/components/ui/label'
import { formatDate, periodEnd, periodStart, type Precision } from '#/lib/dates'

const select =
  "h-10 w-full min-w-0 cursor-pointer appearance-none rounded-md border border-border bg-background bg-[url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='none' stroke='%236b7280' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m4 6 4 4 4-4'/%3E%3C/svg%3E\")] bg-[right_10px_center] bg-no-repeat pr-8 pl-3 text-[15px] outline-none hover:border-[#d1d5db] focus:border-foreground focus:ring-[3px] focus:ring-[rgba(17,17,17,0.08)] aria-invalid:border-error-text"

const NONE = ''
const EXACT = 'exact'

const pad = (n: number) => String(n).padStart(2, '0')
/** Today in the Owner's timezone, as an ISO day. */
const localToday = (now: Date) => `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
const addDays = (iso: string, days: number) => {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10)
}
const addMonths = (first: string, months: number) => {
  const [y, m] = first.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1 + months, 1)).toISOString().slice(0, 10)
}

const key = (precision: Precision, first: string) => `${precision}:${first}`

interface Choice {
  key: string
  label: string
}

/**
 * The choices, relative to today: this / next / in two weeks, this / next / in two months, the four quarters of
 * this year and the first two of next year. Each one is stored as the absolute period it names today ("Next month" saved in October is Nov 2026).
 */
function choices(today: string): { label: string; options: Choice[] }[] {
  const week = periodStart(today, 'week')
  const month = periodStart(today, 'month')
  const year = today.slice(0, 4)
  const weekLabel = (first: string) => formatDate(first, 'day', false)
  return [
    {
      label: 'Week',
      options: [
        { key: key('week', week), label: `This week (${weekLabel(week)})` },
        { key: key('week', addDays(week, 7)), label: `Next week (${weekLabel(addDays(week, 7))})` },
        { key: key('week', addDays(week, 14)), label: `In two weeks (${weekLabel(addDays(week, 14))})` },
      ],
    },
    {
      label: 'Month',
      options: [
        { key: key('month', month), label: `This month (${formatDate(month, 'month')})` },
        { key: key('month', addMonths(month, 1)), label: `Next month (${formatDate(addMonths(month, 1), 'month')})` },
        { key: key('month', addMonths(month, 2)), label: `In two months (${formatDate(addMonths(month, 2), 'month')})` },
      ],
    },
    {
      label: 'Quarter',
      options: [
        ...['01', '04', '07', '10'].map((m) => `${year}-${m}-01`),
        ...['01', '04'].map((m) => `${Number(year) + 1}-${m}-01`),
      ].map((first) => ({ key: key('quarter', first), label: formatDate(first, 'quarter') })),
    },
  ]
}

/**
 * One date of an Item, picked from one dropdown: a week, a month or a quarter close to today, or an exact day.
 * The value is always the first day of the period ("2026-07-01" + quarter), null for no date. A saved period
 * that is no longer among the choices (last year's Q3) stays selectable as it is.
 */
export function PlannedDateField({
  id,
  label,
  value,
  precision,
  onChange,
  invalid,
  describedBy,
}: {
  id: string
  label: string
  value: string | null
  precision: Precision
  onChange: (value: string | null, precision: Precision) => void
  invalid?: boolean
  describedBy?: string
}) {
  const today = localToday(new Date())
  const groups = choices(today)
  const selected = value === null ? NONE : precision === 'day' ? EXACT : key(precision, value)
  const listed = selected === NONE || selected === EXACT || groups.some((g) => g.options.some((o) => o.key === selected))
  const aria = { 'aria-invalid': invalid || undefined, 'aria-describedby': describedBy }

  return (
    <div className="flex min-w-0 flex-col gap-1.5">
      <Label htmlFor={id} className="text-[13px] font-medium text-body">
        {label} <span className="font-normal text-muted-foreground">(optional)</span>
      </Label>
      <select
        id={id}
        value={selected}
        onChange={(e) => {
          const next = e.target.value
          if (next === NONE) return onChange(null, 'day')
          if (next === EXACT) return onChange(value ?? today, 'day')
          const [p, first] = next.split(':') as [Precision, string]
          onChange(first, p)
        }}
        className={select}
        {...aria}
      >
        <option value={NONE}>No date</option>
        {!listed && value && <option value={selected}>{formatDate(value, precision)}</option>}
        {groups.map((group) => (
          <optgroup key={group.label} label={group.label}>
            {group.options.map((o) => (
              <option key={o.key} value={o.key}>
                {o.label}
              </option>
            ))}
          </optgroup>
        ))}
        <optgroup label="Day">
          <option value={EXACT}>Exact date…</option>
        </optgroup>
      </select>

      {precision === 'day' && value !== null && (
        <Input
          aria-label={`${label}, exact day`}
          type="date"
          value={value}
          onChange={(e) => onChange(e.target.value || null, 'day')}
          className="pr-2 tabular-nums"
        />
      )}
      {precision === 'week' && value && (
        <span className="text-[13px] text-muted-foreground tabular-nums">
          {formatDate(value, 'day', false)} – {formatDate(periodEnd(value, 'week'), 'day')}
        </span>
      )}
    </div>
  )
}
