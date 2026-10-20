import { useState } from 'react'
import { createFileRoute, notFound } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ArrowUpRight } from 'lucide-react'

import { NotFound } from '#/components/not-found'
import { PublicItemList, PublicTimeline } from '#/components/public-items'
import { ViewToggle, type View } from '#/components/view-toggle'
import { YearNav } from '#/components/year-timeline'
import { ApiError } from '#/lib/api'
import { pageTitle } from '#/lib/brand'
import { publicPageQuery, type PublicItem } from '#/lib/public-page'
import { publicPath } from '#/lib/roadmaps'

/**
 * The Public page reduced to its Items, for an iframe on the Owner's own site: no header, Recently shipped,
 * subscribe box or filters. Same payload and same 404 as the Public page, so Internal Items and Private
 * Roadmaps stay out.
 */
export const Route = createFileRoute('/r/$publicId_/embed')({
  loader: async ({ context, params }) => {
    try {
      return await context.queryClient.ensureQueryData(publicPageQuery(params.publicId))
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw notFound()
      throw error
    }
  },
  head: ({ loaderData }) => ({
    meta: loaderData
      ? [
          { title: pageTitle(loaderData.name, loaderData.brandName) },
          // The Public page is the one to index; this is a copy of it.
          { name: 'robots', content: 'noindex' },
        ]
      : [{ title: 'Page not found' }],
  }),
  notFoundComponent: NotFound,
  component: EmbedView,
})

function EmbedView() {
  const { publicId } = Route.useParams()
  const { data: page } = useSuspenseQuery(publicPageQuery(publicId))
  const [view, setView] = useState<View>('timeline')
  const [year, setYear] = useState(() => new Date().getFullYear())
  const [openId, setOpenId] = useState<number | null>(null)

  const categoryOf = (item: PublicItem) => page.categories.find((c) => c.id === item.categoryId) ?? null

  return (
    <main className="px-4 py-4 sm:px-6 sm:py-5">
      <header className="mb-5 flex flex-wrap items-center justify-between gap-3">
        {/* The full Public page (Recently shipped, filters, subscribe) opens outside the host site. */}
        <a
          href={publicPath({ publicId })}
          target="_blank"
          rel="noopener"
          className="inline-flex min-w-0 items-center gap-1 text-sm font-semibold text-foreground hover:underline"
        >
          <span className="truncate">{page.name}</span>
          <ArrowUpRight className="size-4 shrink-0 text-muted-foreground" aria-hidden />
        </a>
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

      {page.items.length === 0 ? (
        <p className="py-10 text-center text-sm text-muted-foreground">Nothing on this Roadmap yet.</p>
      ) : view === 'list' ? (
        <PublicItemList items={page.items} categoryOf={categoryOf} />
      ) : (
        <PublicTimeline
          items={page.items}
          year={year}
          openId={openId}
          onToggle={(id) => setOpenId(openId === id ? null : id)}
          categoryOf={categoryOf}
          onSeeList={() => setView('list')}
        />
      )}
    </main>
  )
}
