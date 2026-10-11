# Tasks: Roadmap MVP

See `tasks/plan.md` for decisions, `SPEC.md` for requirements. Backend unit
tests: `cd backend && ./mvnw test`. Frontend build: `cd frontend && npm run
build`. DB: `docker compose up -d`.

Until Phase 4, the app runs as a single dev Owner (`DevCurrentOwner`). Manual
API checks go through port 3000 (`curl -i localhost:3000/api/...`).

---

## Phase 1: Foundation — Assignee: Claude

### Task 1: Database and Liquibase

**Acceptance criteria:**
- [ ] Datasource for the compose MariaDB in `application.properties`
      (env vars with dev defaults); `ddl-auto=validate`.
- [ ] Liquibase starter added; master changelog runs at startup.
- [ ] Spring-context test `RoadmapApplicationTests` removed (unit tests only);
      `./mvnw test` passes without a DB.

**Verification:**
- [ ] `./mvnw test` green with Docker stopped
- [ ] Manual: `docker compose up -d && ./mvnw spring-boot:run` starts and
      creates `DATABASECHANGELOG`.

**Dependencies:** None
**Files likely touched:** `backend/pom.xml`, `application.properties`,
`db/changelog/db.changelog-master.yaml`, `RoadmapApplicationTests.java`
**Estimated scope:** S

### Task 2: Dev proxy and forwarded headers

**Acceptance criteria:**
- [ ] Vite `server.proxy` forwards `/api`, `/oauth2`, `/login`, `/logout` to
      `localhost:8045` with `X-Forwarded-*` (host 3000).
- [ ] Spring sets `server.forward-headers-strategy=framework`.
- [ ] Frontend `/` still renders.

**Verification:**
- [ ] Manual: `curl -i localhost:3000/api/does-not-exist` returns Spring's 404.
- [ ] `npm run build`

**Dependencies:** Task 1
**Files likely touched:** `frontend/vite.config.ts`, `application.properties`
**Estimated scope:** XS

### Task 3: Owner schema, `CurrentOwner` and `DevCurrentOwner`

**Acceptance criteria:**
- [ ] Changeset `owner` (id, provider, provider_subject, email, name,
      created_at; unique provider + provider_subject); `Owner` entity +
      `OwnerRepository.findByProviderAndProviderSubject`.
- [ ] Interface `CurrentOwner { Owner get(); }` documented as the auth seam.
- [ ] `DevCurrentOwner` (default, only bean): finds or creates the
      `dev`/`dev` Owner; no seed data in changesets.
- [ ] `GET /api/me` → `{id, name, email, provider}` from `CurrentOwner`.

**Verification:**
- [ ] `./mvnw test`; Manual: app starts, `owner` table exists.

**Dependencies:** Task 1
**Files likely touched:** changeset, `Owner.java`, `OwnerRepository.java`,
`CurrentOwner.java`, `DevCurrentOwner.java`
**Estimated scope:** S

### Checkpoint: Foundation
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] Both apps run together through port 3000

---

## Phase 2: Backend APIs — Assignee: Claude

All endpoints scope data via `CurrentOwner.get()` (`findByIdAndOwner`,
`findAllByOwner`); another Owner's id → 404.

### Task 4: Categories API

**Acceptance criteria:**
- [x] Changeset `category` (owner FK, name, colour; unique owner + name,
      case-insensitive via the default collation).
- [x] `GET/POST /api/categories`, `PATCH/DELETE /api/categories/{id}`;
      colour must be a palette key (else 400); name trimmed, required;
      duplicate name in any case → 409. `GET` includes `itemCount` (all
      Roadmaps).
- [x] Unit tests for name trimming and colour validation.

**Verification:** `./mvnw test`; Manual: CRUD via curl.
**Dependencies:** Task 3
**Estimated scope:** M

### Task 5: Roadmaps API

**Acceptance criteria:**
- [x] Changeset `roadmap` (owner FK, name, `public_id` unique random
      ≥ 128-bit URL-safe, `published_at` null).
