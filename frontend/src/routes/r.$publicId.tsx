import { useState } from 'react'
import { createFileRoute, notFound } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ChevronRight } from 'lucide-react'

import { CategoryChip, CategoryDot, StatusChip } from '#/components/chips'
import { NotFound } from '#/components/not-found'
import { RecentlyShipped } from '#/components/recently-shipped'
import { Button } from '#/components/ui/button'
import { ViewToggle, type View } from '#/components/view-toggle'
import { StatusKey, YearNav, YearTimeline } from '#/components/year-timeline'
import { ApiError } from '#/lib/api'
import { colourVar, type Category } from '#/lib/categories'
import { formatDates, formatInstant } from '#/lib/format'
import { STATUSES, STATUS_LABEL, type ItemStatus } from '#/lib/items'
import { publicPageQuery, type PublicItem } from '#/lib/public-page'
import { place } from '#/lib/timeline'
import { cn } from '#/lib/utils'

/** The anonymous Public page of a Published Roadmap; Private and unknown ids both give "Page not found". */
export const Route = createFileRoute('/r/$publicId')({
  loader: async ({ context, params }) => {
    try {
      return await context.queryClient.ensureQueryData(publicPageQuery(params.publicId))
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw notFound()
      throw error
    }
  },
  head: ({ loaderData }) => ({ meta: [{ title: loaderData?.name ?? 'Page not found' }] }),
  notFoundComponent: NotFound,
  component: PublicPageView,
})

const filterButton =
  'inline-flex h-8 cursor-pointer items-center gap-[7px] rounded-full border border-border bg-background px-3.5 text-[13px] font-medium text-body transition-colors hover:border-[#d1d5db] hover:text-foreground aria-pressed:border-foreground aria-pressed:bg-foreground aria-pressed:font-semibold aria-pressed:text-white'

function PublicPageView() {
  const { publicId } = Route.useParams()
  const { data: page } = useSuspenseQuery(publicPageQuery(publicId))
  const [view, setView] = useState<View>('timeline')
  const [year, setYear] = useState(() => new Date().getFullYear())
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [status, setStatus] = useState<ItemStatus | null>(null)
  const [openId, setOpenId] = useState<number | null>(null)

  const categoryOf = (item: PublicItem) => page.categories.find((c) => c.id === item.categoryId) ?? null
  const items = page.items.filter(
    (item) => (categoryId === null || item.categoryId === categoryId) && (status === null || item.status === status),
  )

  return (
    <main className="mx-auto max-w-[1120px] px-4 pt-10 pb-24 sm:px-8 sm:pt-18 sm:pb-12">
      <header className="mb-9 flex flex-wrap items-end justify-between gap-6">
        <div>
          <h1 className="font-display text-4xl leading-[1.1] font-semibold tracking-[-1px] sm:text-5xl sm:tracking-[-1.5px]">
            {page.name}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground tabular-nums" suppressHydrationWarning>
            Published {formatInstant(page.publishedAt)}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          {view === 'timeline' && (
            <YearNav
              year={year}
              onChange={(next) => {
                setYear(next)
                setOpenId(null)
              }}
            />
          )}
          <ViewToggle view={view} onChange={setView} />
        </div>
      </header>

      <div className="mb-7">
        <RecentlyShipped items={page.items} categories={page.categories} />
      </div>

      <div className="mb-7 flex flex-col gap-3 border-y border-hairline-soft py-5">
        <div role="group" aria-label="Filter by Category" className="flex flex-wrap items-center gap-2">
          <span className="w-full shrink-0 text-[13px] font-medium text-muted-foreground sm:w-[76px]">Category</span>
          <button aria-pressed={categoryId === null} onClick={() => setCategoryId(null)} className={filterButton}>
            All
          </button>
          {page.categories.map((category) => (
            <button
              key={category.id}
              aria-pressed={categoryId === category.id}
              onClick={() => setCategoryId(category.id)}
              className={filterButton}
            >
              <span aria-hidden className="size-2 rounded-full" style={{ background: colourVar(category.colour) }} />
              {category.name}
            </button>
          ))}
        </div>
        <div role="group" aria-label="Filter by Status" className="flex flex-wrap items-center gap-2">
          <span className="w-full shrink-0 text-[13px] font-medium text-muted-foreground sm:w-[76px]">Status</span>
          <button aria-pressed={status === null} onClick={() => setStatus(null)} className={filterButton}>
            All
          </button>
          {STATUSES.map((s) => (
            <button key={s} aria-pressed={status === s} onClick={() => setStatus(s)} className={filterButton}>
              {STATUS_LABEL[s]}
            </button>
          ))}
        </div>
      </div>

      {items.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[#d1d5db] px-6 py-14 text-center">
          <h2 className="text-lg font-semibold">No Items match these filters</h2>
          <Button
            variant="secondary"
            onClick={() => {
              setCategoryId(null)
              setStatus(null)
            }}
          >
            Clear filters
          </Button>
        </div>
      ) : view === 'list' ? (
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
      ) : (
        <TimelineView
          items={items}
          year={year}
          openId={openId}
          onToggle={(id) => setOpenId(openId === id ? null : id)}
          categoryOf={categoryOf}
          onSeeList={() => setView('list')}
        />
      )}

      <footer className="mt-14 flex justify-center border-t border-hairline-soft pt-5 text-[13px] text-muted-foreground">
        Made with Product name
      </footer>
    </main>
  )
}

function TimelineView({
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
