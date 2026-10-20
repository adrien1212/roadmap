import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'

import { Button } from '#/components/ui/button'
import type { Dated } from '#/lib/dates'
import { formatDates } from '#/lib/format'
import { STATUS_LABEL, type ItemStatus } from '#/lib/items'
import { MONTHS, place, todayPosition, type Placement } from '#/lib/timeline'
import { cn } from '#/lib/utils'

export interface TimelineItem extends Dated {
  id: number
  title: string
  status: ItemStatus
}

export interface TimelineGroup<T extends TimelineItem> {
  key: string
  /** Lane header; omitted when the timeline is not in swimlanes. */
  header?: React.ReactNode
  items: T[]
}

/**
 * One calendar year by months, shared by the editor and the Public page. Items with both
 * dates are bars (clipped to the year, with cut markers), one Day date a point, one Week,
 * Month or Quarter date a band; the ends of such periods are faded. Items outside the year
 * show a marker with their date at the edge. Callers leave undated Items out.
 */
export function YearTimeline<T extends TimelineItem>({
  year,
  groups,
  renderLabel,
  onLabelClick,
  isExpanded,
  renderExpanded,
  isStriped = () => false,
  empty,
  stickyHeaderClassName = 'top-0',
}: {
  year: number
  groups: TimelineGroup<T>[]
  renderLabel: (item: T) => React.ReactNode
  onLabelClick: (item: T) => void
  /** For expandable rows (Public page): aria-expanded on the label and the content below. */
  isExpanded?: (item: T) => boolean
  renderExpanded?: (item: T) => React.ReactNode
  /** Internal Items get a striped mark in the editor. */
  isStriped?: (item: T) => boolean
  empty: React.ReactNode
  stickyHeaderClassName?: string
}) {
  // Today is the viewer's day: computed after hydration so server and browser never disagree.
  const [now, setNow] = useState<Date | null>(null)
  useEffect(() => setNow(new Date()), [])
  const today = now ? todayPosition(year, now) : null
  const hasRows = groups.some((group) => group.items.length > 0)

  return (
    <div className="relative [--label:0px] min-[640px]:[--label:180px] min-[900px]:[--label:260px]">
      <div
        className={cn(
          'sticky z-[5] grid grid-cols-1 bg-background/95 min-[640px]:grid-cols-[var(--label)_1fr]',
          stickyHeaderClassName,
        )}
      >
        <div className="max-[639px]:hidden" />
        <div className="grid grid-cols-12 border-b border-border">
          {MONTHS.map((month, i) => (
            <span
              key={month}
              className={cn(
                'border-l border-hairline-soft pt-2.5 pb-2 pl-[3px] text-[11px] font-medium min-[640px]:pl-2 min-[640px]:text-xs',
                i % 3 === 0 ? 'text-foreground' : 'text-muted-foreground',
              )}
            >
              {month}
              {i === 0 && <span className="sr-only"> {year}</span>}
            </span>
          ))}
        </div>
      </div>

      <div className="relative">
        <div
          aria-hidden
          className="pointer-events-none absolute inset-y-0 right-0 left-[var(--label)] grid grid-cols-12"
        >
          {MONTHS.map((month) => (
            <span key={month} className="border-l border-hairline-soft" />
          ))}
        </div>
        {today !== null && (
          <div
            aria-hidden
            className="pointer-events-none absolute inset-y-0 z-[3] w-0 border-l-[1.5px] border-error"
            style={{ left: `calc(var(--label) + (100% - var(--label)) * ${today / 100})` }}
          >
            <b className="absolute -top-px -translate-x-1/2 rounded-full bg-error-text px-1.5 py-px text-[11px] font-semibold whitespace-nowrap text-white">
              Today
            </b>
          </div>
        )}

        {hasRows
          ? groups.map((group) => (
              <div key={group.key}>
                {group.header && (
                  <div className="flex items-center gap-2 px-1 pt-5 pb-2 text-[13px] font-semibold">
                    {group.header}
                    <span className="font-normal text-muted-foreground tabular-nums">{group.items.length}</span>
                  </div>
                )}
                {group.items.map((item) => {
                  const placement = place(item, year)
                  const expanded = isExpanded?.(item) ?? false
                  return (
                    <div
                      key={item.id}
                      className="relative grid grid-cols-1 items-center py-2 pb-1 hover:bg-surface-soft min-[640px]:min-h-11 min-[640px]:grid-cols-[var(--label)_1fr] min-[640px]:py-0"
                    >
                      <button
                        onClick={() => onLabelClick(item)}
                        aria-expanded={isExpanded ? expanded : undefined}
                        className="flex min-w-0 cursor-pointer items-center gap-2 rounded-sm pr-4 pl-1 text-left text-sm font-medium focus-visible:outline-offset-[-2px] min-[640px]:h-11"
                      >
                        {renderLabel(item)}
                      </button>
                      <div className="relative h-8 min-[640px]:h-11">
                        <Mark item={item} placement={placement} striped={isStriped(item)} />
                      </div>
                      {expanded && renderExpanded && (
                        <div className="col-span-full mb-3 max-w-[640px] rounded-md bg-surface-soft px-4 py-3 text-sm whitespace-pre-line text-body min-[640px]:ml-[var(--label)]">
                          {renderExpanded(item)}
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            ))
          : empty}
      </div>
    </div>
  )
}

const MARK_STYLE: Record<ItemStatus, string> = {
  shipped: 'bg-foreground',
  in_progress: 'bg-[#3b82f6]',
  planned: 'border-2 border-dashed border-muted-soft bg-background',
}

const STRIPES: Record<ItemStatus, string> = {
  shipped: 'bg-[repeating-linear-gradient(-45deg,rgba(255,255,255,0.55)_0_4px,transparent_4px_8px)]',
  in_progress: 'bg-[repeating-linear-gradient(-45deg,rgba(255,255,255,0.55)_0_4px,transparent_4px_8px)]',
  planned: 'bg-[repeating-linear-gradient(-45deg,var(--surface-card)_0_4px,transparent_4px_8px)]',
}

function Mark({ item, placement, striped }: { item: TimelineItem; placement: Placement; striped: boolean }) {
  const label = `${item.title}: ${STATUS_LABEL[item.status]}, ${formatDates(item)}`
  const off = 'absolute top-2 flex items-center gap-0.5 text-xs text-muted-foreground min-[640px]:top-3.5'

  switch (placement.kind) {
    case 'bar':
      return (
        <>
          <div
            role="img"
            aria-label={label}
            className={cn(
              'absolute top-1.5 h-5 min-w-2 rounded-full min-[640px]:top-3',
              MARK_STYLE[item.status],
              striped && STRIPES[item.status],
              (placement.cutLeft || placement.fuzzyLeft) && 'rounded-l-none',
              (placement.cutRight || placement.fuzzyRight) && 'rounded-r-none',
              item.status === 'planned' && placement.cutLeft && 'border-l-transparent [border-left-style:solid]',
              item.status === 'planned' && placement.cutRight && 'border-r-transparent [border-right-style:solid]',
            )}
            style={{
              left: `${placement.left}%`,
              width: `${placement.right - placement.left}%`,
              maskImage: fadeMask(placement.fuzzyLeft, placement.fuzzyRight),
            }}
          />
          {placement.cutLeft && (
            <span className={cn(off, 'left-0.5')}>
              <ChevronLeft className="size-4" aria-hidden />
            </span>
          )}
          {placement.cutRight && (
            <span className={cn(off, 'right-1')}>
              <ChevronRight className="size-4" aria-hidden />
            </span>
          )}
        </>
      )
    case 'point':
      return (
        <div
          role="img"
          aria-label={label}
          className={cn(
            'absolute top-[9px] size-3.5 -translate-x-1/2 rounded-full min-[640px]:top-[15px]',
            item.status === 'planned' ? 'border-2 border-muted-soft bg-background' : MARK_STYLE[item.status],
          )}
          style={{ left: `${placement.x}%` }}
        />
      )
    case 'outside':
      return placement.side === 'before' ? (
        <span className={cn(off, 'left-1')} aria-label={label}>
          <ChevronLeft className="size-4" aria-hidden />
          {placement.label}
        </span>
      ) : (
        <span className={cn(off, 'right-1')} aria-label={label}>
          {placement.label}
          <ChevronRight className="size-4" aria-hidden />
        </span>
      )
    case 'none':
      return null
  }
}

/** A Week, Month or Quarter end fades out over its last stretch: the plan is that period, not that day. */
function fadeMask(left: boolean, right: boolean): string | undefined {
  if (!left && !right) return undefined
  const from = left ? 'transparent 0, #000 min(16px, 30%)' : '#000 0'
  const to = right ? '#000 calc(100% - min(16px, 30%)), transparent 100%' : '#000 100%'
  return `linear-gradient(to right, ${from}, ${to})`
}

/** The legend of mark styles shown under a timeline. */
export function StatusKey() {
  return (
    <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
      <span className="inline-flex items-center gap-1.5">
        <i className="inline-block h-2 w-[18px] rounded-full bg-foreground" />
        Shipped
      </span>
      <span className="inline-flex items-center gap-1.5">
        <i className="inline-block h-2 w-[18px] rounded-full bg-[#3b82f6]" />
        In progress
      </span>
      <span className="inline-flex items-center gap-1.5">
        <i className="inline-block h-2.5 w-[18px] rounded-full border-2 border-dashed border-muted-soft" />
        Planned
      </span>
      <span className="inline-flex items-center gap-1.5">
        <i className="inline-block size-2.5 rounded-full bg-foreground" />
        Single date
      </span>
      <span className="inline-flex items-center gap-1.5">
        <i
          className="inline-block h-2 w-[18px] bg-foreground"
          style={{ maskImage: 'linear-gradient(to right, transparent, #000 40%, #000 60%, transparent)' }}
        />
        Week, month or quarter
      </span>
    </div>
  )
}

/** "‹ 2026 ›" year navigation. */
export function YearNav({ year, onChange }: { year: number; onChange: (year: number) => void }) {
  return (
    <div className="inline-flex items-center gap-1">
      <Button variant="ghost" size="icon-sm" aria-label="Previous year" onClick={() => onChange(year - 1)}>
        <ChevronLeft aria-hidden />
      </Button>
      <strong aria-live="polite" className="min-w-14 text-center font-display text-xl font-semibold tabular-nums">
        {year}
      </strong>
      <Button variant="ghost" size="icon-sm" aria-label="Next year" onClick={() => onChange(year + 1)}>
        <ChevronRight aria-hidden />
      </Button>
    </div>
  )
}
