# Spec: organisation

> Module `organisation` (split out of `teams-billing` in `ROADMAP.md`; billing
> stays later). Status: **approved 2026-10-18.** `SPEC.md` stays the source of
> truth for everything not changed here; the changes listed under "Changes to
> SPEC.md" have been applied to it, and to `ROADMAP.md`.

## Objective

Today one Owner owns everything: Roadmaps, Categories, Brand, SMTP settings.
A software publisher has a product team, not one person, and the next modules
(`embed`, per-Item subscriptions) are aimed at publishers.

This module adds the **Organisation**: the thing that owns Roadmaps,
Categories, Brand and SMTP settings. An Organisation has several people from
the publisher, each with a **Role** (Owner, Admin or Editor). Every one of them
sees and edits every Roadmap of the Organisation. Someone joins through an
**Invitation** sent to their email address; only that person can accept it.

**Roles are recorded, not enforced, in this module.** They are chosen at
invitation, stored and shown; permission checks (Spring Security) come in a
later module. Until then everyone in an Organisation can do everything,
except the few Owner rules below, which are domain rules.

Readers, the publisher's clients, are never part of an Organisation and have
no Role. They keep reading through the Public page only; Public pages, the
Feed and Reader emails are unchanged.

## Assumptions

1. **A person belongs to at most one Organisation.** No switching between
   Organisations. A person with none is sent to the Welcome page.
2. **First sign-in creates the person only, with no Organisation.** They then
   create one (and become its Owner) or join one from the Welcome page.
3. **No per-Roadmap access.** A Role applies to all Roadmaps of the
   Organisation.
4. **Invitation emails go through the Organisation's SMTP settings** (decided).
   Until they work, the link is copied from the popup and sent by hand.
5. **Existing data migrates one-to-one**: each existing Owner gets their own
   Organisation, named "{name}'s Organisation", with Role Owner, which takes
   over their Roadmaps, Categories, Brand and SMTP settings. No data is
   merged, and they never see the Welcome page.
6. **No real-time collaboration.** Two people editing at once: last write
   wins, as two browser tabs do today.

## Language (additions and changes)

**Organisation** (new):
The publisher's people who maintain the same Roadmaps. Owns the Roadmaps, the
Categories, the Brand and the SMTP settings. Has a name, set in Settings, seen
only by its people. Never shown to Readers.
_Avoid_: workspace, team, account, company, tenant, group

**Role** (new):
What a person is in their Organisation. Recorded in this module, enforced in
a later one.
- **Owner**: the person who created the Organisation. Exactly one per
  Organisation; cannot be removed, leave, or change Role.
- **Admin**: the same rights as the Owner.
- **Editor**: adds and edits Items.

Readers have no Role.
_Avoid_: permission, access level, rights

**Invitation** (new):
A single-use link created for one email address, with a Role (Admin or
Editor). Only the person who signs in with Google under that email can accept
it. Pending until accepted, revoked, replaced or expired (7 days after
creation, fixed).
_Avoid_: invite code, token (in the UI), membership, access request

**Owner** (changed): a person signed in with Google. Belongs to at most one
Organisation, with one Role there; does everything to what their Organisation
owns and nothing to anyone else's. The person keeps this name in code
(`Owner`, `owner` table, `CurrentOwner`); the Role is always written "Role
Owner" or `OWNER` to keep the two apart.

**Category**, **Brand**, **SMTP settings** (changed): "belong to the Owner" /
"set once per Owner" become "belong to the Organisation" / "set once per
Organisation". SMTP settings are also used to send Invitations. Brand's
_Avoid_ list drops "organisation" and keeps the rest.

## Requirements

### Ownership and scoping
- Roadmaps, Categories, the Brand and SMTP settings belong to an Organisation.
- Every authenticated query is scoped by the current person's Organisation.
  Any access to another Organisation's resource returns 404, as today.
- Category names stay unique per Organisation, compared case-insensitively.
- The Categories screen counts Items across all Roadmaps of the Organisation.

### Welcome page (`/welcome`)

