# Tasks: reader-updates

Spec: `SPEC-reader-updates.md`. Plan: `tasks/reader-updates/plan.md`. All tasks
are _Assignee: Claude_. Backend rules are written test first (plain JUnit, no
Spring context). Manual email checks use Mailpit (`http://localhost:8025`).

## Phase 1: The Owner's SMTP server

### Task 1: Encryption and SMTP server guard

**Description:** The two security building blocks, pure and tested: encrypt the
SMTP password, and refuse SMTP servers that are not public mail servers.

**Acceptance criteria:**
- [x] `SecretBox`: AES-256-GCM with a random nonce per value; key from
      `SMTP_SECRET_KEY` (base64, 32 bytes); round trip works; a tampered value
      or a wrong key is rejected; a missing key gives a clear error.
- [x] `SmtpServerGuard`: ports 25, 465, 587, 2525 only; a host resolving to any
      loopback, private, link-local, unique-local, CGNAT, multicast or wildcard
      address is refused, unless private hosts are allowed (dev).

**Verification:**
- [x] Unit tests first, then green: `./mvnw test -Dtest='SecretBoxTest,SmtpServerGuardTest'`

**Dependencies:** None
**Files likely touched:** `smtp/SecretBox.java`, `smtp/SmtpServerGuard.java`,
their tests
**Estimated scope:** S

### Task 2: SMTP settings API

**Description:** Store the Owner's SMTP settings, with the password encrypted
and never returned.

**Acceptance criteria:**
- [x] Changeset `007-smtp-settings.sql`; `SmtpSettings` entity (one per Owner)
      with state NONE / UNTESTED / CONNECTED / NOT_WORKING.
- [x] `GET /api/smtp-settings` (no password; `passwordSaved` flag),
      `PUT /api/smtp-settings` (password optional, omitted keeps it; host,
      port and sender email validated; saving sets UNTESTED); scoped by
      `CurrentOwner`.
- [x] Without `SMTP_SECRET_KEY`, `PUT` answers 503 "Email to Readers is not
      available on this server".

**Verification:**
- [x] Unit tests first for the settings rules (port, sender email, state on
      change): `./mvnw test`
- [x] Manual: `PUT` then `GET` → no password in the JSON; the DB column holds
      ciphertext.

**Dependencies:** Task 1
**Files likely touched:** `007-smtp-settings.sql`, `db.changelog-master.yaml`,
`smtp/SmtpSettings.java` (+ repository, test), `smtp/SmtpSettingsController.java`
**Estimated scope:** M

### Task 3: Send test email

**Description:** Prove the settings work by sending an email to the Owner.

**Acceptance criteria:**
- [x] `spring-boot-starter-mail` added; `mailpit` service in
      `docker-compose.yml` (SMTP exposed on 2525, an allowed port; inbox 8025); `app.smtp.allow-private-hosts`
      and `app.public-url` properties.
- [x] `Mailer` builds a sender from the settings (security, timeouts 10 s),
      checks the guard first, and sends one message.
- [x] `POST /api/smtp-settings/test` emails the Owner's account email (or the
      sender email): success → CONNECTED with the date; failure → NOT_WORKING
      with the error message.

**Verification:**
- [x] `./mvnw test`
- [x] Manual: settings for Mailpit → test email in the Mailpit inbox, state
      CONNECTED; host `127.0.0.1` with the guard on, or port 22 → refused.

**Dependencies:** Task 2
**Files likely touched:** `pom.xml`, `docker-compose.yml`,
`application.properties`, `smtp/Mailer.java`, `smtp/SmtpSettingsController.java`
**Estimated scope:** M

### Task 4: Settings: Email to Readers

**Description:** The SMTP section of the Settings screen, as in the mockup.

**Acceptance criteria:**
- [x] Fields host, port, security (switches 587 ↔ 465), username, password
      (write-only: "Saved, encrypted · Replace"), sender name (Brand as
      placeholder), sender email; Save; Send test email.
- [x] State line: Not set up / Saved, send a test email / Connected, test email
      sent {date} / Not working + error.

**Verification:**
- [x] `npx tsc --noEmit`, `npm run build`; curl the SSR HTML of `/settings`
- [x] Manual: save, test against Mailpit, see Connected.

**Dependencies:** Task 3
**Files likely touched:** `frontend/src/lib/smtp.ts`,
`frontend/src/components/smtp-settings-form.tsx`,
`frontend/src/routes/_app/settings.tsx`
**Estimated scope:** M

