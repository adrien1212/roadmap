# Tasks: organisation

Spec: `SPEC-organisation.md`. Plan: `tasks/organisation/plan.md`. All tasks
are _Assignee: Claude_. Backend rules are written test first (plain JUnit, no
Spring context).

## Phase 1: Everything belongs to an Organisation (no visible change)

### Task 1: Organisation, Role and the expand changeset

**Description:** Add the Organisation and the Owner's Role, migrate every
existing Owner into their own Organisation with Role Owner, and add
`organisation_id` next to `owner_id` everywhere, filled in.

**Acceptance criteria:**
- [x] `010-organisation.sql`: table `organisation` (id, name VARCHAR(80),
      brand_name VARCHAR(60), created_at); `owner.organisation_id` (FK,
      nullable), `owner.role`, `owner.joined_at`, `owner.email_verified`;
      nullable `organisation_id` (FK) on `roadmap`, `category`,
      `smtp_settings`; one Organisation per existing Owner (same id, "{name}'s
      Organisation", Brand copied), Role `OWNER`; all backfilled; rollback
      drops it all.
- [x] `Organisation` entity with `rename(name)` (trimmed, required, ≤ 80);
      `Role` enum `OWNER, ADMIN, EDITOR` with `invitable()`; `Owner` gains
      `organisation`, `role`, `joinedAt`, `emailVerified`.
- [x] Owner rules: `joinAs(organisation, role, now)`, `changeRole(role)` and
      `leave()` refuse on Role Owner and refuse `OWNER` as a target
      (`ConflictException`).
- [x] `CurrentOrganisation` (throws `NotFoundException` without one);
      `OwnerProvisioningService` stores `email_verified`.

**Verification:**
- [x] Unit tests first, then green: `./mvnw test -Dtest='OrganisationTest,RoleTest,OwnerTest'`
- [x] `./mvnw test`
- [x] Manual: run the app on the dev database; every `owner` row has an
      Organisation with the same id and Role `OWNER`; every `roadmap`,
      `category` and `smtp_settings` row has `organisation_id = owner_id`.

**Dependencies:** None
**Files likely touched:** `010-organisation.sql`, `db.changelog-master.yaml`,
`organisation/Organisation.java`, `organisation/Role.java`,
`organisation/CurrentOrganisation.java`, `owner/Owner.java`,
`owner/OwnerProvisioningService.java`, tests
**Estimated scope:** M

### Task 2: Roadmaps and Items scoped by Organisation

**Description:** Roadmaps belong to an Organisation; every Roadmap and Item
query is scoped by `CurrentOrganisation`.

**Acceptance criteria:**
- [ ] `Roadmap.organisation` (mapped to `organisation_id`); `owner_id` still
      written until Task 6, as the creator.
- [ ] `RoadmapRepository`: `findAllByOrganisation…`, `findByIdAndOrganisation`,
      `countItems(organisation)`; `RoadmapService`, `ItemService` use
      `CurrentOrganisation`.
- [ ] Someone in another Organisation gets 404 on the Roadmap and its Items.

**Verification:**
- [ ] `./mvnw test -Dtest=RoadmapTest`, `./mvnw test`
- [ ] Manual: list, create, rename, publish and delete Roadmaps; add, edit,
      reorder Items, as before.

**Dependencies:** Task 1
**Files likely touched:** `Roadmap.java`, `RoadmapRepository.java`,
`RoadmapService.java`, `ItemService.java`, `RoadmapTest.java`
**Estimated scope:** M

### Task 3: Categories scoped by Organisation

**Description:** Categories belong to an Organisation; names unique per
Organisation.

**Acceptance criteria:**
- [ ] `Category.organisation`; repository methods by Organisation, including
      `existsByOrganisationAndName…` and `countItems(organisation)`;
      `CategoryService` and the Category lookup in `ItemService` use
      `CurrentOrganisation`.
- [ ] A duplicate name (ignoring case) in the same Organisation → 409, as
      today.

**Verification:**
- [ ] `./mvnw test -Dtest=CategoryTest`, `./mvnw test`
- [ ] Manual: Categories screen counts, create, rename, recolour, delete; an
      Item's Category select.

**Dependencies:** Task 2
**Files likely touched:** `Category.java`, `CategoryRepository.java`,
`CategoryService.java`, `ItemService.java`
**Estimated scope:** S

### Task 4: Brand on the Organisation

**Description:** The Brand moves from the Owner to the Organisation; Readers
see it as before.

**Acceptance criteria:**
- [ ] `Organisation.changeBrand` (rule and tests moved from `Owner`);
      `BrandService` reads and writes the current Organisation.
- [ ] `PublicPage`, `RssFeed`, `SubscriptionEmails` read
      `roadmap.getOrganisation().getBrandName()`.
- [ ] The public payload is unchanged (exact-fields test unchanged).

**Verification:**
- [ ] `./mvnw test -Dtest='OrganisationTest,PublicPageTest,RssFeedTest,ShippedEmailsTest'`
- [ ] Manual: change the Brand in Settings; the Public page, its title and the
      Feed show it.

**Dependencies:** Task 2
**Files likely touched:** `Organisation.java`, `Owner.java`, `BrandService.java`,
`PublicPage.java`, `RssFeed.java`, `SubscriptionEmails.java`, tests
**Estimated scope:** M

### Task 5: SMTP settings on the Organisation

**Description:** SMTP settings belong to the Organisation.

**Acceptance criteria:**
- [ ] `SmtpSettings.organisationId`, `SmtpSettingsRepository.findByOrganisationId`;
      `SmtpSettingsService`, `SubscriptionEmails`, `SubscriptionService` use
      it.
- [ ] Save, "Send test email" and emails to subscribed Readers work as before.

**Verification:**
- [ ] `./mvnw test -Dtest=SmtpSettingsTest`, `./mvnw test`
- [ ] Manual (Mailpit): test email; a subscribed Reader gets the Shipped email.

**Dependencies:** Task 1
**Files likely touched:** `SmtpSettings.java`, `SmtpSettingsRepository.java`,
`SmtpSettingsService.java`, `SubscriptionEmails.java`, `SubscriptionService.java`
**Estimated scope:** M

### Task 6: Contract changeset

**Description:** Drop what the Owner no longer owns.

**Acceptance criteria:**
- [ ] No repository or service scopes by Owner any more (`grep`).
- [ ] `011-organisation-contract.sql`: drop `owner_id` from `roadmap` and
      `category`; `organisation_id` `NOT NULL`; unique
      `(organisation_id, name)` on `category`; `smtp_settings` primary key
      becomes `organisation_id`; drop `owner.brand_name`. Rollback restores
      them from `organisation_id`.
- [ ] Entities no longer map the dropped columns.

**Verification:**
- [ ] `./mvnw test`
- [ ] Manual: `010` and `011` on a dump of production data; row counts per
      Organisation equal those per Owner before; the app works as before.

**Dependencies:** Tasks 3, 4, 5
**Files likely touched:** `011-organisation-contract.sql`,
`db.changelog-master.yaml`, `Roadmap.java`, `Category.java`, `SmtpSettings.java`
**Estimated scope:** S

### Checkpoint: migrated, nothing visible changed
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] An existing Owner sees exactly their Roadmaps, Categories, Brand and
      SMTP settings; Public pages, Feed and emails unchanged

