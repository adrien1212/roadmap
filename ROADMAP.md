# Product Roadmap (after the MVP)

> Status: **approved 2026-10-11.** This is the capability map: module ids,
> dependencies and build order. Each module gets its own `SPEC-<module-id>.md`
> before it is planned or built. Authentication (Phase 4 of `tasks/plan.md`) is
> out of this document and lands independently.

## Who we build for

Indie founders and small SaaS teams who want a public Roadmap their Readers
trust, without running a product-management tool. They have one product, few
people, and little time to keep a Roadmap current.

## The gap after the MVP

The MVP proves the loop **an Owner publishes, Readers read**. It does not yet
give Readers a reason to come back. A Reader opens the Public page once, and
nothing tells them when something ships. The next release closes that gap.

## Decision: Readers may leave data, opt-in only

Today SPEC says a Reader is "never identified, cannot write anything". From
`reader-updates` on, a Reader may **choose** to leave an email address (to
subscribe) or a vote, still without an account or sign-in. SPEC's Language and
Boundaries are updated in `SPEC-reader-updates.md` before any code. Internal
Items and Owner data stay off the Public page, as before.

## Capability map

| Module id | What it gives users | Depends on |
|---|---|---|
| `changelog` | "Recently shipped" on the Public page, plus an RSS feed: proof the Roadmap gets delivered | — |
| `branding` | Product name and logo on the Public page and in emails; page title and link preview image for social and search | — |
| `reader-updates` | A Reader subscribes by email to a Roadmap and is emailed when Items ship; one-click unsubscribe | `changelog`, `branding` |
| `fuzzy-dates` | Plan by month or quarter, or Now / Next / Later, instead of exact days | — |
| `reader-feedback` | A Reader votes "I want this" on an Item, optionally with their email; the Owner sees demand per Item | `reader-updates` |
| `analytics` | Public page views, most-opened Items and the number of subscribers, without identifying Readers | `reader-updates` |
| `embed` | The Public page inside the Owner's own site or app | `branding` |
| `import` | CSV import first; Linear, Jira or GitHub later | — |
| `custom-domain` | `roadmap.theirproduct.com` | `branding` |
| `teams-billing` | Several people per account, and paid plans | authentication |

Dependencies point one way; there are no cycles.

## Build order

**Now — "Readers come back"**
1. `changelog`: no new infrastructure, visible immediately, and it supplies the
   events `reader-updates` sends.
2. `branding`: emails and links need the product's name and logo.
3. `reader-updates`: the release's goal.

**Next — "Owners see it works"**
`reader-feedback`, `analytics`, `fuzzy-dates`, `embed`

**Later**
`import`, `custom-domain`, `teams-billing`

## How we'll know the release worked

- A Published Roadmap with recently shipped Items shows them on its Public page
  and in its RSS feed.
- A Reader can subscribe with an email, confirm it, receive an email when an
  Item of that Roadmap becomes Shipped, and unsubscribe in one click.
- No email ever mentions an Internal Item.
- The Public page shows the Owner's product name and logo, and its link shows a
  preview when shared.

## Dogfooding

Once approved, this roadmap is also entered as a Roadmap in the app (one Item
per module, Status Planned, Now modules first) and published, so our own Public
page shows it.

## Open questions (answered in each module's spec)

- `changelog`: what counts as "recently": the last N Shipped Items, or the last
  N days? Should Status changes be timestamped (a new `shippedAt`)?
- `reader-updates`: which email provider (SMTP, Postmark, Resend…)? Double
  opt-in (recommended for GDPR)? One email per shipped Item, or a digest?
- `branding`: does the Owner set the product name and logo once (Owner
  settings) or per Roadmap?
- Does the "Made with Product name" footer stay on every Public page (word of
  mouth) or can the Owner remove it?