### Checkpoint: SMTP
- [x] `./mvnw test` green, `npm run build` clean
- [x] Mailpit receives the test email; the state reads Connected; `127.0.0.1:22`
      is refused

## Phase 2: Readers subscribe

### Task 5: Subscription domain and schema

**Description:** Store Subscriptions with their lifecycle rules.

**Acceptance criteria:**
- [x] Changeset `008-subscription.sql`: `subscription` table (unique roadmap +
      email, token ascii_bin unique, cascade on Roadmap delete) and
      `roadmap.unsubscribed_count`.
- [x] `Subscription`: email trimmed, lower-cased and checked (≤ 254 chars,
      plausible format); random 128-bit URL-safe token; pending → confirmed;
      `isExpired(now)` after 7 days pending; `mayResendConfirmation(now)` once
      per 10 minutes.

**Verification:**
- [x] Unit tests first, then green: `./mvnw test -Dtest=SubscriptionTest`

**Dependencies:** Task 3
**Files likely touched:** `008-subscription.sql`, `db.changelog-master.yaml`,
`subscription/Subscription.java` (+ repository, test), `Roadmap.java`
**Estimated scope:** M

### Task 6: Subscribe and confirm API

**Description:** Readers subscribe and confirm, without revealing who is
already subscribed.

**Acceptance criteria:**
- [x] `POST /api/public/roadmaps/{publicId}/subscriptions`: 400 bad email; 404
      Private / unknown (as the Public page); 409 SMTP not Connected; otherwise
      always 202 with the same body; sends the confirmation email for new or
      pending (throttled).
- [x] `EmailContent.confirmation`: subject "Confirm your email for {Roadmap}",
      HTML-escaped, link to `{public-url}/r/{publicId}/confirm?token=…`.
- [x] `POST /api/public/subscriptions/confirm` with `{ token }`: confirms; an
      unknown or expired token → 404.

**Verification:**
- [x] Unit tests first for `EmailContent.confirmation`; `./mvnw test`
- [x] Manual (Mailpit): subscribe → email; subscribe again within 10 min → 202,
      no second email; confirm → CONFIRMED.

**Dependencies:** Task 5
**Files likely touched:** `subscription/SubscriptionService.java`,
`subscription/PublicSubscriptionController.java`, `subscription/EmailContent.java`
(+ test)
**Estimated scope:** M

### Task 7: Subscribe box and confirm page

**Description:** Readers see the subscribe box when emails can be sent, and a
confirm page.

**Acceptance criteria:**
- [x] Public payload gains `subscriptionsOpen` (Published and SMTP Connected);
      exact-fields guard updated; SPEC payload line updated.
- [x] Public page: subscribe box (email, Subscribe, the hint naming the sender
      and that {Brand} receives the address, "Check your inbox" after) when
      open; "Follow what ships" (RSS) otherwise; RSS link in both.
- [x] `/r/$publicId/confirm?token=` confirms and shows "You're subscribed", or
      "This link has expired".

**Verification:**
- [x] `./mvnw test`; `npm run build`; curl the SSR HTML in both states
- [x] Manual: subscribe from the page, confirm from Mailpit.

**Dependencies:** Task 6
**Files likely touched:** `PublicPage.java` (+ test), `frontend/src/lib/public-page.ts`,
`frontend/src/components/recently-shipped.tsx`,
`frontend/src/routes/r.$publicId.confirm.tsx`
**Estimated scope:** M

### Checkpoint: Subscribe
- [x] A Reader subscribes and confirms via Mailpit; identical answers for new
      and confirmed emails

## Phase 3: Emails, unsubscribe, Owner view

### Task 8: Shipped emails

**Description:** Email subscribed Readers when an Item becomes Shipped.

**Acceptance criteria:**
- [x] `ShippedEmails.recipients(...)`: confirmed Readers only, and nobody for an
      Internal Item, a Private Roadmap, an Item no longer Shipped, or SMTP not
      Connected.
- [x] `EmailContent.shipped`: subject "Shipped: {title}", Brand, title, Category,
      description, "See the Roadmap", footer with unsubscribe link; HTML +
      text; `List-Unsubscribe` + `List-Unsubscribe-Post` headers.
- [x] `ItemService` publishes `ItemShipped` on the transition; an after-commit
      async listener sends one email per recipient; a failure drops it and
      sets NOT_WORKING.