## Phase 2: Create an Organisation

### Task 7: Create an Organisation (API) and `/api/me`

**Description:** Someone without an Organisation creates one and gets Role
Owner.

**Acceptance criteria:**
- [ ] `POST /api/organisation` `{ name }` → the Organisation, caller Role
      `OWNER`; already in one → 409; name rules → 400.
- [ ] `GET /api/me` adds `role` and `organisation: { name }` (null without).
- [ ] Every scoped endpoint returns 404 without an Organisation.

**Verification:**
- [ ] `./mvnw test -Dtest=OrganisationTest`
- [ ] Manual: a fresh Google account → `/api/me` has `organisation: null`,
      `/api/roadmaps` → 404; after `POST`, both work.

**Dependencies:** Task 6
**Files likely touched:** `OrganisationController.java`, `OrganisationService.java`,
`OrganisationRepository.java`, `MeController.java`
**Estimated scope:** M

### Task 8: Welcome page and guard

**Description:** After sign-in, someone without an Organisation lands on the
Welcome page and creates one, or pastes an invitation link.

**Acceptance criteria:**
- [ ] `/welcome` from `design/Onboarding.dc.html`, restyled to the
      `design/ui-design.html` tokens, with the spec's copy (no "workspace",
      "team", "teammate").
- [ ] `_app` redirects to `/welcome` when `organisation` is null; `/welcome`
      redirects to `/roadmaps` when it isn't, and to `/` when signed out.
