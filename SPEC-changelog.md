# Spec: changelog

> Module `changelog` of `ROADMAP.md` (Now release, first module).
> Status: **approved 2026-10-11.** `SPEC.md` stays the source of truth for
> everything not changed here; the changes listed under "Changes to SPEC.md"
> have been applied to it.

## Objective

Give Readers proof that the Roadmap gets delivered, and a way to follow it
without leaving any data. Today a Reader sees Shipped Items mixed with the
plan and cannot tell what shipped recently or follow new ones.

For indie and small SaaS teams (see `ROADMAP.md`), this module adds:

1. **Shipped date**: the moment an Item became Shipped, recorded by the app.
2. **Recently shipped** on the Public page: the last Shipped Items, newest
   first.
3. **RSS feed** per Published Roadmap, for feed readers.

It also supplies the "an Item became Shipped" event that `reader-updates` will
email about.

## Language (additions)

**Shipped date** (`shippedAt`):
The instant an Item's Status last became Shipped. Set by the app when the Owner
changes the Status to Shipped, cleared when it leaves Shipped. It is a record of
the Owner's assertion, never inferred from the Item's dates, and the Owner does
not edit it directly.
_Avoid_: release date, delivery date, done date

**Recently shipped**:
The last 3 non-Internal Items of a Published Roadmap that have a Shipped date,
newest first, shown on its Public page.
_Avoid_: changelog (in the UI), release notes, what's new

**Feed**:
The RSS 2.0 document of a Published Roadmap's recently Shipped Items, for feed
readers. Anonymous, like the Public page.
_Avoid_: newsletter, stream

"Changelog" remains the module id only; the UI says "Recently shipped" and "RSS".

## Requirements

### Shipped date
- Changing an Item's Status to Shipped (on create or on edit) sets `shippedAt`
  to now. Saving an Item that is already Shipped keeps its `shippedAt`.
  Changing the Status away from Shipped clears it.
- Items already Shipped before this module have no Shipped date (null). They
  are not in Recently shipped or the Feed until their Status becomes Shipped
  again. No date is invented.
- The Item dialog shows "Shipped Oct 2, 2026" under the Status when it has a
  Shipped date (read-only).

### Recently shipped (Public page)
- A "Recently shipped" section between the header and the filters: up to 3
  Items, each with a Shipped chip, its title, its Category chip and its Shipped
  date.
- Internal Items never appear (they never reach the browser). The section is
  hidden when no Item qualifies.
- It ignores the Category and Status filters below it.
- Next to it, "Follow what ships" offers the RSS link (the email subscribe box
  comes with `reader-updates`).

### Public API
- `GET /api/public/roadmaps/{publicId}` adds `shippedAt` (ISO instant or null)
  to each Item. Nothing else changes; Internal Items are still never sent.

### Feed
- `GET /api/public/roadmaps/{publicId}/feed.xml` returns RSS 2.0
  (`application/rss+xml; charset=UTF-8`):
  - channel: title = Roadmap name, link = Public page URL, description,
    `lastBuildDate` = newest Shipped date;
  - up to 20 items: non-Internal Items with a Shipped date, newest first; each
    has title, description (plain text), link = Public page URL, `pubDate` =
    Shipped date, and a stable `guid` = `{publicId}-{itemId}`
    (`isPermaLink="false"`).
- Private or unknown `publicId` → 404, identical to the Public page's.
- Owner text is XML-escaped. No Owner data.
- `Cache-Control: public, max-age=300` and an `ETag`; a matching
  `If-None-Match` gets 304.
- The Public page links to the feed (`<link rel="alternate"
  type="application/rss+xml">` in its head, and the "RSS feed" link).
- Absolute URLs use the public origin (`forward-headers-strategy`, as for the
  OAuth redirects).

## Changes to SPEC.md (applied)

- Language: add Shipped date, Recently shipped, Feed.
- Public page: add the Recently shipped section and the RSS link.
- Public API payload: Items gain `shippedAt`.
- New anonymous endpoint: `/api/public/roadmaps/{publicId}/feed.xml`.

## Tech stack, commands, project structure

Unchanged from `SPEC.md`. No new dependency: the Feed XML is written by a small
builder with an escaper, not a library (Rome would need approval).

Placement:
```
backend/.../item/Item.java                    → shippedAt + changeStatus(status, now)
backend/.../item/ItemRepository.java          → findRecentlyShipped(roadmap, limit)
backend/.../publicpage/RssFeed.java           → pure builder (unit-tested)
backend/.../publicpage/PublicPage*.java       → shippedAt in payload, feed endpoint
backend/src/main/resources/db/changelog/changes/005-item-shipped-at.sql
frontend/src/routes/r.$publicId.tsx           → Recently shipped, RSS link, head link
frontend/src/components/editor/item-dialog.tsx → read-only Shipped date
```

## Code style

As in `SPEC.md`. The domain rule lives in the entity and takes the clock as an
argument so it is testable without Spring:

```java
public void changeStatus(ItemStatus status, Instant now) {
    if (status == null) throw new ValidationException("Status is required");
    if (status == ItemStatus.SHIPPED && this.status != ItemStatus.SHIPPED) shippedAt = now;
    if (status != ItemStatus.SHIPPED) shippedAt = null;
    this.status = status;
}
```

## Testing strategy

Backend unit tests (JUnit, no Spring context, written first):
- Shipped date: becoming Shipped sets it; staying Shipped keeps it; leaving
  Shipped clears it; a new Item created as Shipped gets it.
- `RssFeed`: newest first; at most what it is given; XML special characters in
  titles and descriptions are escaped; `guid` is stable; no Internal Item in the
  output.
- Public mapping: `shippedAt` is mapped; Internal Items still dropped.

Manual, against the running app:
- An Item switched to Shipped appears first in Recently shipped and in the Feed.
- An Internal Shipped Item appears in neither.
- The Feed of a Private Roadmap and of an unknown id both 404, identically.
- A feed reader (or `curl -H 'If-None-Match: …'`) gets 304 when nothing changed.

Frontend: no automated tests (as in `SPEC.md`).

## Boundaries

- Always: filter Internal Items and Private Roadmaps on the backend, in the
  payload and in the Feed; escape Owner text in XML.
- Ask first: adding a feed library; letting the Owner edit Shipped dates.
- Never: infer a Shipped date from the Item's dates; backfill invented dates for
  Items shipped before this module.

## Success criteria

1. Switching an Item to Shipped records a Shipped date; switching it away
   clears it.
2. A Published Roadmap's Public page shows its last 3 non-Internal Shipped
   Items, newest first, with their Shipped dates; the section is absent when
   there are none.
3. `/api/public/roadmaps/{publicId}/feed.xml` is valid RSS 2.0 listing the same
   Items (up to 20) and can be added to a feed reader from the Public page URL.
4. Neither the payload nor the Feed ever contains an Internal Item; a Private or
   unknown Roadmap's Feed returns the same 404 as its Public page.
5. `./mvnw test` green with the new unit tests; `npm run build` clean.

## Decided on approval

1. Recently shipped is the **last 3** Shipped Items, not a time window, so a
   Roadmap that ships rarely still shows something.
2. Items Shipped before this module keep no Shipped date and stay out of
   Recently shipped and the Feed; no date is invented. Letting the Owner set one
   stays an "Ask first".
3. The Feed lists up to **20** Items.
