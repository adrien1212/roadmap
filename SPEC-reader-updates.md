# Spec: reader-updates

> Module `reader-updates` of `ROADMAP.md` (Now release, third module).
> Status: **approved 2026-10-11.** `SPEC.md` stays the source of truth for
> everything not changed here; the changes listed under "Changes to SPEC.md"
> have been applied to it. Depends on `changelog` (Shipped date)
> and `branding` (Brand), both built.

## Objective

A Reader opens a Public page once and is never told when something ships. This
module lets a Reader **subscribe by email** to a Published Roadmap and receive
**one email each time an Item becomes Shipped**, sent through **the Owner's own
SMTP server**. The Owner sees, per Roadmap, **who is subscribed** and **how
many**, and can remove a Reader.

## Decisions already taken (ROADMAP.md and 2026-10-11)

- Readers may leave an email, opt-in only, with no account. Double opt-in, and
  one-click unsubscribe in every email.
- One email per Item that becomes Shipped; never for an Internal Item.
- Emails go through the Owner's SMTP server, set in Settings. The password is
  stored encrypted and never shown again. "Send test email" checks it. Until a
  test succeeds, the Public page offers RSS only.
- Owner list per Roadmap: confirmed and pending Readers with their date.
  Unsubscribed Readers are deleted and neither listed nor counted (decided
  2026-10-11, after the first build).
- The Owner can remove a Reader. No export.
- Statistics: counts only (confirmed, pending) per Roadmap.
- When the SMTP server fails: the email is dropped and the Owner is warned.
  Subscriptions pause (RSS only on the Public page) until a test email
  succeeds.
- Copy says "subscribed Readers", never "subscribers".

## Assumptions (accepted on approval)

1. **Emails go out right after the save** that makes an Item Shipped, in the
   background (after the transaction commits). There is no grace period: an
   Item switched to Shipped by mistake has already been emailed.
2. **Pending subscriptions expire after 7 days** without confirmation and are
   deleted.
3. **Unsubscribing deletes the email address**, and nothing else is kept.
   Removing a Reader from the Owner's list does the same.
