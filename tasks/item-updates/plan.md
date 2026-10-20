# Implementation Plan: item-updates

## Overview

Build module `item-updates` from `SPEC-item-updates.md` (approved 2026-10-20).
The Item page and "Copy link" are already built (see the spec's Progress).
What remains: Item Subscriptions ("Notify me when this ships"), their Shipped
emails, and the Owner's view of them.

Tasks live in `tasks/item-updates/todo.md`. All are _Assignee: Claude_.

## Architecture Decisions

- **One `Subscription` entity, an optional `item`.** Same token, confirm,
  expiry, resend delay and unsubscribe as Roadmap Subscriptions; only who it
  covers changes. `Subscription.pendingForItem(item, email, now)`.
- **Uniqueness with a stored generated column** `item_key = COALESCE(item_id,
  0)` and `UNIQUE (roadmap_id, item_key, email)` (changeset
  `015-item-subscription.sql`). Not mapped in JPA.
- **Lookups split by kind**: `findByRoadmapAndItemIsNullAndEmail` and
  `findByItemAndEmail`; the Roadmap recipients query becomes
  `findAllByRoadmapAndItemIsNullAndState`, so Item Subscriptions never receive
  the whole Roadmap's emails.
- **`ShippedEmails.recipients(item, smtp, roadmapSubs, itemSubs)`** returns
  both lists concatenated, confirmed only, no de-duplication.
- **`EmailContent`** gains `itemConfirmation(...)` and a `forItem` flag on the
  Shipped email that switches the footer; both keep the unsubscribe link and
  RFC 8058 headers.
- **Confirm / unsubscribe answer `{ itemTitle }`** (200) so the pages name the
  Item; null for Roadmap Subscriptions or an Item now Internal.
- **Owner side**: `SubscriptionResponse` gains `itemId`, `itemTitle`;
  `Counts` gains `confirmedOnItems`. Waiting Readers are counted in the
  editor from that list (it is already loaded there for the email hint), so
  the `item` package does not depend on `subscription`.
- **Item deletion** cascades in the database (`ON DELETE CASCADE` on
  `item_id`), as Roadmap deletion does.

## Dependency graph

```
Task 1  Item Subscriptions: schema, entity, subscribe endpoint (backend)
  ├── Task 2  Shipped emails for both kinds (backend)
  ├── Task 3  Confirm / unsubscribe name the Item (backend + pages)
  ├── Task 4  Notify me box on the Item page (frontend)
  └── Task 5  Owner: Readers screen "For" column, Waiting Readers (backend + frontend)
```

## Task List

### Phase 1: Subscribe to one Item
- [x] Task 1: Item Subscriptions (backend)
- [x] Task 4: Notify me box on the Item page

### Phase 2: Emails and pages
- [x] Task 2: Shipped emails for both kinds
- [x] Task 3: Confirm and unsubscribe name the Item

### Phase 3: Owner
- [x] Task 5: Readers screen and Waiting Readers

### Checkpoint: Complete (Success Criteria of SPEC-item-updates.md)
- [x] `./mvnw test` green, `npm run build` clean
- [ ] Criteria 1–4 verified with Mailpit (human, with a signed-in session)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Item Subscribers receive every Shipped email of the Roadmap | High | Roadmap recipients query filters `item is null`; `ShippedEmailsTest` |
| Notify me reveals an Internal Item | High | Endpoint resolves the Item among the Roadmap's non-Internal Items; same 404 |
| Unique key lets duplicate Roadmap Subscriptions in (NULL ≠ NULL) | Med | Generated `item_key` column in the unique key |
| Confirm/unsubscribe status change (204 → 200) breaks the pages or mail clients | Low | Pages updated in the same task; RFC 8058 only needs a 2xx |