**Verification:**
- [x] Unit tests first: `./mvnw test -Dtest='ShippedEmailsTest,EmailContentTest'`
- [x] Manual (Mailpit): ship a visible Item → one email per subscribed Reader;
      ship an Internal Item / on a Private Roadmap → none.

**Dependencies:** Task 7
**Files likely touched:** `subscription/ShippedEmails.java` (+ test),
`subscription/EmailContent.java` (+ test), `subscription/ShippedEmailListener.java`,
`item/ItemService.java`, `RoadmapApplication.java` (`@EnableAsync`)
**Estimated scope:** M

### Task 9: Unsubscribe

**Description:** One click from any email ends the Subscription.

**Acceptance criteria:**
- [x] `POST /api/public/subscriptions/unsubscribe` (token in body or query, for
      the one-click header) deletes the Subscription and increments the
      Roadmap's unsubscribed count; unknown token → 404.
- [x] `/r/$publicId/unsubscribe?token=` posts on load and shows "You're
      unsubscribed" with "See the Roadmap".

**Verification:**
- [x] `./mvnw test`; `npm run build`
- [x] Manual: click Unsubscribe in a Mailpit email → page; ship again → no
      email for that Reader; `curl -X POST` one-click works.

**Dependencies:** Task 8
**Files likely touched:** `subscription/SubscriptionService.java`,
`subscription/PublicSubscriptionController.java`,
`frontend/src/routes/r.$publicId.unsubscribe.tsx`
**Estimated scope:** S

### Task 10: Subscribed Readers screen

**Description:** The Owner sees and manages who is subscribed to each Roadmap.

**Acceptance criteria:**
- [x] `GET /api/roadmaps/{id}/subscriptions` → counts (confirmed, pending,
      unsubscribed) + list (email, state, date), newest first;
      `DELETE /api/roadmaps/{id}/subscriptions/{subscriptionId}` (counts as
      unsubscribed); another Owner's Roadmap → 404.
- [x] `/roadmaps/$roadmapId/readers`: counts, list, Remove with confirm; the
      Share dialog says "{N} Readers are subscribed" and links to it.

**Verification:**
- [x] `./mvnw test`; `npm run build`; curl the SSR HTML of the screen
- [x] Manual: remove a Reader → list and counts update.

**Dependencies:** Task 9
**Files likely touched:** `subscription/SubscriptionController.java`,
`subscription/SubscriptionService.java`, `frontend/src/lib/subscriptions.ts`,
`frontend/src/routes/_app/roadmaps.$roadmapId.readers.tsx`,
`frontend/src/components/editor/publish-dialog.tsx`
**Estimated scope:** M

### Task 11: Hints, warnings and pending expiry

**Description:** Tell the Owner when emails will go out or are not working, and
clean up stale pending Subscriptions.

**Acceptance criteria:**
- [x] Item dialog: "{N} subscribed Readers get an email about this Item when
      you save" when saving would send it (Published, SMTP Connected, becoming
      Shipped, not Internal, N > 0).
- [x] Editor of a Roadmap with subscribed Readers: a warning when the SMTP
      state is Not working, linking to Settings.
- [x] Daily job deletes pending Subscriptions older than 7 days
      (`@EnableScheduling`).

**Verification:**
- [x] `./mvnw test`; `npm run build`
- [x] Manual: hint shows / hides as the Status and Internal toggle change;
      break the SMTP host → warning in the editor.

**Dependencies:** Task 10
**Files likely touched:** `frontend/src/components/editor/item-dialog.tsx`,
`frontend/src/routes/_app/roadmaps.$roadmapId.tsx`,
`subscription/PendingExpiry.java`, `RoadmapApplication.java`
**Estimated scope:** M

### Checkpoint: Complete (Success Criteria of SPEC-reader-updates.md)
- [x] 1. Subscribe, confirm, receive a Shipped email, unsubscribe in one click.
- [x] 2. No email for Internal Items, Private Roadmaps, pending Readers, or SMTP
      not Connected.
- [x] 3. The Owner sees confirmed / pending Readers with dates and the three
      counts, and can remove a Reader.
- [x] 4. The SMTP password is never returned and stored encrypted; private
      hosts and non-SMTP ports are refused.
- [x] 5. On SMTP failure the Owner is warned and the Public page offers RSS
      only until a test succeeds.
- [x] 6. `./mvnw test` green; `npm run build` clean.
- [ ] Review with human
