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
| `analytics` | Public page views, most-opened Items and how many Readers are subscribed, without identifying Readers | `reader-updates` |
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

## Decided (2026-10-11)

- `branding` is set **once, in Owner settings** (a Settings screen): product
  name and logo, used on every Public page and in emails.
- `reader-updates` sends **one email per Item that becomes Shipped** (never for
  an Internal Item), with double opt-in and one-click unsubscribe.
- Emails go through **the Owner's own SMTP server**, set in Settings (host,
  port, STARTTLS or SSL/TLS, username, password, sender name and email). The
  password is stored encrypted and never shown again. "Send test email" checks
  the settings. Until a test succeeds, the Public page offers RSS only, so no
  Reader subscribes to emails that can't be sent.
- The "Made with Product name" footer **always shows**; removing it may become
  a paid option with `teams-billing`.
- Copy says "subscribed Readers", never "subscribers" (SPEC's _Avoid_ list for
  Reader).

Mockups: `design/roadmap-ui.html` shows Settings (branding and SMTP), Recently shipped and
subscribe on the Public page, the email, and the unsubscribed page.

## Open questions (answered in each module's spec)

- `changelog`: "Recently shipped" shows the last 3 Shipped Items in the
  mockup; is that the rule, or the last N days? It needs a new `shippedAt`,
  set when an Item becomes Shipped.
- `reader-updates`: where does the key that encrypts SMTP passwords live, and
  how is it rotated? Which hosts and ports may the server connect to? (The
  Owner chooses the host, so block private network addresses and allow only
  SMTP ports 25, 465, 587 and 2525.) What happens to subscriptions when SMTP
  later stops working: pause and warn the Owner?
