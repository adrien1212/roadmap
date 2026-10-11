# Implementation Plan: changelog

## Overview

Build module `changelog` from `SPEC-changelog.md` (approved): a Shipped date
recorded when an Item becomes Shipped, a "Recently shipped" section on the
Public page, and an RSS feed per Published Roadmap. The MVP plan
(`tasks/plan.md`) is untouched; its open Phases 4–5 (authentication) are
independent of this module.

Tasks live in `tasks/changelog/todo.md`. All are _Assignee: Claude_.

## Architecture Decisions

- **The Shipped date rule lives in the entity**, with the clock passed in:
  `Item.changeStatus(status, now)`. `ItemService` passes `Instant.now()`, as
  `RoadmapService.publish` does. Unit-testable without Spring.
- **Schema through a new changeset** `005-item-shipped-at.sql` (nullable
  `DATETIME(6)`). No backfill: Items already Shipped keep `shipped_at` null.
- **`shippedAt` goes through both read models**: the editor's `ItemResponse`
  (for the read-only date in the Item dialog) and the public `PublicItem`.
  `PublicPageTest`'s exact-fields test is updated on purpose; it is the guard
  that the payload only grows by what the spec says.
- **Recently shipped is computed in the browser** from the public payload
  (`shippedAt` desc, non-null, first 3). Internal Items never reach the browser,
  so this cannot leak them, and the API stays one endpoint.
- **The Feed is a pure builder** (`RssFeed.of(...)` with its own XML escaper),
  not a library: no new dependency. The controller only adds HTTP concerns
  (content type, `Cache-Control`, `ETag` / 304).
- **The Feed query filters on the backend**: non-Internal, Shipped, with a
  Shipped date, newest first, limited to 20, on a Roadmap found by
  `findByPublicIdAndPublishedAtIsNotNull` (same 404 as the Public page).
- **Absolute URLs in the Feed** come from the request's public origin
  (`ServletUriComponentsBuilder` + `forward-headers-strategy=framework`), as the
  OAuth redirects will.

## Dependency graph

```
Task 1  Shipped date (schema + rule + editor API)
  ├── Task 2  Shipped date in the Item dialog
  ├── Task 3  shippedAt in the public payload + Recently shipped
  └── Task 4  RSS feed endpoint
        └── Task 5  Feed links on the Public page
```

## Task List

### Phase 1: Shipped date
- [x] Task 1: Shipped date on Items (backend)
- [x] Task 2: Shipped date in the Item dialog

### Checkpoint: Shipped date
- [x] `./mvnw test` green, `npm run build` clean
- [x] Switching an Item to Shipped and back sets then clears its date

### Phase 2: Readers see what shipped
- [x] Task 3: Recently shipped on the Public page
- [x] Task 4: RSS feed endpoint
- [x] Task 5: Feed links on the Public page

### Checkpoint: Complete (Success Criteria of SPEC-changelog.md)
- [x] Criteria 1–5 verified (manual curl / browser)
- [ ] Review with human

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| An Internal Item leaks through the new payload field or the Feed | High | Feed query filters `internal = false`; `RssFeed` test asserts no Internal Item; public mapping test kept; manual check at the checkpoint |
| Existing Shipped Items have no date, so Recently shipped looks empty on old Roadmaps | Med | Accepted in the spec (no invented dates); the dates appear as Owners ship again |
| Feed links point at `:8045` instead of the public origin | Med | Build URLs from the forwarded request; verify through port 3000 with curl |
| ETag ignores edits to an already-shipped Item's title | Low | Resolved in Task 4: the ETag is an MD5 of the feed content, so any change in it changes the ETag |
| Hibernate `validate` rejects the new column type | Low | `DATETIME(6)` already validates for `Instant` (`published_at`) |

## Open Questions

None. The spec's questions were settled on approval.
