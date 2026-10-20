# Tasks: item-updates

Spec: `SPEC-item-updates.md`. Plan: `tasks/item-updates/plan.md`. All tasks are
_Assignee: Claude_. Backend rules are written test first (plain JUnit, no
Spring context).

Already built (2026-10-20): the Item page, "Copy link" on the Public page, the
embed and the Item dialog.

## Phase 1: Subscribe to one Item

### Task 1: Item Subscriptions (backend)

**Acceptance criteria:**
- [x] Changeset `015-item-subscription.sql`: `item_id` (FK, cascade),
      generated `item_key`, unique `(roadmap_id, item_key, email)`.
- [x] `Subscription.pendingForItem(item, email, now)`, `getItem()`,
      `isForItem()`.
- [x] `POST /api/public/roadmaps/{publicId}/items/{itemId}/subscriptions`:
      202 always; 404 for Private/unknown Roadmap or unknown/Internal Item;
      409 when SMTP not Connected or the Item is Shipped; resend delay per
      address and Item.

**Verification:** `SubscriptionTest` first; `./mvnw test`; curl 404s
**Dependencies:** None
**Estimated scope:** M

### Task 4: Notify me box on the Item page

**Acceptance criteria:**
- [x] "Notify me when this ships" (email + Notify me, the spec's hint) when
      subscriptions are open and the Item is not Shipped; otherwise the
      Roadmap's box.
- [x] "Check your inbox" after submit; errors shown in the form.

**Verification:** build; curl the Item page; browser (human)
**Dependencies:** Task 1
**Estimated scope:** S

## Phase 2: Emails and pages

### Task 2: Shipped emails for both kinds

**Acceptance criteria:**
- [x] Recipients: confirmed Roadmap Subscriptions + confirmed Item
      Subscriptions of the Item; no de-duplication; same conditions.
- [x] Item Subscription email: Item footer, "Get every update" link,
      unsubscribe link and headers.
- [x] Confirmation email for an Item Subscription ("Confirm your email to
      hear when {Item} ships").
- [x] Roadmap recipients exclude Item Subscriptions.

**Verification:** `ShippedEmailsTest`, `EmailContentTest` first; `./mvnw test`
**Dependencies:** Task 1
**Estimated scope:** M

### Task 3: Confirm and unsubscribe name the Item

**Acceptance criteria:**
- [x] Both endpoints answer 200 `{ itemTitle }` (null for a Roadmap
      Subscription or an Internal Item).
- [x] The confirm page says "You'll get an email when {Item} ships." and the
      unsubscribe page "You won't get emails about {Item} anymore." for Item
      Subscriptions; Roadmap texts unchanged.

**Verification:** build; `./mvnw test`
**Dependencies:** Task 1
**Estimated scope:** S

## Phase 3: Owner

### Task 5: Readers screen and Waiting Readers

**Acceptance criteria:**
- [x] `GET /api/roadmaps/{id}/subscriptions`: each row has `itemId`,
      `itemTitle`; counts gain `confirmedOnItems` (the split).
- [x] The Readers screen shows a "For" column (Roadmap or Item title) and
      "12 confirmed (8 on the Roadmap, 4 on Items)".
- [x] Waiting Readers counted in the editor from the Readers list it already
      loads (no field on `ItemResponse`); the Item dialog shows "{N} Readers are
      waiting for this Item" when N > 0; the "get an email when you save"
      count adds the Item's Waiting Readers.

**Verification:** build; `./mvnw test`; browser (human)
**Dependencies:** Task 1
**Estimated scope:** M