- [x] `GET/POST /api/roadmaps`, `GET/PATCH/DELETE /api/roadmaps/{id}`; new
      Roadmap is Private; `publicId` never changes; name required,
      ≤ 120 chars; `POST` without name → "Untitled Roadmap".
- [x] `GET /api/roadmaps` returns per Roadmap: `itemCount`,
      `internalItemCount`, `publishedAt`.
- [x] Unit tests for `publicId` generation.

**Verification:** `./mvnw test`; Manual: CRUD via curl.
**Dependencies:** Task 3
**Estimated scope:** M

### Task 6: Items API

**Acceptance criteria:**
- [x] Changeset `item` (roadmap FK cascade, category FK `ON DELETE SET NULL`,
      title, description, start_date, end_date, status, internal, position).
- [x] CRUD under `/api/roadmaps/{id}/items`; title required, ≤ 140 chars;
      end ≥ start when
      both set (else 400); defaults `planned`, no Category, not Internal,
      appended last; Category must belong to the same Owner.
- [x] Unit tests: date rule, defaults, position append.

**Verification:** `./mvnw test`; Manual: deleting a Category clears it on
Items; deleting a Roadmap removes its Items.
**Dependencies:** Tasks 4, 5
**Estimated scope:** M

### Task 7: Items reorder API

**Acceptance criteria:**
- [x] `PUT /api/roadmaps/{id}/items/order` with the full ordered id list;
      missing/foreign ids → 400.
- [x] Unit test for the reorder logic.

**Verification:** `./mvnw test`; Manual via curl.
**Dependencies:** Task 6
**Estimated scope:** S

### Task 8: Publish / unpublish and public API

**Acceptance criteria:**
- [x] `POST /api/roadmaps/{id}/publish` sets/moves `publishedAt`;
      `POST .../unpublish` clears it.
- [x] `GET /api/public/roadmaps/{publicId}` returns exactly the payload in
      SPEC (name, `publishedAt`, Categories in use, non-Internal Items with
      descriptions); no Owner data, no `internal` flag; Private or unknown →
      identical 404.
- [x] Unit tests: publish/unpublish rules, public mapping drops Internal Items.

**Verification:** `./mvnw test`; Manual: payload has no Internal Item;
Private → 404; republish keeps the URL.
**Dependencies:** Task 6
**Estimated scope:** M

### Checkpoint: APIs
- [x] `./mvnw test` green
- [x] Full manual curl pass through port 3000
- [x] Review with human

---

## Phase 3: Frontend — Assignee: Claude

### Task 9: Compare `design/` mockups with SPEC

**Description:** Read `design/roadmap-ui.html` and `design/ui-design.html`
and list every mismatch with `SPEC.md` (fields, terms, flows, screens).

**Acceptance criteria:**
- [x] Mismatch list presented to the human; each resolved (update SPEC or
      follow SPEC) before Task 10.
- [x] Screens mapped: `roadmap-ui.html` for all app screens,
      `ui-design.html` for the Home page and UI kit.

**Verification:** Human sign-off.
**Dependencies:** Checkpoint APIs (can run earlier)
**Estimated scope:** XS

### Task 10: API client and app layout

**Acceptance criteria:**
- [x] `#/lib/api` client used by all calls: relative `/api/...` in the
      browser; absolute Spring URL during SSR; JSON errors surfaced.
- [x] Pathless layout route `_app` with the app bar from `roadmap-ui.html`
      (Roadmaps / Categories nav, Owner initials menu from `/api/me`);
      `/r/*` and `/` outside it.
- [x] Design tokens (colours, type, radii) from the UI kit in Tailwind
      theme; toast component for confirmations and Undo.
- [x] Starter demo content removed.

**Verification:** `npm run build`; Manual: editor shell renders, also on hard
refresh (SSR path).
**Dependencies:** Task 9
**Files likely touched:** `src/lib/api.ts`, `src/routes/_app.tsx`,
`src/routes/__root.tsx`, `src/routes/index.tsx`
**Estimated scope:** M

