import { ChevronRight } from 'lucide-react'

import { CategoryChip, CategoryDot, StatusChip } from '#/components/chips'
import { Button } from '#/components/ui/button'
import { StatusKey, YearTimeline } from '#/components/year-timeline'
import type { Category } from '#/lib/categories'
import { formatDates } from '#/lib/format'
import type { PublicItem } from '#/lib/public-page'
import { place } from '#/lib/timeline'
import { cn } from '#/lib/utils'

/** List view of the Public page and its embed: Items in manual order, with their descriptions. */
export function PublicItemList({
  items,
  categoryOf,
}: {
  items: PublicItem[]
  categoryOf: (item: PublicItem) => Category | null
}) {
  return (
    <ul>
      {items.map((item) => {
        const category = categoryOf(item)
        return (
          <li
            key={item.id}
            className="grid grid-cols-1 gap-2.5 border-b border-hairline-soft py-5 sm:grid-cols-[minmax(0,1fr)_220px] sm:gap-6"
          >
            <div>
              <h3 className="mb-1.5 text-base font-semibold">{item.title}</h3>
              {item.description && (
                <p className="max-w-[68ch] text-sm whitespace-pre-line text-body">{item.description}</p>
              )}
            </div>
            <div className="flex flex-col items-start gap-2">
              <div className="flex flex-wrap gap-1.5">
                <StatusChip status={item.status} />
                {category && <CategoryChip name={category.name} colour={category.colour} />}
              </div>
              <span
                className={cn(
                  'text-sm tabular-nums',
                  item.startDate || item.endDate ? 'text-body' : 'text-muted-foreground',
                )}
              >
                {formatDates(item.startDate, item.endDate)}
              </span>
            </div>
          </li>
        )
      })}
    </ul>
  )
}

/** Timeline view of the Public page and its embed: one year, rows expand to their description. */
export function PublicTimeline({
  items,
  year,
  openId,
  onToggle,
  categoryOf,
  onSeeList,
}: {
  items: PublicItem[]
  year: number
  openId: number | null
  onToggle: (id: number) => void
  categoryOf: (item: PublicItem) => Category | null
  onSeeList: () => void
}) {
  const onYear = items.filter((item) => {
    const kind = place(item, year).kind
    return kind === 'bar' || kind === 'point'
  })
  const undated = items.filter((item) => !item.startDate && !item.endDate).length
  const outside = items.length - onYear.length - undated

  return (
    <>
      <YearTimeline
        year={year}
        groups={[{ key: 'all', items: onYear }]}
        onLabelClick={(item) => onToggle(item.id)}
        isExpanded={(item) => item.id === openId}
        renderLabel={(item) => (
          <>
            <CategoryDot colour={categoryOf(item)?.colour ?? null} />
            <span className="truncate">{item.title}</span>
          </>
        )}
        renderExpanded={(item) => {
          const category = categoryOf(item)
          return (
            <>
              <div className="mb-1.5 flex flex-wrap items-center gap-2">
                <StatusChip status={item.status} />
                {category && <CategoryChip name={category.name} colour={category.colour} />}
                <span className="text-[13px] font-medium text-muted-foreground tabular-nums">
                  {formatDates(item.startDate, item.endDate)}
                </span>
              </div>
              {item.description || <span className="text-muted-foreground">No description.</span>}
            </>
          )
        }}
        empty={<p className="px-1 py-7 text-sm text-muted-foreground">Nothing on the timeline in {year}.</p>}
      />
      <div className="mt-4">
        <StatusKey />
      </div>
      {(undated > 0 || outside > 0) && (
        <div className="mt-4 flex flex-wrap items-center gap-4 rounded-md bg-surface-soft px-4 py-3 text-[13px] text-body">
          {undated > 0 && <span>{undated === 1 ? '1 Item has no dates.' : `${undated} Items have no dates.`}</span>}
          {outside > 0 && (
            <span>
              {outside} fall{outside === 1 ? 's' : ''} outside {year}.
            </span>
          )}
          <Button variant="ghost" size="sm" onClick={onSeeList}>
            See all in List
            <ChevronRight aria-hidden />
          </Button>
        </div>
      )}
    </>
  )
}
