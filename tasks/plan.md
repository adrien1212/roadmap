# Implementation Plan: Roadmap MVP

## Overview

Build the MVP described in `SPEC.md`: an Owner manages Categories, Roadmaps
and Items in an editor, and publishes a Roadmap to an anonymous Public page at
`/r/{publicId}`; Owners sign in with Google from the Home page.

**The whole app is built first without authentication**, running as a single
dev Owner. **Authentication (Google) comes last and is implemented by
the human as a learning exercise** (_Assignee: you_): those tasks give the
goal, the contract and the concepts to learn — not the code. All other tasks
are _Assignee: Claude_.

Detailed tasks live in `tasks/todo.md`.

## Architecture Decisions

- **Same-origin via proxy.** The browser only talks to port 3000; Vite
  `server.proxy` forwards `/api/**`, `/oauth2/**`, `/login/**`, `/logout` to
  Spring on 8045. No CORS, first-party session cookie.
- **Spring is the OAuth2 client and owns the session.** The frontend never
  sees a token; it only knows "signed in or not" through `GET /api/me`.
- **Owner identity = (provider, providerSubject)**, i.e. (`google`, OIDC `sub`).
- **`GET /api/me` → `{id, name, email, provider}`** (Claude, Task 3) built on
  `CurrentOwner`; the header shows the Owner's initials and "Signed in with
  {provider}". Returns 401 once real auth exists.
- **Auth seam: `CurrentOwner { Owner get(); }`.**
  - Until auth: `DevCurrentOwner` is the only implementation — it finds or
    creates a fixed Owner (`provider = dev`), so the app always runs as that
    Owner. No dev data in Liquibase changesets.
  - At the end, you write the real implementation (Security context,
    anonymous → 401) and `DevCurrentOwner` is deleted. Feature code only knows
    the interface and does not change.
- **Rules from day one so auth drops in cheaply:**
  1. Every authenticated query is scoped by `CurrentOwner.get()`
     (repositories expose `findByIdAndOwner` / `findAllByOwner`, never a bare
     `findById`). Another Owner's resource → 404.
  2. Every frontend call goes through one API client (`#/lib/api`), so the
     SSR cookie and CSRF header are added in one place later.
  3. Editor routes sit under a pathless layout route (`_app`); the Public page
     (`/r/*`) sits outside it, so the sign-in guard lands on one file.
- **Public API is a separate read model** (`/api/public/roadmaps/{publicId}`):
  no Internal Items, no Owner data, 404 when Private.
- **Category colour is a palette key** (8 values, see SPEC), stored as a
  string and validated by an enum.
- **Case-insensitive Category names** rely on MariaDB's default
  case-insensitive collation for the unique (owner, name) constraint; names
  are trimmed before saving.
- **Item delete with Undo is client-side**: the UI hides the Item and sends
  `DELETE` after 5 s unless undone; no soft delete in the backend.
- **Category removal via FK `ON DELETE SET NULL`** on `item.category_id`;
  Roadmap removal cascades to Items.
- **Schema only through Liquibase changesets**, written as formatted SQL
  (`db/changelog/changes/NNN-name.sql`, included from the YAML master);
  JPA `ddl-auto=validate`.
- **Domain rules live in plain Java** (entity methods / small services without
  Spring) so they are unit-testable without a context or database.
- **Tests: unit only.** API behaviour checked manually with `curl` through
  port 3000.
- **Timeline component is shared** between editor and Public page.
- **UI follows the mockups in `design/`.**
- **Dependencies to add** (approving this plan approves them): Liquibase
  starter, `@dnd-kit`, shadcn components as needed, Spring Security OAuth2
  client starter (you, Phase 4).

## Task List

### Phase 1: Foundation — Claude
- [x] Task 1: Database and Liquibase
- [x] Task 2: Dev proxy and forwarded headers
- [x] Task 3: Owner schema, `CurrentOwner` and `DevCurrentOwner`

### Checkpoint: Foundation

### Phase 2: Backend APIs — Claude
- [ ] Task 4: Categories API
- [ ] Task 5: Roadmaps API
- [ ] Task 6: Items API
- [ ] Task 7: Items reorder API
- [ ] Task 8: Publish / unpublish and public API

### Checkpoint: APIs

### Phase 3: Frontend — Claude
- [x] Task 9: Compare `design/` mockups with SPEC
- [ ] Task 10: API client and app layout
- [ ] Task 11: Categories UI
- [ ] Task 12: Roadmaps UI

### Checkpoint: Roadmaps & Categories UI

- [ ] Task 13: Editor list view with Item form
- [ ] Task 14: Drag-and-drop reorder
- [ ] Task 15: Year timeline component and editor timeline view
- [ ] Task 16: Publish controls in the editor
- [ ] Task 17: Public page
- [ ] Task 18: Home page

### Checkpoint: App complete without auth

### Phase 4: Authentication — you (learning)
- [ ] Task 19: Sign in with Google (end to end)
- [ ] Task 20: Owner provisioning and real `CurrentOwner`

### Phase 5: Session integration — Claude
- [ ] Task 21: Remove the stub, wire sign-in, guard, SSR cookie
- [ ] Task 22: CSRF for the SPA

### Checkpoint: Complete (all Success Criteria of SPEC.md)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Session-only problems surface late (redirect instead of 401, SSR cookie, CSRF) | Med | Known list, each centralised by the three day-one rules; handled in Tasks 20, 21, 22 |
| A query not scoped by Owner slips in while only one Owner exists | High | Repositories never expose bare `findById`; manual cross-Owner check at the final checkpoint |
| Internal Items leak to Readers (no security/integration tests) | High | Public DTO separate from editor DTO; unit test on the public mapping; manual check |
| `DevCurrentOwner` left in place | Med | Deleted in Task 21; two `CurrentOwner` beans would fail startup anyway |
| OAuth redirect URIs break behind the proxy (Spring builds `:8045` URLs) | High | `forward-headers-strategy=framework` + `X-Forwarded-*` (Task 2); verify in Task 19 |
| Mockups diverge from SPEC | Med | Task 9 lists mismatches for a human decision before UI work |
| Spring Boot 4 / Java 27 starter names differ from older tutorials | Med | Check Boot 4 docs when adding starters |
| Timeline layout (partial dates, cross-year bars) | Med | Pure layout function, reused in editor and Public page |
