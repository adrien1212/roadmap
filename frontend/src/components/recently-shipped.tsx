import { Rss } from 'lucide-react'

import { CategoryChip, StatusChip } from '#/components/chips'
import { Button } from '#/components/ui/button'
import type { Category } from '#/lib/categories'
import { formatInstant } from '#/lib/format'
import { feedPath, type PublicItem } from '#/lib/public-page'

const RECENTLY_SHIPPED = 3

/** The last Shipped Items, newest first. Internal Items never reach the browser, so none can show here. */
export function recentlyShipped(items: PublicItem[]): PublicItem[] {
  return items
    .filter((item) => item.status === 'shipped' && item.shippedAt)
    .sort((a, b) => b.shippedAt!.localeCompare(a.shippedAt!))
    .slice(0, RECENTLY_SHIPPED)
}

/** "Recently shipped" on the Public page; renders nothing when no Item qualifies. Ignores the filters. */
export function RecentlyShipped({ items, categories }: { items: PublicItem[]; categories: Category[] }) {
  const shipped = recentlyShipped(items)
  if (shipped.length === 0) return null

  return (
    <section aria-labelledby="recently-shipped">
      <h2 id="recently-shipped" className="text-lg font-semibold">
        Recently shipped
      </h2>
      <ul className="mt-2">
        {shipped.map((item) => {
          const category = categories.find((c) => c.id === item.categoryId)
          return (
            <li
              key={item.id}
              className="flex flex-wrap items-center gap-3 border-b border-hairline-soft py-2.5 last:border-b-0"
            >
              <StatusChip status="shipped" />
              <span className="font-medium">{item.title}</span>
              {category && <CategoryChip name={category.name} colour={category.colour} />}
              <span
                className="ml-auto text-[13px] font-medium whitespace-nowrap text-muted-foreground tabular-nums"
                suppressHydrationWarning
              >
                {formatInstant(item.shippedAt!)}
              </span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

/** "Follow what ships": the Feed, for Readers who use a feed reader. Shown on every Public page. */
export function FollowFeed({ publicId }: { publicId: string }) {
  return (
    <aside aria-labelledby="follow-feed" className="flex flex-col gap-3 rounded-lg bg-surface-soft p-5">
      <h2 id="follow-feed" className="flex items-center gap-2 text-base font-semibold">
        <Rss className="size-4" aria-hidden />
        Follow what ships
      </h2>
      <p className="m-0 text-[13px] text-muted-foreground">Add this Roadmap's feed to your RSS reader.</p>
      <div>
        <Button variant="secondary" size="sm" asChild>
          <a href={feedPath(publicId)}>
            <Rss aria-hidden />
            RSS feed
          </a>
        </Button>
      </div>
    </aside>
  )
}
