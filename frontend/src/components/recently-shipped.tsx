import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Mail, Rss } from 'lucide-react'

import { CategoryChip, StatusChip } from '#/components/chips'
import { FieldError } from '#/components/field-error'
import { Button } from '#/components/ui/button'
import { Input } from '#/components/ui/input'
import type { Category } from '#/lib/categories'
import { formatInstant } from '#/lib/format'
import { feedPath, subscribe, type PublicItem } from '#/lib/public-page'

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

/**
 * Beside Recently shipped: subscribe by email when the Owner's SMTP server works,
 * otherwise "Follow what ships" with the Feed only. The RSS link shows in both.
 */
export function ReaderUpdates({
  publicId,
  brandName,
  subscriptionsOpen,
}: {
  publicId: string
  brandName: string | null
  subscriptionsOpen: boolean
}) {
  return (
    <aside aria-labelledby="reader-updates" className="flex flex-col gap-3 rounded-lg bg-surface-soft p-5">
      {subscriptionsOpen ? (
        <SubscribeForm publicId={publicId} brandName={brandName} />
      ) : (
        <>
          <h2 id="reader-updates" className="flex items-center gap-2 text-base font-semibold">
            <Rss className="size-4" aria-hidden />
            Follow what ships
          </h2>
          <p className="m-0 text-[13px] text-muted-foreground">Add this Roadmap's feed to your RSS reader.</p>
        </>
      )}
      <div>
        <Button variant={subscriptionsOpen ? 'link' : 'secondary'} size="sm" asChild className={subscriptionsOpen ? 'h-auto px-0 text-[13px] font-medium text-muted-foreground' : undefined}>
          <a href={feedPath(publicId)}>
            <Rss aria-hidden />
            {subscriptionsOpen ? 'Or follow with RSS' : 'RSS feed'}
          </a>
        </Button>
      </div>
    </aside>
  )
}

function SubscribeForm({ publicId, brandName }: { publicId: string; brandName: string | null }) {
  return (
    <EmailForm
      heading="Get an email when something ships"
      cta="Subscribe"
      hint={`One email per shipped Item. ${brandName ?? 'The owner of this Roadmap'} receives your email address. Unsubscribe in one click.`}
      done="Check your inbox. Confirm your email to start getting updates."
      send={(email) => subscribe(publicId, email)}
    />
  )
}

/** An email field and a button that asks for a Subscription; the answer never says whether the address was known. */
export function EmailForm({
  heading,
  cta,
  hint,
  done,
  send: sendEmail,
}: {
  heading: string
  cta: string
  hint: string
  done: string
  send: (email: string) => Promise<unknown>
}) {
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const send = useMutation({
    mutationFn: () => sendEmail(email),
    onSuccess: () => setError(null),
    onError: (e) => setError(e.message),
  })

  return (
    <>
      <h2 id="reader-updates" className="flex items-center gap-2 text-base font-semibold">
        <Mail className="size-4" aria-hidden />
        {heading}
      </h2>
      {send.isSuccess ? (
        <p role="status" className="m-0 text-sm text-body">
          {done}
        </p>
      ) : (
        <>
          <form
            noValidate
            className="flex flex-col gap-2 min-[480px]:flex-row"
            onSubmit={(e) => {
              e.preventDefault()
              send.mutate()
            }}
          >
            <label htmlFor="sub-email" className="sr-only">
              Email
            </label>
            <Input
              id="sub-email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? 'sub-err' : 'sub-hint'}
            />
            <Button type="submit" disabled={send.isPending}>
              {cta}
            </Button>
          </form>
          {error && <FieldError id="sub-err">{error}</FieldError>}
          <p id="sub-hint" className="m-0 text-[13px] text-muted-foreground">
            {hint}
          </p>
        </>
      )}
    </>
  )
}
