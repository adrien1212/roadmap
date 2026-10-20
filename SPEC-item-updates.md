# Spec: item-updates

> New module `item-updates` (added to `ROADMAP.md`). Status:
> **approved 2026-10-20.** `SPEC.md` stays the source of truth for
> everything not changed here; the changes listed under "Changes to SPEC.md"
> have been applied to it. Depends on `reader-updates` (Subscriptions, SMTP
> settings, confirm and unsubscribe pages), built.

## Objective

A buyer asks sales or support "when will X ship?". Today the answer is a link
to the whole Public page, and the buyer either has to look for X or subscribe
to every Shipped email for the Roadmap. This module gives each Item its own
**Item page**, a link you can send, and **"Notify me when this ships"**: an
email each time that Item becomes Shipped, and nothing about other Items.

For the Owner, the number of Readers waiting for an Item is also a demand
signal, without votes or Reader accounts.

## Language (additions and changes)

**Item page**:
The anonymous page at `/r/{publicId}/items/{itemId}` that shows one
non-Internal Item of a Published Roadmap. Part of the Public page: same rules,
same 404.
_Avoid_: item detail, permalink (in the UI), card, ticket page

**Subscription** (changed):
A Reader's email address on one Published Roadmap (**Roadmap Subscription**)
or on one Item of it (**Item Subscription**). Pending until the Reader
confirms it from the email they receive, then confirmed. A Roadmap
Subscription of either kind ends when the Reader unsubscribes or the Owner
removes it; the address is then deleted.
_Avoid_: follow, watch, membership, mailing list, newsletter

**Waiting Readers**:
The confirmed Item Subscriptions of an Item, as a count shown to the Owner.
Never shown to Readers.
_Avoid_: votes, followers, watchers, demand, interest

UI copy for a Reader: "Notify me when this ships", never "follow" or "watch".

## Requirements

### Item page (Reader)
- `/r/{publicId}/items/{itemId}` shows, in the Public page's style: the Brand
  (if any), the Roadmap name linking to the Public page, the Item title, its
  Status and Category chips, its dates, its description and, when Shipped, its
  Shipped date.
- Built from the Public page payload (`GET /api/public/roadmaps/{publicId}`):
  no new endpoint. An Item that is not in it (Internal, deleted, from another
  Roadmap, or unknown) and a Private or unknown Roadmap give the Public page's
  404, identical.
- Page title "{Item title} · {Roadmap name}" (+ " · {Brand}" when set);
  `og:title` the same, `og:description` the Item's description cut at 200
  characters (or the Public page's description when empty); `og:type`
  `website`; `twitter:card` `summary`. Rendered on the server, like the Public
  page, so links unfurl in Slack and email.
- **Getting the link:**
  - Public page: a "Copy link" button on each Item, in the list view and in
    the timeline row once expanded.
  - Editor: "Copy Item link" in the Item dialog, only when the Roadmap is
    Published and the Item is not Internal (otherwise the link would 404).

### Notify me (Reader, Item page)
- The Item page shows the box **"Notify me when this ships"** when the Roadmap
  is Published, the Owner's SMTP state is Connected, and the Item is not
  Shipped. It has an email field and "Notify me", and the hint: "An email when
  this Item ships, from {sender name}. {Brand} receives your email address.
  Unsubscribe in one click."
- When the Item is Shipped, or SMTP is not Connected, the box is replaced by
  the Roadmap's "Follow what ships" box (RSS, and the Roadmap subscribe form
  when SMTP is Connected), as on the Public page.
- The box is on the Item page only, not on each row of the Public page. A
  Reader who wants it opens the Item ("Copy link" sits next to an "Open" link).