- [ ] Create: name field, inline errors, lands on `/roadmaps`. Join: accepts a
      full link or a secret and opens `/join/{secret}`; anything else shows
      "This link isn't an invitation link."

**Verification:**
- [ ] `npx tsc --noEmit`, `npm run build`
- [ ] Manual: fresh account → Welcome → create → Roadmaps; reload `/welcome`
      → Roadmaps.

**Dependencies:** Task 7
**Files likely touched:** `frontend/src/lib/me.ts`,
`frontend/src/lib/organisation.ts`, `frontend/src/routes/_app.tsx`,
`frontend/src/routes/welcome.tsx`
**Estimated scope:** M

### Checkpoint: a new Owner creates an Organisation
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] A fresh account goes Welcome → create → Roadmaps; existing Owners never
      see Welcome

## Phase 3: Invitations

### Task 9: Invitations (domain and API)

**Description:** Create, list, replace and revoke Invitations, and email them.

**Acceptance criteria:**
- [ ] `012-invitation.sql` and `Invitation` (email, role, invitedBy,
      secretHash, createdAt, expiresAt = +7 days, usedAt, revokedAt) with
      `isPending(now)`, `revoke()`; tests first.
- [ ] `POST /api/invitations` `{ email, role }` → `{ id, url, expiresAt,
      emailed }`: `owner` → 400; already in the Organisation → 409; earlier
      pending Invitation for that email revoked; only the hash stored.
- [ ] `DELETE /api/invitations/{id}` → 204; another Organisation's → 404.
- [ ] Email (subject "{inviter} invited you to {Organisation name}", Role,
      link, expiry) through the Organisation's working SMTP settings;
      `emailed: false` otherwise.

**Verification:**
- [ ] `./mvnw test -Dtest='InvitationTest,InvitationEmailTest'`
- [ ] Manual (Mailpit): invite → email with the link; invite again → first
      link replaced; without SMTP → `emailed: false`.

**Dependencies:** Task 7
**Files likely touched:** `012-invitation.sql`, `db.changelog-master.yaml`,
`Invitation.java`, `InvitationRepository.java`, `InvitationService.java`,
`InvitationController.java`, `InvitationEmail.java`, tests
**Estimated scope:** L (the email may split off if it grows)

### Task 10: Invite popup

**Description:** The invite popup from the spec, opened from Settings.

**Acceptance criteria:**
- [ ] Email, Role (Editor default, Admin), the "expires in 7 days and works
      only for {email}" note; inline API errors.
- [ ] After Invite: "Sent to {email}" or "Email isn't set up. Copy the link
      and send it.", the link with Copy, "This link is shown only once."
- [ ] A temporary "Invite" button in Settings opens it (Task 14 places it in
      People).

**Verification:**
- [ ] `npx tsc --noEmit`, `npm run build`
- [ ] Manual: both outcomes, with and without working SMTP settings.

**Dependencies:** Task 9
**Files likely touched:** `frontend/src/components/invite-dialog.tsx`,
`frontend/src/lib/organisation.ts`, `frontend/src/routes/_app/settings.tsx`
**Estimated scope:** S

### Task 11: Accept an Invitation (API and sign-in return)

**Description:** The invited person, and only them, accepts once.

**Acceptance criteria:**
- [ ] `GET /api/invitations/{secret}` (anonymous) → `{ inviterName,
      organisationName, role, maskedEmail, expiresAt }`; 404 for unknown,
      used, revoked, replaced, expired.
