# Implementation Plan: branding

## Overview

Build module `branding` from `SPEC-branding.md` (approved): the Owner sets an
optional Brand (product name, letter mark, no upload) in a new Settings screen,
and Readers see it on every Public page, in its title and link preview, and in
the Feed. Independent of the MVP's open auth phases (`tasks/plan.md`).

Tasks live in `tasks/branding/todo.md`. All are _Assignee: Claude_.

## Architecture Decisions

- **The Brand is a column on `owner`** (`brand_name VARCHAR(60) NULL`, new
  changeset `006-owner-brand.sql`), not a separate table: one Brand per Owner,
  one field.
- **The rule lives on the entity**: `Owner.changeBrand(name)` trims, rejects
  more than 60 characters, and stores blank as null. Unit-tested without Spring.
- **`BrandService` saves explicitly** (`owners.save(owner)`) rather than relying
  on dirty checking, because the Owner comes from `CurrentOwner`, whose real
  implementation (Phase 4) may hand back a detached entity.
- **Readers get the Brand through the Roadmap's Owner**: `PublicPage.of` and
  `RssFeed.of` read `roadmap.getOwner().getBrandName()`, inside the existing
  read-only transaction. No signature change; the exact-fields guard on the
  payload is updated to add `brandName` only.
- **The letter mark is computed in the browser** from the name (first
  character, upper-cased, by code point so an emoji or accented letter is not
  split). One `BrandMark` component serves Settings and the Public page.
- **Page title and link preview come from the route's `head`**, rendered during
  SSR, so link unfurlers (which do not run JavaScript) see them.

## Dependency graph

```
Task 1  Brand on the Owner (schema + rule + /api/brand)
  ├── Task 2  Settings screen
  └── Task 3  Brand for Readers (payload + Feed title)
        └── Task 4  Brand on the Public page (+ title, link preview)   [also uses BrandMark from Task 2]
```

## Task List

### Phase 1: The Owner sets a Brand
- [x] Task 1: Brand on the Owner (backend)
- [x] Task 2: Settings screen

### Checkpoint: Owner side
- [x] `./mvnw test` green, `npm run build` clean
- [x] Setting, changing and clearing the Brand in Settings persists

### Phase 2: Readers see the Brand
- [x] Task 3: Brand in the public payload and the Feed
- [x] Task 4: Brand on the Public page, its title and link preview

### Checkpoint: Complete (Success Criteria of SPEC-branding.md)
- [x] Criteria 1–4 verified (manual curl / browser)
- [ ] Review with human

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Owner data (Google name, email) leaks to Readers with the Brand | High | Only `brandName` is added to the `PublicPage` record; exact-fields test; manual curl of the payload for name / email / provider |
| Link unfurlers miss the title and `og:` tags | Med | Tags set in the route `head` and checked in the SSR HTML with curl |
| Brand edits not saved once real auth hands a detached Owner | Med | `BrandService` calls `owners.save(owner)` |
| Lazy `roadmap.owner` loaded outside a transaction | Low | Mapping and feed building already run inside `PublicPageService`'s read-only transaction |

## Open Questions

None. The spec's questions were settled on approval.
