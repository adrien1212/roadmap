import { createFileRoute } from '@tanstack/react-router'
import { ArrowRight, EyeOff } from 'lucide-react'

import { NeutralChip, StateBadge, StatusChip } from '#/components/chips'
import { GoogleButton } from '#/components/google-button'
import { Wordmark } from '#/components/wordmark'
import { Button } from '#/components/ui/button'
import type { ItemStatus } from '#/lib/items'
import { cn } from '#/lib/utils'

/**
 * Home: presents the product and is where an Owner signs in (design/ui-design.html, Landing page).
 * Sending a signed-in Owner on to their Roadmaps comes with real sign-in (Task 21).
 */
export const Route = createFileRoute('/')({
  component: HomePage,
})

const wrap = 'mx-auto max-w-[1200px] px-4 sm:px-8'

// The mock Roadmap in the hero: [title, status, left %, width %].
const MOCK_ITEMS: [string, ItemStatus, number, number][] = [
  ['SSO & SCIM', 'shipped', 0, 30],
  ['Slack notifications', 'shipped', 12, 26],
  ['Mobile app', 'in_progress', 28, 64],
  ['Usage reports', 'in_progress', 50, 30],
  ['Public API v2', 'planned', 70, 30],
]

const MOCK_BAR: Record<ItemStatus, string> = {
  shipped: 'bg-foreground',
  in_progress: 'bg-[#3b82f6]',
  planned: 'border-2 border-dashed border-muted-soft bg-background',
}

function HomePage() {
  return (
    <>
      <div className={wrap}>
        <header className="flex h-16 items-center justify-between">
          <Wordmark />
          <Button variant="ghost" asChild>
            <a href="/oauth2/authorization/google">Sign in</a>
          </Button>
        </header>

        <section className="grid grid-cols-1 items-center gap-10 pt-12 pb-24 min-[900px]:grid-cols-[7fr_5fr] min-[900px]:gap-14 min-[900px]:pt-20">
          <div>
            <h1 className="font-display text-[44px] leading-[1.05] font-semibold tracking-[-1.2px] sm:text-[64px] sm:tracking-[-2px]">
              Show what's coming, and when.
            </h1>
            <p className="mt-5 mb-8 max-w-[470px] text-lg text-body">
              Keep a dated Roadmap, press Publish, share one link. Edits show up for Readers right away, and Internal
              Items never leave your editor.
            </p>
            <GoogleButton />
          </div>

          <div
            aria-hidden
            className="rounded-xl border border-border bg-background p-6 shadow-elevated"
          >
            <div className="flex items-baseline justify-between gap-3">
              <span className="text-lg font-semibold">Product roadmap 2026</span>
              <span className="text-[13px] font-medium text-muted-foreground">Published Oct 4</span>
            </div>
            <div className="mt-4 mb-1.5 grid grid-cols-6 border-b border-hairline-soft pb-1.5 text-[11px] text-muted-foreground">
              {['Jan', 'Mar', 'May', 'Jul', 'Sep', 'Nov'].map((month) => (
                <span key={month}>{month}</span>
              ))}
            </div>
            {MOCK_ITEMS.map(([title, status, left, width]) => (
              <div key={title} className="grid h-[30px] grid-cols-[118px_1fr] items-center text-xs font-medium">
                <span className="truncate">{title}</span>
                <div className="relative h-[30px]">
                  <div
                    className={cn('absolute top-2 h-3.5 rounded-full', MOCK_BAR[status])}
                    style={{ left: `${left}%`, width: `${width}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </section>

        <section aria-label="How it works" className="grid grid-cols-1 gap-6 pb-24 min-[900px]:grid-cols-3">
          <Feature
            demo={
              <>
                <StateBadge published={false} />
                <ArrowRight className="size-4" aria-hidden />
                <StateBadge published />
              </>
            }
            title="One Roadmap, no drafts"
          >
            It's Private until you publish it. After that, every edit is what Readers see. There's no second copy to
            keep in sync.
          </Feature>
          <Feature
            demo={
              <NeutralChip>
                <EyeOff className="size-3.5" aria-hidden />
                Internal
              </NeutralChip>
            }
            title="Internal Items stay out"
          >
            Mark an Item Internal and it's left out of the Public page before anything reaches the browser.
          </Feature>
          <Feature
            demo={
              <>
                <StatusChip status="planned" />
                <StatusChip status="in_progress" />
                <StatusChip status="shipped" />
              </>
            }
            title="Status is yours to set"
          >
            Dates describe the plan. Status is what you say is true, and it never changes on its own when a date
            passes.
          </Feature>
        </section>

        <section className="mb-24 flex flex-col items-center gap-4 rounded-lg bg-surface-card p-12 text-center">
          <h2 className="font-display text-[28px] font-semibold tracking-[-0.5px]">Publish your first Roadmap</h2>
          <p className="text-body">Sign in with Google. Your first sign-in sets everything up.</p>
          <GoogleButton />
        </section>
      </div>

      <footer className="bg-surface-dark py-10 text-sm text-[#a1a1aa]">
        <div className={cn(wrap, 'flex flex-wrap items-center justify-between gap-4')}>
          <Wordmark className="text-white [&_i]:after:bg-surface-dark" />
          <span>Roadmaps you can publish.</span>
        </div>
      </footer>
    </>
  )
}

function Feature({ demo, title, children }: { demo: React.ReactNode; title: string; children: React.ReactNode }) {
  return (
    <article className="flex flex-col gap-3 rounded-lg bg-surface-card p-8">
      <div className="mb-2 flex h-14 items-center gap-2">{demo}</div>
      <h2 className="text-lg font-semibold">{title}</h2>
      <p className="text-body">{children}</p>
    </article>
  )
}
