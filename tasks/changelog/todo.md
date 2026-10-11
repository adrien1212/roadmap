# Tasks: changelog

Spec: `SPEC-changelog.md`. Plan: `tasks/changelog/plan.md`. All tasks are
_Assignee: Claude_. Backend rules are written test first (plain JUnit, no
Spring context).

## Phase 1: Shipped date

### Task 1: Shipped date on Items (backend)

**Description:** Record when an Item becomes Shipped. Add the column, the rule
in the entity, and expose `shippedAt` in the editor's Item API.

**Acceptance criteria:**
- [x] Changeset `005-item-shipped-at.sql` adds nullable `item.shipped_at
      DATETIME(6)`; no backfill.
- [x] `Item.changeStatus(status, now)`: becoming Shipped sets `shippedAt = now`;
      staying Shipped keeps it; leaving Shipped clears it. `ItemService` passes
      `Instant.now()` on create and replace.
- [x] `ItemResponse` includes `shippedAt` (ISO instant or null).

**Verification:**
- [x] Unit tests first, then green: `./mvnw test -Dtest=ItemTest`
- [x] `./mvnw test`
- [x] Manual: `PUT` an Item to `shipped` → `shippedAt` set; `PUT` it again as
      `shipped` → unchanged; `PUT` to `planned` → null.

**Dependencies:** None
**Files likely touched:** `005-item-shipped-at.sql`, `db.changelog-master.yaml`,
`Item.java`, `ItemService.java`, `ItemController.java`, `ItemTest.java`
**Estimated scope:** M

### Task 2: Shipped date in the Item dialog

**Description:** Show the Owner when an Item shipped.

**Acceptance criteria:**
- [x] `Item` type gains `shippedAt: string | null`.
- [x] The Item dialog shows "Shipped Oct 2, 2026" under the Status when the
      Item has a Shipped date (read-only; not shown for a new Item).

**Verification:**
- [x] `npx tsc --noEmit`, `npm run build`
- [x] Manual: open a Shipped Item → its date shows; an Item shipped before this
      module shows none.

**Dependencies:** Task 1
**Files likely touched:** `frontend/src/lib/items.ts`,
`frontend/src/components/editor/item-dialog.tsx`
**Estimated scope:** XS

### Checkpoint: Shipped date
- [x] `./mvnw test` green, `npm run build` clean
- [x] Switching an Item to Shipped and back sets then clears its date

## Phase 2: Readers see what shipped

### Task 3: Recently shipped on the Public page

**Description:** Send the Shipped date to Readers and show the last 3 Shipped
Items above the filters.

**Acceptance criteria:**
- [x] `PublicItem` gains `shippedAt`; the exact-fields test is updated to the
      new list, and Internal Items are still dropped.
- [x] The Public page shows "Recently shipped": up to 3 Items with a Shipped
      date, newest first, each with a Shipped chip, title, Category chip and
      date; hidden when none; unaffected by the filters.

**Verification:**
- [x] Unit tests first, then green: `./mvnw test -Dtest=PublicPageTest`
- [x] `npm run build`; curl the SSR HTML of `/r/{publicId}` for the section
- [x] Manual: ship an Internal Item → it does not appear.

**Dependencies:** Task 1
**Files likely touched:** `PublicPage.java`, `PublicPageTest.java`,
`frontend/src/lib/public-page.ts`, `frontend/src/routes/r.$publicId.tsx`,
`frontend/src/components/recently-shipped.tsx`
**Estimated scope:** M

### Task 4: RSS feed endpoint

**Description:** `GET /api/public/roadmaps/{publicId}/feed.xml` returns the
Feed of a Published Roadmap.

**Acceptance criteria:**
- [x] `ItemRepository.findRecentlyShipped(roadmap, limit)`: non-Internal,
      Shipped, with a Shipped date, newest first; called with 20.
- [x] `RssFeed.of(...)` builds RSS 2.0 (channel title, link, description,
      `lastBuildDate`; items with title, description, link, `pubDate`,
      `guid = {publicId}-{itemId}`), XML-escaping Owner text.
- [x] Endpoint: `application/rss+xml; charset=UTF-8`,
      `Cache-Control: public, max-age=300`, `ETag` with 304 on match; Private
      or unknown → the same 404 as the Public page; links use the public origin.

**Verification:**
- [x] Unit tests first, then green: `./mvnw test -Dtest=RssFeedTest`
- [x] `./mvnw test`
- [x] Manual through port 3000: `curl -i .../feed.xml` (200, links on :3000),
      again with `If-None-Match` (304), Private and unknown ids (identical 404).

**Dependencies:** Task 1
**Files likely touched:** `ItemRepository.java`, `RssFeed.java`,
`RssFeedTest.java`, `PublicPageService.java`, `PublicPageController.java`
**Estimated scope:** M

### Task 5: Feed links on the Public page

**Description:** Let Readers and feed readers find the Feed.

**Acceptance criteria:**
- [x] The `/r/$publicId` head has `<link rel="alternate"
      type="application/rss+xml">` to the Feed.
- [x] Next to Recently shipped, "Follow what ships" with an "RSS feed" link
      (always shown on a Public page, even when nothing has shipped yet).

**Verification:**
- [x] `npm run build`; curl the SSR HTML for the alternate link
- [x] Manual: add the Public page URL to a feed reader → it finds the Feed.

**Dependencies:** Tasks 3, 4
**Files likely touched:** `frontend/src/routes/r.$publicId.tsx`,
`frontend/src/components/recently-shipped.tsx`
**Estimated scope:** XS

### Checkpoint: Complete (Success Criteria of SPEC-changelog.md)
- [x] 1. Switching an Item to Shipped records a Shipped date; switching it away
      clears it.
- [x] 2. The Public page shows the last 3 non-Internal Shipped Items, newest
      first; absent when none.
- [x] 3. The Feed is valid RSS 2.0 with the same Items (up to 20) and is found
      from the Public page URL.
- [x] 4. No Internal Item in the payload or the Feed; a Private or unknown
      Roadmap's Feed returns the same 404 as its Public page.
- [x] 5. `./mvnw test` green; `npm run build` clean.
- [ ] Review with human