### Task 11: Categories UI

**Acceptance criteria:**
- [x] Categories screen: list with Item counts, create (name + palette
      swatch, next colour preselected), rename inline, recolour via swatch
      popover, delete with confirm stating how many Items it is cleared from.
- [x] Duplicate name error inline.

**Verification:** `npm run build`; Manual: changes persist.
**Dependencies:** Task 10
**Estimated scope:** M

### Task 12: Roadmaps UI

**Acceptance criteria:**
- [ ] Roadmaps screen: cards with name, Item / Internal counts,
      Published N days ago / Private (no preview bars, unlike the mockup);
      menu with open, view Public page,
      delete (confirm); empty state.
- [ ] "New Roadmap" creates "Untitled Roadmap" and opens the editor with the
      name in inline-rename mode.

**Verification:** `npm run build`; Manual: changes persist.
**Dependencies:** Task 10
**Estimated scope:** M

### Checkpoint: Roadmaps & Categories UI
- [ ] Success Criterion 3 holds; review with human

### Task 13: Editor list view with Item form

**Acceptance criteria:**
- [ ] Items in manual order with Status, Category chip, dates, Internal
      marker; optional swimlanes by Category (+ "No Category").
- [ ] Create/edit side drawer (all Item fields) with validation; delete with
      confirm, then Undo toast (5 s, `DELETE` sent after the window).
- [ ] Category legend with per-Roadmap counts and inline "+ Category".

**Verification:** `npm run build`; Manual: Items with no / start-only /
end-only / both dates.
**Dependencies:** Tasks 11, 12
**Estimated scope:** M

### Task 14: Drag-and-drop reorder

**Acceptance criteria:**
- [ ] Reorder by drag and drop and by arrow keys on the handle
      (`@dnd-kit`), optimistic with rollback; survives refresh; swimlanes
      never change the order.

**Verification:** `npm run build`; Manual.
**Dependencies:** Task 13
**Estimated scope:** S

### Task 15: Year timeline component and editor timeline view

**Acceptance criteria:**
- [ ] Reusable `YearTimeline`: 12 months, bars (clipped to the year, with
      cut markers), points for single-date Items, undated omitted,
      "Today" line in the current year; previous/next year.
- [ ] Editor list/timeline toggle; optional swimlanes; "No dates" section
      below the timeline.

**Verification:** `npm run build`; Manual: cross-year Item clipped on both
years.
**Dependencies:** Task 13
**Estimated scope:** M

### Task 16: Publish controls in the editor

**Acceptance criteria:**
- [ ] Publish dialog (URL, facts about Internal Items / no draft), Share
      dialog when Published (copy link, Publish again, View Public page,
      Unpublish with confirm), as in the mockup.

**Verification:** `npm run build`; Manual.
**Dependencies:** Task 13
**Estimated scope:** S

### Task 17: Public page

**Acceptance criteria:**
- [ ] `/r/$publicId` with SSR, outside `_app`; 404 page on API 404.
- [ ] Header with name and published date; timeline (current year,
      navigation, expandable rows with description, note about undated /
      outside-year Items) and list views (with descriptions); filters by
      Category (in use) and Status; no swimlanes; "Page not found" page.

**Verification:** `npm run build`; Manual in a private window: filter, change
year; unpublish → 404.
**Dependencies:** Tasks 15, 16
**Estimated scope:** M

### Task 18: Home page

**Acceptance criteria:**
- [ ] `/` (outside `_app`) follows the Landing page layout and style of
      `design/ui-design.html`, with a single "Continue with Google" button
      (links to `/oauth2/authorization/google`; inert until Phase 4).
- [ ] Copy uses the Language terms; the only date mentioned is the published
      date (no "last updated").

**Verification:** `npm run build`; Manual: page matches the mockup.
**Dependencies:** Task 10
**Files likely touched:** `src/routes/index.tsx`, home components
**Estimated scope:** S