4. **The SMTP password is encrypted with AES-256-GCM** (JDK only) under a key
   from the `SMTP_SECRET_KEY` environment variable. Without the key, the server
   refuses to save SMTP settings ("Email to Readers is not available on this
   server") and everything else works.
5. **Mail is sent with `spring-boot-starter-mail`** (Jakarta Mail). This is a
   new dependency; approving this spec approves it. For local testing, a
   **Mailpit** service is added to `docker-compose.yml` (a local SMTP server
   with a web inbox).
6. **The test email goes to the Owner's account email** (from sign-in), or to
   the sender email when there is none.

## Language (changes)

**Reader** (changed):
Anyone who opens a Public page. Never authenticated. May choose to leave an
email address to subscribe to a Roadmap; otherwise never identified. Cannot
write anything else.
_Avoid_: customer, viewer, visitor, end user, subscriber, stakeholder

**Subscription**:
A Reader's email address on one Published Roadmap. **Pending** until the Reader
confirms it from the email they receive, then **confirmed**. Ends when the
Reader unsubscribes or the Owner removes it; the address is then deleted.
_Avoid_: follow, membership, mailing list, newsletter

**Subscribed Reader**:
A Reader with a confirmed Subscription. Only subscribed Readers are emailed.
_Avoid_: subscriber, follower, contact, lead, audience

**SMTP settings**:
The Owner's mail server, set once in Settings, used to email subscribed
Readers from the Owner's own address.
_Avoid_: mail provider, email integration

## Requirements

### SMTP settings (Owner, Settings screen)
- "Email to Readers" section, as in `design/roadmap-ui.html`: SMTP host, port,
  security (STARTTLS or SSL/TLS), username, password, sender name (defaults to
  the Brand), sender email; Save; "Send test email"; a state: Not set up /
  Saved, send a test email / Connected, test email sent {date} / Not working.
- The password is write-only: after saving it shows "Saved, encrypted" with
  "Replace"; the API never returns it.
- Saving changed settings sets the state to "Saved, send a test email" and
  pauses subscriptions until a test succeeds.
- **Allowed servers:** port 25, 465, 587 or 2525 only; the host must resolve
  only to public addresses (no loopback, private, link-local or unique-local
  ranges), checked when testing and before every connection. Connect and read
  timeouts of 10 s. (The Owner chooses the host our server connects to; without
  this, the feature lets anyone use our server to reach machines it shouldn't.)
- API: `GET /api/smtp-settings` (no password), `PUT /api/smtp-settings`
  (password optional; omitted keeps the saved one), `POST
  /api/smtp-settings/test`.

### Subscribe (Reader, Public page)
- When the Roadmap is Published and the Owner's SMTP state is Connected, the
  Public page shows "Get an email when something ships": an email field and
  Subscribe, with the hint "One email per shipped Item, from {sender name}.
  {Brand} receives your email address. Unsubscribe in one click." Otherwise it
  shows "Follow what ships" (RSS only), as today.
- `POST /api/public/roadmaps/{publicId}/subscriptions` with `{ "email" }`:
  - the email is trimmed and checked for a plausible format (else 400);
  - the answer is always the same (202, "Check your inbox"), whether the email
    is new, pending or already confirmed, so nobody can learn who is
    subscribed;
  - a confirmation email is sent for a new or pending Subscription, at most
    once per 10 minutes per address and Roadmap;
  - Private or unknown Roadmap → the Public page's 404; SMTP not Connected →
    409.
- **Confirmation email:** "Confirm your email for {Roadmap}", from the sender,
  with a button to `/r/{publicId}/confirm?token=…`. That page confirms
  (`POST /api/public/subscriptions/confirm`) and says "You're subscribed".
  An unknown or expired token shows "This link has expired".

### Shipped email (to subscribed Readers)
- When an Item becomes Shipped (its Shipped date is set), and the Item is not
  Internal, its Roadmap is Published and the Owner's SMTP state is Connected,
  every subscribed Reader of that Roadmap gets one email, after the save
  commits:
  - subject "Shipped: {Item title}"; from "{sender name} <{sender email}>";
  - the Brand's letter mark and name, the title, the Shipped chip and Category,
    the description (plain text), a "See the Roadmap" link to the Public page,
    and the footer: "You get this email because you subscribed to {Roadmap} by
    {Brand}. Unsubscribe in one click." and "Made with Product name";
  - HTML and plain-text parts; `List-Unsubscribe` and `List-Unsubscribe-Post`
    headers (RFC 8058 one-click).
- One email per subscribed Reader (never one email with everyone in To or Cc).
- If the SMTP server fails: the email is dropped, the state becomes "Not
  working" with the error, the Owner is warned in Settings and in the editor of
  each Roadmap with subscribed Readers, and the Public page goes back to RSS
  only.
- The Item dialog says "{N} subscribed Readers get an email about this Item
  when you save" when saving would send it (as in the mockup).

### Unsubscribe
- Every email links to `/r/{publicId}/unsubscribe?token=…`. Opening it
  unsubscribes at once (the page posts to `POST
  /api/public/subscriptions/unsubscribe`; a link preview that only fetches the
  page does not) and shows "You're unsubscribed", with "See the Roadmap".
- The one-click header posts to the same endpoint.
- Unsubscribing deletes the address. Tokens are random, at least 128-bit and URL-safe, like `publicId`.

### Subscribed Readers (Owner, editor)
- The Share dialog says "{N} Readers are subscribed" with a link to the new
  screen `/roadmaps/{id}/readers`.
- That screen shows the counts (confirmed, pending) and the list:
  email, Pending or Confirmed, and the date (subscribed / confirmed). Newest
  first.
- "Remove" on a row, after a confirm ("{email} won't get emails about this
  Roadmap anymore."), deletes the Subscription.
- API, scoped by `CurrentOwner` (another Owner's Roadmap → 404):
  `GET /api/roadmaps/{id}/subscriptions` → `{ counts, subscriptions[] }`,
  `DELETE /api/roadmaps/{id}/subscriptions/{subscriptionId}`.
- Deleting a Roadmap deletes its Subscriptions. Unpublishing keeps them but
  sends nothing while Private.

## Data model (new changesets)

```
smtp_settings   owner_id PK/FK, host, port, security, username,
                password_encrypted VARBINARY, from_name, from_email,
                state (NONE | UNTESTED | CONNECTED | NOT_WORKING),
                last_error, tested_at
subscription    id, roadmap_id FK (on delete cascade), email VARCHAR(254),
                state (PENDING | CONFIRMED), token CHAR(22) ascii_bin unique,
                created_at, confirmed_at, confirmation_sent_at;
                unique (roadmap_id, email)
```

## Changes to SPEC.md (applied)

- Language: Reader changed; add Subscription, Subscribed Reader, SMTP settings.
- Out of scope: "Reader subscriptions" moves in scope (email only; feedback
  stays out).
- Public page: subscribe box when SMTP is Connected; confirm and unsubscribe
  pages.
- Editor: Subscribed Readers screen; Settings: SMTP section.
- Boundaries: never email pending Readers, Internal Items or Private Roadmaps;
  never return the SMTP password; never show Subscriptions to Readers.
- Tech stack: `spring-boot-starter-mail`; Mailpit in `docker-compose.yml`;
  `SMTP_SECRET_KEY` environment variable.

## Tech stack, commands, project structure

As `SPEC.md`, plus `spring-boot-starter-mail` and a `mailpit` service in
`docker-compose.yml` (SMTP on 1025, web inbox on 8025; dev only, reachable
because the allowed-server check is relaxed for it in dev only).

```
backend/.../subscription/   → Subscription, repository, service, public + Owner controllers
backend/.../smtp/           → SmtpSettings, SecretBox (AES-GCM), SmtpServerGuard, mail sending
backend/.../subscription/ShippedEmails.java → who gets emailed when an Item ships (pure)
backend/.../subscription/EmailContent.java  → subject, HTML and text bodies (pure)
frontend/src/routes/_app/settings.tsx       → SMTP section
frontend/src/routes/_app/roadmaps.$roadmapId.readers.tsx → Subscribed Readers
frontend/src/routes/r.$publicId.confirm.tsx / .unsubscribe.tsx
frontend/src/components/recently-shipped.tsx → subscribe box
```

## Code style

As `SPEC.md`. Decisions that matter for privacy are pure functions with unit
tests, for example:

```java
/** Who gets an email when an Item changes: nobody unless it just became Shipped, visibly, on a Published Roadmap. */
static List<Subscription> recipients(Item item, ItemStatus before, SmtpState smtp, List<Subscription> subscriptions) {
    boolean ships = item.getStatus() == SHIPPED && before != SHIPPED;
    if (!ships || item.isInternal() || item.getRoadmap().getPublishedAt() == null || smtp != CONNECTED) return List.of();
    return subscriptions.stream().filter(Subscription::isConfirmed).toList();
}
```

## Testing strategy

Backend unit tests (JUnit, no Spring context, written first):
- Subscription: email normalised and validated; pending → confirmed; token
  random and URL-safe; pending older than 7 days expired.
- Recipients rule: only confirmed Readers; nobody for an Internal Item, a
  Private Roadmap, an Item already Shipped, or SMTP not Connected.
- Email content: subject; HTML-escaped Owner text; unsubscribe link and
  headers present; no other Reader's address.
- `SmtpServerGuard`: allowed ports; loopback, private, link-local and
  unique-local addresses refused.
- `SecretBox`: encrypt / decrypt round trip; a tampered value is rejected.

Manual, against the running app with Mailpit:
- Subscribe, confirm from the Mailpit inbox, ship an Item: the email arrives;
  an Internal Item or a Private Roadmap sends nothing; unsubscribe works and the
  counts change.
- The subscribe answer is the same for a new and an already confirmed email.
- An SMTP host such as `127.0.0.1` or port 22 is refused.
- Another Owner's Subscriptions return 404 (once auth exists).

Frontend: no automated tests (as in `SPEC.md`).

## Boundaries

- Always: email only confirmed Readers; check SMTP servers against the allowed
  list before connecting; keep the password encrypted; one email per Reader.
- Ask first: storing more about Readers than email and dates; export of the
  list; any change to the allowed SMTP ports.
- Never: return the SMTP password from the API; email about an Internal Item or
  a Private Roadmap; reveal to a Reader whether an email is already subscribed;
  put several Readers in one email.

## Success criteria

1. With working SMTP settings, a Reader can subscribe on a Public page, confirm
   from the email, and receive an email when an Item of that Roadmap becomes
   Shipped; they can unsubscribe in one click.
2. No email is ever sent for an Internal Item, for a Private Roadmap, to a
   pending Reader, or when the SMTP state is not Connected.
3. The Owner sees, per Roadmap, the confirmed and pending Readers with their
   dates and the confirmed / pending counts, and can remove a
   Reader.
4. The SMTP password is never returned by the API and is stored encrypted;
   SMTP hosts on private networks and non-SMTP ports are refused.
5. When the SMTP server fails, the Owner is warned and the Public page offers
   RSS only until a test email succeeds.
6. `./mvnw test` green with the new unit tests; `npm run build` clean.

## Decided on approval

1. Emails go out right after the save that makes an Item Shipped.
2. Pending Subscriptions expire after 7 days.
3. `spring-boot-starter-mail` and a Mailpit service in `docker-compose.yml` are
   approved.
