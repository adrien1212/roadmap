import { useState } from 'react'
import { createFileRoute, notFound } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'

import { BrandMark } from '#/components/brand-mark'
import { NotFound } from '#/components/not-found'
import { PublicItemList, PublicTimeline } from '#/components/public-items'
import { ReaderUpdates, RecentlyShipped } from '#/components/recently-shipped'
import { Button } from '#/components/ui/button'
import { ViewToggle, type View } from '#/components/view-toggle'
import { YearNav } from '#/components/year-timeline'
import { ApiError } from '#/lib/api'
import { pageTitle, previewDescription } from '#/lib/brand'
import { colourVar } from '#/lib/categories'
import { formatInstant } from '#/lib/format'
import { STATUSES, STATUS_LABEL, type ItemStatus } from '#/lib/items'
import { feedPath, publicPageQuery, type PublicItem } from '#/lib/public-page'

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
  // Rendered on the server: link previews (Slack, X, LinkedIn…) do not run JavaScript.
  head: ({ loaderData, params }) => ({
    meta: loaderData
      ? [
          { title: pageTitle(loaderData.name, loaderData.brandName) },
          { name: 'description', content: previewDescription(loaderData.brandName) },
          { property: 'og:title', content: pageTitle(loaderData.name, loaderData.brandName) },
          { property: 'og:description', content: previewDescription(loaderData.brandName) },
          { property: 'og:type', content: 'website' },
          { name: 'twitter:card', content: 'summary' },
        ]
      : [{ title: 'Page not found' }],
    // Lets feed readers find the Feed from the Public page URL.
    links: loaderData
      ? [{ rel: 'alternate', type: 'application/rss+xml', title: loaderData.name, href: feedPath(params.publicId) }]
      : [],
  }),
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
      {page.brandName && <BrandMark name={page.brandName} className="mb-5" />}
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

      <div className="mb-7 grid grid-cols-1 items-start gap-6 min-[640px]:grid-cols-[minmax(0,1fr)_340px]">
        <div>
          <RecentlyShipped items={page.items} categories={page.categories} />
        </div>
        <ReaderUpdates publicId={publicId} brandName={page.brandName} subscriptionsOpen={page.subscriptionsOpen} />
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
        <PublicItemList publicId={publicId} items={items} categoryOf={categoryOf} />
      ) : (
        <PublicTimeline
          publicId={publicId}
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