- `POST /api/public/roadmaps/{publicId}/items/{itemId}/subscriptions` with
  `{ "email" }`. It behaves exactly like the Roadmap subscribe endpoint: email
  trimmed and checked (else 400); always 202 "Check your inbox"; a
  confirmation email at most once per 10 minutes per address and Item; unknown
  or Internal Item, Private or unknown Roadmap → the Public page's 404; SMTP not
  Connected → 409. An already Shipped Item → 409 ("This Item has already
  shipped").
- **One Item per Item Subscription.** A Reader who wants several Items
  subscribes on each Item page; each is its own Subscription with its own
  confirmation and token. The same address may also hold a Roadmap
  Subscription.
- **Confirmation email:** "Confirm your email to hear when {Item title}
  ships", button to the existing `/r/{publicId}/confirm?token=…`. The confirmed
  page says "You'll get an email when {Item title} ships."
- Pending Item Subscriptions expire after 7 days, like Roadmap ones.

### Shipped email
- When an Item becomes Shipped (same conditions as today: not Internal,
  Roadmap Published, SMTP Connected), emails go to:
  1. every confirmed Roadmap Subscription of its Roadmap (unchanged);
  2. every confirmed Item Subscription of that Item.
- **No de-duplication.** An address with a Roadmap Subscription and an Item
  Subscription to that Item gets **two emails**: each comes from a
  Subscription the Reader asked for, and each one's footer says which.
- The Item Subscription email has the same content as today's Shipped email,
  with the footer: "You get this email because you asked to hear when {Item
  title} ships. Unsubscribe in one click." and a "Get every update from
  {Roadmap}" link to the Public page. It carries the unsubscribe link and the
  `List-Unsubscribe` / `List-Unsubscribe-Post` headers, like the Roadmap email.
- **The Item Subscription stays after the email.** If the Item leaves Shipped
  and becomes Shipped again, the Reader is emailed again, until they
  unsubscribe. A failed send is dropped and the Owner warned, as today.
- An Item Subscription is never emailed for an Internal Item. If the Item
  becomes Internal, its Item Subscriptions are kept but silent; if it becomes
  visible again, they work again.
- Deleting an Item deletes its Item Subscriptions. Deleting a Roadmap deletes
  all of its Subscriptions (unchanged).

### Unsubscribe
- As for Roadmap Subscriptions: every Shipped email of an Item Subscription
  links to the existing `/r/{publicId}/unsubscribe?token=…`, and the one-click
  header posts to the same endpoint. The unsubscribed page says "You won't get
  emails about {Item title} anymore." Unsubscribing deletes the address.
- So the confirm and unsubscribe pages can name the Item, `POST
  /api/public/subscriptions/confirm` and `…/unsubscribe` answer 200 with
  `{ "itemTitle" }` (null for a Roadmap Subscription, or when the Item is now
  Internal) instead of 204.
- Unsubscribing from an Item Subscription leaves the same address's Roadmap
  Subscription (if any) untouched, and the other way round: each has its own
  token.

### Subscribed Readers (Owner, editor)
- `/roadmaps/{id}/readers` lists both kinds. A new "For" column says "Roadmap"
  or the Item title. The counts (confirmed, pending) count Subscriptions of
  both kinds, with a split: "12 confirmed (8 on the Roadmap, 4 on Items)".
  "Remove" works for both.
- The Item dialog shows "{N} Readers are waiting for this Item" (confirmed Item
  Subscriptions) when N > 0, and the existing "…get an email about this Item
  when you save" counts both kinds (emails sent, not distinct people).
- Owner API: `GET /api/roadmaps/{id}/subscriptions` gains, per Subscription,
  `itemId` and `itemTitle` (null for a Roadmap Subscription) and the split
  counts. Waiting Readers are counted by the editor from that list.

## Data model (new changeset)

```
015-item-subscription.sql
subscription  + item_id  BIGINT NULL, FK item(id) ON DELETE CASCADE
              + item_key BIGINT AS (COALESCE(item_id, 0)) STORED
              - unique (roadmap_id, email)
              + unique (roadmap_id, item_key, email)
```

`item_key` exists because MariaDB treats NULLs as distinct in a unique key, so
`(roadmap_id, item_id, email)` would allow two Roadmap Subscriptions for one
address. The unique key keeps one Subscription per address per Roadmap and
one per address per Item.

## Code style

As `SPEC.md`. The recipients rule stays a pure function, extended:

```java
/** Roadmap Subscriptions and this Item's Subscriptions, confirmed only; one email per Subscription, never merged. */
static List<Subscription> recipients(Item item, SmtpState smtpState,
        List<Subscription> roadmapSubscriptions, List<Subscription> itemSubscriptions) { … }
```

