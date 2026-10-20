import { createFileRoute, notFound } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ChevronLeft } from 'lucide-react'

import { BrandMark } from '#/components/brand-mark'
import { CategoryChip, StatusChip } from '#/components/chips'
import { NotFound } from '#/components/not-found'
import { NotifyMe } from '#/components/notify-me'
import { CopyItemLink } from '#/components/public-items'
import { ReaderUpdates } from '#/components/recently-shipped'
import { ApiError } from '#/lib/api'
import { itemPageTitle, previewDescription } from '#/lib/brand'
import { formatDates, formatInstant } from '#/lib/format'
import { feedPath, publicPageQuery, type PublicPage } from '#/lib/public-page'
import { publicPath } from '#/lib/roadmaps'
import { cn } from '#/lib/utils'

const PREVIEW_LENGTH = 200

/**
 * The Item page: one non-Internal Item of a Published Roadmap, the link to send about it.
 * Built from the Public page payload, so an Internal, deleted or unknown Item gives the same 404 as a Private Roadmap.
 */
export const Route = createFileRoute('/r/$publicId_/items/$itemId')({
  loader: async ({ context, params }) => {
    let page: PublicPage
    try {
      page = await context.queryClient.ensureQueryData(publicPageQuery(params.publicId))
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw notFound()
      throw error
    }
    const item = page.items.find((i) => String(i.id) === params.itemId)
    if (!item) throw notFound()
    return { page, item }
  },
  // Rendered on the server so the link unfurls with the Item's title in Slack, email or LinkedIn.
  head: ({ loaderData, params }) => {
    if (!loaderData) return { meta: [{ title: 'Page not found' }] }
    const { page, item } = loaderData
    const title = itemPageTitle(item.title, page.name, page.brandName)
    const description = item.description ? preview(item.description) : previewDescription(page.brandName)
    return {
      meta: [
        { title },
        { name: 'description', content: description },
        { property: 'og:title', content: title },
        { property: 'og:description', content: description },
        { property: 'og:type', content: 'website' },
        { name: 'twitter:card', content: 'summary' },
      ],
      links: [{ rel: 'alternate', type: 'application/rss+xml', title: page.name, href: feedPath(params.publicId) }],
    }
  },
  notFoundComponent: NotFound,
  component: ItemPageView,
})

/** The description on one line, cut at a word near 200 characters. */
function preview(description: string): string {
  const flat = description.replace(/\s+/g, ' ').trim()
  if (flat.length <= PREVIEW_LENGTH) return flat
  const cut = flat.slice(0, PREVIEW_LENGTH)
  return `${cut.slice(0, cut.lastIndexOf(' ') > 0 ? cut.lastIndexOf(' ') : PREVIEW_LENGTH)}…`
}

function ItemPageView() {
  const { publicId } = Route.useParams()
  const { item: loaded } = Route.useLoaderData()
  const { data: page } = useSuspenseQuery(publicPageQuery(publicId))
  // The Item as of the latest payload; the loader already 404s when it is missing.
  const item = page.items.find((i) => i.id === loaded.id) ?? loaded
  const category = page.categories.find((c) => c.id === item.categoryId) ?? null
  const dated = Boolean(item.startDate || item.endDate)

  return (
    <main className="mx-auto max-w-[760px] px-4 pt-10 pb-24 sm:px-8 sm:pt-18 sm:pb-12">
      {page.brandName && <BrandMark name={page.brandName} className="mb-5" />}
      <a
        href={publicPath({ publicId })}
        className="-ml-1 mb-6 inline-flex items-center gap-1 text-sm font-medium text-muted-foreground hover:text-foreground"
      >
        <ChevronLeft className="size-4" aria-hidden />
        {page.name}
      </a>

      <article className="mb-10">
        <h1 className="font-display text-3xl leading-[1.15] font-semibold tracking-[-0.8px] sm:text-4xl sm:tracking-[-1px]">
          {item.title}
        </h1>
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <StatusChip status={item.status} />
          {category && <CategoryChip name={category.name} colour={category.colour} />}
          <span className={cn('text-sm tabular-nums', dated ? 'text-body' : 'text-muted-foreground')}>
            {formatDates(item)}
          </span>
        </div>
        {item.shippedAt && (
          <p className="mt-2 text-sm text-muted-foreground tabular-nums" suppressHydrationWarning>
            Shipped {formatInstant(item.shippedAt)}
          </p>
        )}
        {item.description ? (
          <p className="mt-6 max-w-[68ch] text-[15px] leading-relaxed whitespace-pre-line text-body">
            {item.description}
          </p>
        ) : (
          <p className="mt-6 text-sm text-muted-foreground">No description.</p>
        )}
        <div className="mt-5 -ml-2.5">
          <CopyItemLink publicId={publicId} itemId={item.id} />
        </div>
      </article>

      {page.subscriptionsOpen && item.status !== 'shipped' ? (
        <NotifyMe publicId={publicId} itemId={item.id} roadmapName={page.name} brandName={page.brandName} />
      ) : (
        <ReaderUpdates publicId={publicId} brandName={page.brandName} subscriptionsOpen={page.subscriptionsOpen} />
      )}

      <footer className="mt-14 flex justify-center border-t border-hairline-soft pt-5 text-[13px] text-muted-foreground">
        Made with Product name
      </footer>
    </main>
  )
}