- [ ] `POST /api/invitations/{secret}/accept` → 204: email mismatch or not
      verified → 403, nothing used up; Organisation with Roadmaps → 409;
      Organisation without Roadmaps → moved (old one deleted when empty;
      Role Owner only when alone); single use under concurrency.
- [ ] Sign-in with `continue=/join/{secret}` returns there; any other value
      falls back to `/roadmaps`.

**Verification:**
- [ ] `./mvnw test -Dtest='InvitationTest,OwnerTest'`
- [ ] Manual with two Google accounts: wrong account → 403; right one → joins
      with the Role; second use → 404; `continue=https://evil.example` →
      `/roadmaps`.

**Dependencies:** Task 9
**Files likely touched:** `InvitationService.java`, `InvitationController.java`,
`OAuth2ClientSecurityConfig.java`, `JoinReturn.java` (resolver + success
handler), `OrganisationRepository.java`
**Estimated scope:** M

### Task 12: Join page

**Description:** `/join/{secret}` with a single Accept button.

**Acceptance criteria:**
- [ ] Inviter, Organisation name, Role, masked email and expiry, as in the
      spec's sketch; "This invitation is no longer valid" on 404.
- [ ] Signed out: Accept → Google → back with `?accept=1` → accepts. Signed
      in: Accept accepts. Success → `/roadmaps`; 403 and 409 show the spec's
      messages.

**Verification:**
- [ ] `npx tsc --noEmit`, `npm run build`
- [ ] Manual: signed out and signed in; wrong account; already in an
      Organisation with Roadmaps; from the Welcome page's Join card.

**Dependencies:** Tasks 8, 11
**Files likely touched:** `frontend/src/routes/join.$secret.tsx`,
`frontend/src/lib/organisation.ts`
**Estimated scope:** S

### Checkpoint: invite and join end to end
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] A invites B (Admin) and C (Editor); both join and see the same Roadmaps
      and Categories; links fail for another account, a second time, after
      revoke and replacement

## Phase 4: Settings

### Task 13: Organisation and People (API)

**Description:** Rename the Organisation; list people and Invitations; change
Roles; remove and leave.

**Acceptance criteria:**
- [ ] `GET /api/organisation` → `{ name, people, invitations }` (pending only);
      `PUT /api/organisation` `{ name }`.
- [ ] `PUT /api/organisation/people/{id}/role` → 409 on Role Owner or to
      `owner`; `DELETE /api/organisation/people/{id}` (oneself = Leave) → 409
      on Role Owner; the person is left without an Organisation.
- [ ] Another Organisation's person or Invitation → 404.

**Verification:**
- [ ] `./mvnw test -Dtest=OwnerTest`, `./mvnw test`
- [ ] Manual: rename; change B's Role; remove C, who lands on Welcome at next
      load; the Owner cannot be removed, leave or change Role.

**Dependencies:** Task 11
**Files likely touched:** `OrganisationController.java`,
`OrganisationService.java`, `OwnerRepository.java`
**Estimated scope:** M

### Task 14: Organisation and People sections

**Description:** The Organisation and People sections of Settings.

**Acceptance criteria:**
- [ ] Organisation: name field, Save, "Settings saved" toast.
- [ ] People: people (name, email, Role, joined), Role select and Remove (not
      on the Role Owner), Leave for oneself (not for the Role Owner) with a
      confirmation; pending Invitations (email, Role, invited by, expires)
      with Revoke; "Invite" opens the popup (temporary button removed).
- [ ] After Leave: `/welcome`.

**Verification:**
- [ ] `npx tsc --noEmit`, `npm run build`
- [ ] Manual: the Phase 4 flows with three accounts.

**Dependencies:** Tasks 10, 13
**Files likely touched:** `frontend/src/routes/_app/settings.tsx`,
`frontend/src/components/people-section.tsx`,
`frontend/src/lib/organisation.ts`
**Estimated scope:** M

### Checkpoint: Complete
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] All Success Criteria of `SPEC-organisation.md` checked manually
- [ ] No person, Organisation or Role in the public payload, the Feed or
      Reader emails