Layout from `design/Onboarding.dc.html`, restyled to the tokens of
`design/ui-design.html` (the mockup uses another design system's tokens).

- Shown after sign-in to anyone without an Organisation; every editor route
  sends them there, and `/welcome` sends anyone with an Organisation to their
  Roadmaps.
- Title "Welcome to Product name", subtitle "Create an organisation, or join
  the one you were invited to."
- **Create a new organisation**: "Set up your organisation; invite people
  later." Organisation name (trimmed, required, ≤ 80 characters) and "Create
  organisation". The person becomes its Owner and lands on the Roadmaps
  screen.
- **Join an organisation**: "Paste the invitation link you received." A field
  that accepts the full link (or its secret) and "Join organisation", which
  opens the join page for it. An unusable value shows "This link isn't an
  invitation link."
- The mockup's "workspace", "team" and "teammate" are replaced by Language
  terms.

### Owner rules (domain rules, enforced now)
- An Organisation has exactly one Owner, set at creation.
- The Owner cannot be removed, cannot leave, and their Role cannot be changed.
  Transferring ownership is out of scope.
- An Invitation can carry Admin or Editor, never Owner.

### Settings
- **Organisation** section: its name (trimmed, required, ≤ 80 characters).
- **People** section: everyone in the Organisation (name, email, Role, date
  joined) and the pending Invitations (email, Role, invited by, expires on),
  with "Invite", a Role select per person (Admin / Editor; not on the Owner),
  "Remove" (not on the Owner), "Revoke" per Invitation, and "Leave" for
  oneself (not for the Owner), with a confirmation.
- A removed (or leaving) person no longer has an Organisation and goes to the
  Welcome page next time. What they created stays with the Organisation.
- Deleting an Organisation is out of scope for now; closing one is not (below).

### Closing the Organisation (added 2026-10-19)

- **Close organisation**, at the bottom of Settings, shown to the Role Owner
  only. This is an Owner rule, enforced now: anyone else gets 403.
- A confirmation popup asks to type the Organisation name; a different name →
  400.
- Closing, in one transaction:
  - every Roadmap of the Organisation becomes Private (`publishedAt` null):
    its Public page and Feed return 404, so no Reader email is sent;
  - its pending Invitations are revoked;
  - everyone in it, the Role Owner included, leaves it and goes to the
    Welcome page at their next page load;
  - the Organisation is marked closed (`closedAt`, `closedBy`).
- **Nothing is deleted**: Roadmaps, Items, Categories, Brand, SMTP settings and
  Subscriptions are kept, so the Organisation can be restored later (not in
  this module).
- `POST /api/organisation/close` `{ name }` → 204.

### Invite popup

```
┌ Invite to Acme product team ─────────────────────── ✕ ┐
│ Email                                                  │
│ [ jane@acme.com                                     ]  │
│ Role                                                   │
│ (•) Editor   Adds and edits Items.                     │
│ ( ) Admin    Same rights as the Owner.                 │
│                                                        │
│ The link expires in 7 days and works only for          │
│ jane@acme.com.                                         │
│                                   [Cancel] [Invite]    │
└────────────────────────────────────────────────────────┘
        after Invite ↓
┌ Invitation sent ────────────────────────────────── ✕ ┐
│ ✓ Sent to jane@acme.com.                              │
│   (or, without working SMTP settings:                 │
│    "Email isn't set up. Copy the link and send it.")  │
│ [ https://…/join/Xk3…                    ] [Copy]     │
│ This link is shown only once.               [Done]    │
└───────────────────────────────────────────────────────┘
```

- Email: trimmed, validated like Reader emails, compared case-insensitively.
  Role defaults to Editor.
- One Invitation per email address: inviting an address that already has a
  pending Invitation replaces it (the old link stops working). Inviting
  someone already in this Organisation → "jane@acme.com is already in this
  Organisation".
- Only a hash of the link's secret is stored; a lost link is revoked and a new
  Invitation created.
- The email: subject "{inviter} invited you to {Organisation name}", the
  Role, the link, the expiry date. Sent from the Organisation's SMTP settings.

### Join page (`/join/{secret}`)

```
┌──────────────────────────────────────────────┐
│                 [A]                          │
│   Ada Lovelace invited you to join           │
│   Acme product team as Editor.               │
│                                              │
│               [ Accept ]                     │
│   For j***@acme.com · expires 25 Oct 2026    │
└──────────────────────────────────────────────┘
```

- Anonymous allowed. One button, "Accept".
- Not signed in: Accept goes through Google sign-in, comes back to the page
  and accepts.
- Accepting checks that the Google account's verified email equals the
  Invitation's email. If not: "This invitation is for j***@acme.com. Sign in
  with that Google account." Nothing is used up.
- Someone without an Organisation (new, or arriving from the Welcome page)
  joins it with the Invitation's Role, without ever seeing the Welcome page.
  Someone whose Organisation has **no Roadmap** moves into it (their old
  Organisation is deleted if nobody else is in it; with Role Owner, they can
  only move when nobody else is in it). Someone whose Organisation has
  Roadmaps is refused: "You already belong to {name}. Leave it, or ask to be
  invited with another Google account."
- On success the Invitation is used and the person lands on the Roadmaps
  screen.
- Unknown, used, revoked, replaced or expired: the same "This invitation is no
  longer valid" page.

### API
- `GET /api/me` adds `role` and `organisation: { name }`, both null without
  an Organisation.
- `POST /api/organisation` `{ name }` → the new Organisation, the caller as
  Role Owner; already in one → 409.
- Every Organisation-scoped endpoint returns 404 to someone without an
  Organisation.
- `GET /api/organisation` → `{ name, people: [{ id, name, email, role,
  joinedAt }], invitations: [{ id, email, role, invitedBy, expiresAt }] }`.
- `PUT /api/organisation` `{ name }`.
- `POST /api/invitations` `{ email, role }` → `{ id, url, expiresAt, emailed }`;
  the only response that ever contains the link. Role `owner` → 400.
- `DELETE /api/invitations/{id}` → 204.
- `PUT /api/organisation/people/{id}/role` `{ role }`; on the Owner, or to
  `owner` → 409.
- `DELETE /api/organisation/people/{id}` (oneself = Leave) → 204; the Owner →
  409.
- `GET /api/invitations/{secret}` (anonymous) → `{ inviterName,
  organisationName, role, maskedEmail, expiresAt }`, or 404 for every invalid
  case.
- `POST /api/invitations/{secret}/accept` (signed in) → 204; email mismatch →
  403; Organisation with Roadmaps → 409.
- Role in JSON: `owner`, `admin`, `editor`; enum `OWNER`, `ADMIN`, `EDITOR`.
- No endpoint checks the Role in this module (see Objective).

### Public page, Feed, Reader emails
- Unchanged. They read the Brand from the Roadmap's Organisation. No person,
  Organisation name or Role is ever sent to a Reader.

## Changes to SPEC.md (applied)

- Out of scope: "Workspaces, multiple people per account, invitations, roles"
  becomes "Several Organisations per person, per-Roadmap access, enforcing
  Roles, transferring ownership, deleting an Organisation, billing".
- Language: add Organisation, Role, Invitation; update Owner, Category,
  Brand, SMTP settings as above.
- Functional requirements / Owner: "first sign-in creates the Owner" gains
  "then the Welcome page: create or join an Organisation"; "Sees only their
  own Roadmaps and Categories" becomes "…their Organisation's Roadmaps and
  Categories".
- Settings: add the Organisation and People sections.
- Boundaries / Always: "scope every authenticated query by the current
  person's Organisation".
- Success criterion 7: "Someone in Organisation A gets 404 on any of
  Organisation B's Roadmaps, Items or Categories."
- `ROADMAP.md`: `teams-billing` becomes `organisation` (Now, before `embed`),
  `roles` (Next: enforce Roles with Spring Security) and `billing` (Later).

## Tech stack, commands, project structure

Unchanged from `SPEC.md`. No new dependency.

```
backend/src/main/resources/db/changelog/changes/010-organisation.sql
    → organisation (name); owner.organisation_id + owner.role; one Organisation per
      existing Owner (role OWNER); roadmap/category/smtp_settings: owner_id → organisation_id;
      brand_name moves to organisation
backend/src/main/resources/db/changelog/changes/011-invitation.sql
    → invitation (organisation_id, email, role, invited_by, secret_hash, created_at,
      expires_at, used_at, revoked_at)
backend/.../organisation/  Organisation, Role, Invitation, + repositories, services, controllers
backend/.../owner/OwnerProvisioningService.java → new person created after accepting
backend/.../roadmap, category, item, smtp, subscription, owner/BrandService
    → scope by Organisation
backend/.../publicpage/PublicPage.java, RssFeed.java → Brand from the Organisation
frontend/src/routes/welcome.tsx              → Welcome page (create or join)
frontend/src/routes/_app/settings.tsx        → Organisation and People sections
frontend/src/components/invite-dialog.tsx    → invite popup
frontend/src/routes/join.$secret.tsx         → join page
frontend/src/lib/organisation.ts             → queries and mutations
```

## Code style

As in `SPEC.md`. Rules live on the entities and are unit-tested without Spring:

```java
public enum Role {
    OWNER, ADMIN, EDITOR;

    /** An Invitation can carry any Role but OWNER. */
    public boolean invitable() { return this != OWNER; }
}
```

## Testing strategy

Backend unit tests (JUnit, no Spring context, written first):
- Owner rules: the Owner cannot be removed, leave, or change Role; no Role can
  be changed to Owner; an Invitation with Owner is rejected.
- Invitation: expires after 7 days; single use; revoked or replaced →
  unusable; secret checked against its hash; email matched
  case-insensitively.
- Joining is refused when the person's Organisation has Roadmaps.
- Public mapping: still no person or Organisation data; Brand read from the
  Organisation.

Manual, against the running app, with three Google accounts:
- A signs in for the first time, lands on the Welcome page, creates
  "Acme product team" and becomes its Owner.
- A invites B as Admin and C as Editor; each accepts; all three see and edit
  the same Roadmaps and Categories; People shows the three Roles.
- C opens B's link: refused. A link fails after use, revoke, replacement and
  expiry.
- Someone in another Organisation gets 404 on A's Roadmaps, Items and
  Categories.
- After migration, every existing Owner still sees exactly their Roadmaps,
  Categories, Brand and SMTP settings, with Role Owner.

## Boundaries

- Always: scope by the Organisation; store only a hash of the Invitation
  secret; the same 404 for every invalid Invitation.
- Ask first: enforcing Roles (its own module); per-Roadmap access; editing an
  applied changeset (add new ones).
- Never: show people, the Organisation or Roles to Readers; let an Invitation
  be used twice or by another email; let an Organisation lose its Owner.

## Success criteria

1. Someone in an Organisation invites an email with a Role; that person
   receives the link (or gets it by hand), accepts with the matching Google
   account, and sees the Organisation's Roadmaps.
2. The link fails for another Google account, a second time, after revoke or
   replacement, and after 7 days.
3. Roles are stored and shown in People; the Owner cannot be removed, leave
   or change Role.
4. After first sign-in, the Welcome page lets the person create an
   Organisation (they become its Owner) or join one with a pasted link; the
   Organisation can be renamed in Settings.
5. Existing data migrates with no visible change for current Owners.
6. Another Organisation gets 404 on all of it; the Public payload is
   unchanged.
7. `./mvnw test` green with the new unit tests; `npm run build` clean.

## Decided (2026-10-18)

1. Joining from an Organisation with no Roadmap moves the person; with
   Roadmaps, joining is refused.
2. The Organisation has a name, set in Settings.
3. One single-use Invitation per person (email address), 7 days, fixed.
4. Roles Owner, Admin, Editor; recorded now, enforced in a later module.
   Readers (the publisher's clients) have no Role.
5. Invitation emails use the Organisation's SMTP settings.
6. The join page has a single "Accept" button.
7. First sign-in creates no Organisation: the Welcome page offers "Create a
   new organisation" or "Join an organisation" (`design/Onboarding.dc.html`).
   Removed or leaving people go back to it.
8. The person keeps the name Owner in code; the Role is written "Role Owner"
   / `OWNER`.
9. Deleting an Organisation is left for later.
10. (2026-10-19) The Role Owner can close the Organisation: its Roadmaps become
    Private, everyone leaves it, nothing is deleted.

