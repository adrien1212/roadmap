# Implementation Plan: organisation

## Overview

Build module `organisation` from `SPEC-organisation.md` (approved
2026-10-18): Roadmaps, Categories, the Brand and SMTP settings move from the
Owner to an Organisation; Owners get a Role (Owner, Admin, Editor), recorded
but not enforced; a new Owner creates or joins an Organisation from the
Welcome page; Invitations are single-use links for one email address, sent
through the Organisation's SMTP settings.

Tasks live in `tasks/organisation/todo.md`. All are _Assignee: Claude_.

## Architecture Decisions

- **Expand, then contract.** `010-organisation.sql` adds the Organisation and
  the new `organisation_id` columns next to the old `owner_id` ones and fills
  them; the code moves over task by task; `011-organisation-contract.sql`
  then drops `owner_id` and `owner.brand_name` and sets `NOT NULL`. The app
  works after every task, and no applied changeset is edited.
- **Migrated Organisations reuse the Owner's id** (`INSERT INTO organisation
  (id, …) SELECT id, … FROM owner`), so backfilling is
  `organisation_id = owner_id` everywhere. MariaDB moves `AUTO_INCREMENT` past
  the highest inserted id. Name: "{name}'s Organisation", or "My
  Organisation" without a Google name, cut to 80 characters.
- **The person stays `Owner`** (class, table, `CurrentOwner`). It gains
  `organisation` (nullable `@ManyToOne`), `role` (`Role` enum, null without an
  Organisation) and `joinedAt`.
- **One scoping seam: `CurrentOrganisation`.** `get()` returns the current
  Owner's Organisation, or throws `NotFoundException` without one, so every
  scoped endpoint answers 404 to someone with no Organisation. Repositories
  keep "no bare findById": `findByIdAndOrganisation`, and so on.
- **Rules on the entities, tested without Spring**: `Owner.joinAs`,
  `Owner.changeRole`, `Owner.leave` (Role Owner refused), `Role.invitable()`,
  `Organisation.rename`, `Organisation.changeBrand` (moved from `Owner`),
  `Invitation.accept` / `revoke` / `isPending`.
- **Invitation secret**: 32 random bytes, base64url, in the link only; the
  table stores its SHA-256 (a high-entropy secret needs no slow hash). Lookup
  is by hash, so every invalid case is the same 404.
- **One pending Invitation per email per Organisation**: creating one revokes
  the earlier pending one in the same transaction.
- **Single use under concurrency**: `accept` runs an
  `UPDATE … SET used_at = :now WHERE id = :id AND used_at IS NULL AND
  revoked_at IS NULL`, and treats 0 rows as 404.
- **Email check**: provisioning also stores the OIDC `email_verified` claim on
  the Owner (`owner.email_verified`); accept requires a verified email equal,
  ignoring case, to the Invitation's.
- **Sign-in that returns to the join page**: Accept, signed out, goes to
  `/oauth2/authorization/google?continue=/join/{secret}`. A custom
  `OAuth2AuthorizationRequestResolver` keeps `continue` in the session when it
  matches `^/join/[A-Za-z0-9_-]+$` (nothing else, so no open redirect); the
  success handler redirects there, otherwise to `/roadmaps`. The join page,
  opened signed in with `?accept=1`, accepts at once.
- **Anonymous invitation lookup**: `GET /api/invitations/{secret}` is
  `permitAll` for `GET` only; it is not under `/api/public/**`, which is for
  Readers.
- **Invitation email**: sent with the existing `Mailer` when the
  Organisation's SMTP settings are working; any failure gives `emailed:
  false`, never an error, since the link is in the response.
- **Frontend guard**: `_app` `beforeLoad` already loads `/api/me`; with
  `organisation: null` it redirects to `/welcome`. `/welcome` and
  `/join/$secret` sit outside `_app`; `/welcome` requires sign-in and sends
  anyone with an Organisation to `/roadmaps`.

## Task List

### Phase 1: Everything belongs to an Organisation (no visible change)
- [x] Task 1: Organisation, Role and the expand changeset
- [x] Task 2: Roadmaps and Items scoped by Organisation
- [x] Task 3: Categories scoped by Organisation
- [x] Task 4: Brand on the Organisation
- [x] Task 5: SMTP settings on the Organisation
- [x] Task 6: Contract changeset

### Checkpoint: migrated, nothing visible changed

### Phase 2: Create an Organisation
- [x] Task 7: Create an Organisation (API) and `/api/me`
- [x] Task 8: Welcome page and guard

### Checkpoint: a new Owner creates an Organisation

### Phase 3: Invitations
- [x] Task 9: Invitations (domain and API)
- [x] Task 10: Invite popup
- [x] Task 11: Accept an Invitation (API and sign-in return)
- [x] Task 12: Join page

### Checkpoint: invite and join end to end

### Phase 4: Settings
- [x] Task 13: Organisation and People (API)
- [x] Task 14: Organisation and People sections

### Checkpoint: Complete (all Success Criteria of SPEC-organisation.md)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Migration loses or mixes data | High | Expand/contract; id reuse makes backfill a copy; run `010` and `011` on a dump of the production database first, and compare row counts per Owner before and after |
| A query left scoped by Owner after Task 6 | High | Task 6 drops the `owner_id` columns: anything left fails loudly; `grep` for `Owner owner` in repositories before it |
| Open redirect via `continue` | Med | Only `/join/{secret}` is kept; anything else is ignored |
| Invitation accepted by someone else | High | Verified email must match; secret 256 bits, hashed; same 404 for every invalid case |
| An Invitation used twice at once | Low | Conditional `UPDATE` |
| Roles not enforced: an Editor can remove people or change Roles | Med | Accepted in the spec; `roles` module next; the Owner rules hold anyway |

## Open Questions

None. Decisions are recorded in `SPEC-organisation.md`.
