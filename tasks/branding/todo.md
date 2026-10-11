# Tasks: branding

Spec: `SPEC-branding.md`. Plan: `tasks/branding/plan.md`. All tasks are
_Assignee: Claude_. Backend rules are written test first (plain JUnit, no
Spring context).

## Phase 1: The Owner sets a Brand

### Task 1: Brand on the Owner (backend)

**Description:** Store an optional Brand per Owner and let the Owner read and
change it.

**Acceptance criteria:**
- [ ] Changeset `006-owner-brand.sql` adds `owner.brand_name VARCHAR(60) NULL`.
- [ ] `Owner.changeBrand(name)`: trimmed; blank or null clears it; more than 60
      characters → `ValidationException`.
- [ ] `GET /api/brand` → `{ "name": … }`; `PUT /api/brand` with `{ "name": … }`
      saves and returns it (400 over 60 characters); scoped by `CurrentOwner`.

**Verification:**
- [ ] Unit tests first, then green: `./mvnw test -Dtest=OwnerTest`
- [ ] `./mvnw test`
- [ ] Manual: `PUT` " Acme " → `"Acme"`; `GET` → `"Acme"`; `PUT` "" → null;
      61 characters → 400.

**Dependencies:** None
**Files likely touched:** `006-owner-brand.sql`, `db.changelog-master.yaml`,
`Owner.java`, `OwnerTest.java`, `BrandController.java` (+ `BrandService`)
**Estimated scope:** M

### Task 2: Settings screen

**Description:** The Owner sets their Brand in a new Settings screen and sees
how Readers will see it.

**Acceptance criteria:**
- [ ] `/settings` under the editor layout, with "Settings" in the app bar nav
      after Categories.
- [ ] "Product name" field (≤ 60 characters) with the spec's hint; Save shows
      "Settings saved"; an API error shows inline.
- [ ] Preview: the Public page header (letter mark, Brand, Roadmap name,
      published date) and the link preview (title, origin, description), using
      the first Published Roadmap, or the first Roadmap; "Made with Product
      name" note.

**Verification:**
- [ ] `npx tsc --noEmit`, `npm run build`
- [ ] curl the SSR HTML of `/settings` for the field value and the nav entry
- [ ] Manual: change and clear the Brand; reload; it persists.

**Dependencies:** Task 1
**Files likely touched:** `frontend/src/lib/brand.ts`,
`frontend/src/components/brand-mark.tsx`, `frontend/src/routes/_app/settings.tsx`,
`frontend/src/components/app-bar.tsx`
**Estimated scope:** M

### Checkpoint: Owner side
- [ ] `./mvnw test` green, `npm run build` clean
- [ ] Setting, changing and clearing the Brand in Settings persists

## Phase 2: Readers see the Brand

### Task 3: Brand in the public payload and the Feed

**Description:** Send Readers the Brand, and nothing else of the Owner.

**Acceptance criteria:**
- [ ] `PublicPage` gains `brandName` (null when unset); the exact-fields test
      lists it; Internal Items still dropped.
- [ ] `RssFeed` channel title is "{Roadmap} · {Brand}", or the Roadmap name
      alone without a Brand.

**Verification:**
- [ ] Unit tests first, then green: `./mvnw test -Dtest='PublicPageTest,RssFeedTest'`
- [ ] Manual: the public payload has `brandName` and no Owner name, email or
      provider; the Feed title includes the Brand.

**Dependencies:** Task 1
**Files likely touched:** `PublicPage.java`, `PublicPageTest.java`,
`RssFeed.java`, `RssFeedTest.java`
**Estimated scope:** S

### Task 4: Brand on the Public page, its title and link preview

**Description:** Readers see whose Roadmap it is, and shared links say so.

**Acceptance criteria:**
- [ ] With a Brand, the letter mark and Brand name show above the Roadmap name.
- [ ] `<title>` "{Roadmap} · {Brand}" (Roadmap alone without a Brand);
      `og:title`, `og:description`, `og:type=website`, `twitter:card=summary`
      in the SSR HTML.

**Verification:**
- [ ] `npm run build`; curl the SSR HTML of `/r/{publicId}` with and without a
      Brand for the row, `<title>` and `og:` tags.

**Dependencies:** Tasks 2, 3
**Files likely touched:** `frontend/src/lib/public-page.ts`,
`frontend/src/routes/r.$publicId.tsx`
**Estimated scope:** S

### Checkpoint: Complete (Success Criteria of SPEC-branding.md)
- [ ] 1. An Owner can set, change and clear their Brand in Settings; it
      persists.
- [ ] 2. With a Brand, the Public pages show the letter mark and Brand, and
      their title, `og:` tags and Feed title include it; without one, the
      Roadmap name only.
- [ ] 3. The public payload gains `brandName` and nothing else; no Owner name,
      email or provider is sent.
- [ ] 4. `./mvnw test` green; `npm run build` clean.
- [ ] Review with human