### Checkpoint: App complete without auth
- [ ] Success Criteria 2–6 verified manually (as the dev Owner)
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] Review with human — ready for Phase 4

---

## Phase 4: Authentication — Assignee: you (learning)

Implement the real `CurrentOwner` behind the existing interface; feature code
should not need to change. Tell Claude if the contract moves.

### Task 19: Sign in with Google (end to end)

**Acceptance criteria:**
- [ ] OAuth client in Google Cloud Console; redirect URI
      `http://localhost:3000/login/oauth2/code/google`.
- [ ] OAuth2 client starter added; client id/secret from env vars (never
      committed).
- [ ] `localhost:3000/oauth2/authorization/google` round-trips back to
      `localhost:3000` with a `JSESSIONID` cookie for `localhost:3000`.

**Verification:** Manual round trip; the Network tab never shows `:8045`.

**Concepts to learn:** Authorization Code flow; OpenID Connect vs plain
OAuth2 (`id_token`, `sub`, scopes `openid profile email`); Spring's
`/oauth2/authorization/{registrationId}` and
`/login/oauth2/code/{registrationId}`; the `state` parameter; forwarded
headers behind a proxy.

**Dependencies:** Checkpoint App complete without auth
**Files likely touched:** `pom.xml`, `application.properties`, `SecurityConfig`
**Estimated scope:** S

### Task 20: Owner provisioning and real `CurrentOwner`

**Acceptance criteria:**
- [ ] First sign-in creates the Owner (`provider = google`, `sub`), later
      sign-ins reuse it.
- [ ] Real `CurrentOwner` from the security context; `GET /api/me` (already
      built in Task 3) returns the Google Owner, or 401 when anonymous.
- [ ] `/api/**` → 401 when anonymous (no redirect); `/api/public/**`
      anonymous; `POST /logout` ends the session.

**Verification:** Manual: sign in twice → one `owner` row;
`curl -i localhost:3000/api/me` → 401. (Startup fails while
`DevCurrentOwner` still exists — delete it or leave that to Task 21.)

**Concepts to learn:** `OidcUserService` / `OAuth2UserService`;
`SecurityFilterChain` matchers; `AuthenticationEntryPoint` (401 vs redirect);
session fixation and logout.

**Dependencies:** Task 19
**Files likely touched:** `SecurityConfig`, provisioning service, real
`CurrentOwner`
**Estimated scope:** M

---

## Phase 5: Session integration — Assignee: Claude

### Task 21: Remove the stub, wire sign-in, guard, SSR cookie

**Acceptance criteria:**
- [ ] `DevCurrentOwner` deleted; the dev Owner's data is left as is or
      cleaned up (ask).
- [ ] `_app` guards via `GET /api/me` → redirect to `/` (Home page);
      signed-in Owner on `/` → Roadmaps; header shows Owner name and sign-out.
- [ ] API client forwards the incoming `Cookie` header during SSR.

**Verification:** `npm run build`; Manual: anonymous → `/`; signed in →
editor, also on hard refresh; `/r/*` still anonymous.
**Dependencies:** Task 20
**Files likely touched:** `DevCurrentOwner.java`, `src/routes/_app.tsx`,
`src/routes/index.tsx`, `src/lib/api.ts`
**Estimated scope:** M

### Task 22: CSRF for the SPA

**Acceptance criteria:**
- [ ] Spring issues a JS-readable CSRF cookie; API client sends the header on
      mutating requests (browser and SSR).
- [ ] Sign-out works from the UI; mutating request without token → 403
      (manual curl).

**Verification:** Manual as above.
**Dependencies:** Task 21
**Files likely touched:** `SecurityConfig` (minimal change to your file),
`src/lib/api.ts`
**Estimated scope:** S

### Checkpoint: Complete
- [ ] All Success Criteria in `SPEC.md` verified manually, including:
  - [ ] A second Google account gets 404 on the first Owner's Roadmap, Item
        and Category ids
  - [ ] Anonymous `/api/roadmaps` → 401; `/api/public/...` works anonymously
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] Ready for review
