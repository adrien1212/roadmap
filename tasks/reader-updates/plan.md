# Implementation Plan: reader-updates

## Overview

Build module `reader-updates` from `SPEC-reader-updates.md` (approved): the
Owner sets their SMTP settings; Readers subscribe by email (double opt-in) on a
Public page; subscribed Readers get one email per Item that becomes Shipped,
with one-click unsubscribe; the Owner sees and manages subscribed Readers per
Roadmap. Builds on `changelog` (Shipped date) and `branding` (Brand).
Independent of the MVP's open auth phases.

Tasks live in `tasks/reader-updates/todo.md`. All are _Assignee: Claude_.

## Architecture Decisions

- **Two new packages**: `smtp` (SMTP settings, encryption, server guard,
  sending) and `subscription` (Subscriptions, emails to Readers, public and
  Owner endpoints).
- **Privacy- and security-critical logic is pure and unit-tested first**:
  `SecretBox` (AES-256-GCM, JDK only), `SmtpServerGuard` (allowed ports, public
  addresses only), `Subscription` (email normalisation, token, expiry),
  `ShippedEmails.recipients` (who gets emailed), `EmailContent` (subjects,
  HTML-escaped bodies, unsubscribe link and headers).
- **The guard resolves the host right before every connection** (test and each
  send) and refuses any loopback, private, link-local, unique-local, CGNAT,
  multicast or wildcard address. Allowed ports: 25, 465, 587, 2525.
  `app.smtp.allow-private-hosts` (default `false`) is set to `true` only for
  local dev so Mailpit on `localhost:1025` works.
- **One `JavaMailSender` built per send** from the Owner's settings (no global
  mail config), with 10 s connect / read timeouts.
- **Shipped emails are event-driven**: `ItemService` publishes `ItemShipped`
  only when an Item's Status becomes Shipped; an
  `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` listener reloads the
  Item and applies `recipients(...)` to its final state (Internal? Published?
  still Shipped? SMTP Connected?), then sends one email per subscribed Reader.
  A failure drops the email and sets the SMTP state to Not working.
- **Absolute links in emails come from `app.public-url`** (default
  `http://localhost:3000`), because background sends have no request to read
  the origin from.
- **The Public page learns whether to show the subscribe box from the public
  payload**: a new `subscriptionsOpen` boolean (Published and SMTP Connected).
  It says nothing else about the Owner's mail setup. This is the one payload
  addition, and the exact-fields guard is updated for it.
- **Unsubscribe deletes the row** and increments
  `roadmap.unsubscribed_count`; Owner removal does the same.
- **Pending expiry** runs daily (`@Scheduled`), deleting pending
  Subscriptions older than 7 days.
- **Schema**: changesets `007-smtp-settings.sql` and `008-subscription.sql`
  (with `roadmap.unsubscribed_count`).
- **New dependency** `spring-boot-starter-mail` and a `mailpit` service in
  `docker-compose.yml` (approved with the spec).

## Dependency graph

```
T1 SecretBox + SmtpServerGuard (pure)
 └─ T2 SMTP settings API (store, encrypt)
     └─ T3 Send test email (mail starter, Mailpit, sender)
         ├─ T4 Settings: Email to Readers (frontend)
         └─ T5 Subscription domain + schema
             └─ T6 Subscribe + confirm API, confirmation email
                 └─ T7 Subscribe box + confirm page (frontend, subscriptionsOpen)
                     └─ T8 Shipped emails (event, recipients, content)
                         └─ T9 Unsubscribe (API + page + headers)
                             └─ T10 Subscribed Readers (Owner API + screen + Share dialog)
                                 └─ T11 Hints, warnings and pending expiry
```

## Task List

### Phase 1: The Owner's SMTP server
- [x] Task 1: Encryption and SMTP server guard
- [ ] Task 2: SMTP settings API
- [ ] Task 3: Send test email
- [ ] Task 4: Settings: Email to Readers

### Checkpoint: SMTP
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] An Owner saves SMTP settings for Mailpit, sends a test email that arrives
      in Mailpit, and the state reads Connected; `127.0.0.1:22` is refused

### Phase 2: Readers subscribe
- [ ] Task 5: Subscription domain and schema
- [ ] Task 6: Subscribe and confirm API
- [ ] Task 7: Subscribe box and confirm page

### Checkpoint: Subscribe
- [ ] A Reader subscribes on a Public page, confirms from Mailpit; the answer
      is identical for new and already confirmed emails

### Phase 3: Emails, unsubscribe, Owner view
- [ ] Task 8: Shipped emails
- [ ] Task 9: Unsubscribe
- [ ] Task 10: Subscribed Readers screen
- [ ] Task 11: Hints, warnings and pending expiry

### Checkpoint: Complete (Success Criteria of SPEC-reader-updates.md)
- [ ] Criteria 1–6 verified with Mailpit
- [ ] Review with human

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Owner-chosen SMTP host used to reach internal machines (SSRF) | High | `SmtpServerGuard` on every connection, ports allow-list, timeouts; unit tests for each refused range; manual `127.0.0.1` / port 22 checks |
| DNS rebinding between the guard's lookup and the mail library's | Med | Resolve immediately before connecting; residual risk accepted and noted; revisit by connecting to the checked IP |
| Email sent about an Internal Item or a Private Roadmap, or to a pending Reader | High | Pure `recipients` rule evaluated on the Item's final state after commit; unit tests for each case; manual checks |
| Subscribe form abused to send confirmation emails to strangers from the Owner's server | Med | One confirmation per address and Roadmap per 10 min; identical answers; per-IP limits left for later |
| SMTP password exposed | High | AES-GCM with `SMTP_SECRET_KEY`; write-only API field; no logging of settings |
| Missing `SMTP_SECRET_KEY` breaks the app | Low | Only saving SMTP settings is refused, with a clear message; documented in CLAUDE.md |
| Links in emails point at the wrong origin | Med | `app.public-url`; checked in Mailpit |
| A background send failure goes unnoticed | Med | State Not working + warnings in Settings and editor; dropped emails accepted by the spec |

## Open Questions

- The plan adds `subscriptionsOpen` to the public payload so the Public page
  knows whether to show the subscribe box. OK? (SPEC's payload line is updated
  with Task 7.)