Placement:
```
backend/.../subscription/Subscription.java     → optional item; forItem(…) factory; isForItem()
backend/.../subscription/ShippedEmails.java    → both kinds
backend/.../subscription/EmailContent.java     → Item Subscription footer, confirmation text
backend/.../subscription/PublicSubscriptionController.java → Item endpoint
frontend/src/routes/r.$publicId_.items.$itemId.tsx         → Item page
frontend/src/components/notify-me.tsx                      → the box
```

## Testing strategy

Backend unit tests (JUnit, no Spring context, written first):
- Recipients: an address with both kinds gets two entries; Item Subscriptions
  of another Item get none; pending ones none; Internal Item, Private Roadmap,
  SMTP not Connected → none of either kind.
- Item Subscription: created for one Item; refused for a Shipped Item;
  expiry and resend delay as for Roadmap Subscriptions.
- Email content: Item footer; unsubscribe link and headers present for both
  kinds; Roadmap email unchanged; Owner text escaped.

Manual, against the running app with Mailpit:
- Notify me on an Item, confirm, ship it: one email. Set it back to In
  progress, then Shipped again: a second email. Unsubscribe from it: no more
  emails, and it is gone from the Readers screen.
- Same address on the Roadmap and the Item: two emails.
- Internal Item, Private Roadmap and unknown Item: the Item page and the
  endpoint 404 identically.
- The Item page's link preview shows the Item title (`curl` the HTML).
- "Copy Item link" is absent for an Internal Item or a Private Roadmap.

Frontend: no automated tests (as in `SPEC.md`).

## Changes to SPEC.md (applied)

- Language: add Item page, Waiting Readers; change Subscription (two kinds).
- Public page: Item page, "Copy link", Notify me; confirm / unsubscribe pages
  cover Item Subscriptions.
- Settings: the Readers screen lists both kinds.
- Boundaries: never show Waiting Readers or any Subscription to Readers.
- `ROADMAP.md`: add `item-updates` (depends on `reader-updates`) to the
  capability map, in Now with `fuzzy-dates`.

## Boundaries

- Always: build the Item page from the Public page payload (Internal Items
  never reach the browser); same 404 for every unknown or hidden case; one
  token per Subscription, so unsubscribing ends only that one.
- Ask first: de-duplicating emails across kinds; Notify me for anything other
  than Shipped (dates moved, Status In progress); a Notify me box on Public page
  rows.
- Never: show Waiting Readers or who waits for an Item to a Reader; email an
  Item Subscription about anything but its Item becoming Shipped; keep the
  address after the Reader unsubscribes.

## Success criteria

1. Every non-Internal Item of a Published Roadmap has an Item page whose link
   unfurls with the Item title; an Internal or unknown Item, or a Private
   Roadmap, gives the Public page's 404.
2. A Reader can ask to be notified about one Item, confirm, and receive an
   email each time it becomes Shipped, until they unsubscribe in one click.
3. An address subscribed to the Roadmap and to that Item receives two emails.
4. The Owner sees both kinds of Subscriptions, and how many Readers wait for
   each Item.
5. `./mvnw test` green with the new unit tests; `npm run build` clean.

## Decided (2026-10-20)

1. An Item Subscription covers exactly one Item.
2. A Reader with a Roadmap Subscription and an Item Subscription receives two
   emails when that Item ships; there is no de-duplication.
3. An Item Subscription lasts until the Reader unsubscribes (or the Owner
   removes it): an Item that leaves Shipped and comes back is emailed again.
4. The Item page lives at `/r/{publicId}/items/{itemId}` (this approves the new
   public URL format). Item ids are sequential, but an id only resolves under
   its own Roadmap's `publicId`, and the payload already exposes them.

## Progress

- **Built (2026-10-20):** everything above. Verified by unit tests, and with
  curl and Mailpit against a second local backend: the Item endpoint's 404s and
  409s, two Subscriptions (and two confirmation emails) for one address on the
  Roadmap and an Item, confirm / unsubscribe naming the Item.
- **To verify by hand** (needs a signed-in session): shipping an Item emails
  both kinds; the Readers screen and the Item dialog's Waiting Readers.
