# Implementation Plan: fuzzy-dates

## Overview

Build module `fuzzy-dates` from `SPEC-fuzzy-dates.md` (approved 2026-10-20):
each date of an Item gets a Precision (Day, Week, Month, Quarter), labelled and
drawn as its period in the editor and on the Public page.

Tasks live in `tasks/fuzzy-dates/todo.md`. All are _Assignee: Claude_.

## Architecture Decisions

- **`PlannedDate` value type** (`first`, `precision`, `last()`), with
  normalisation to the first day of the period. Pure, unit-tested; `Item`
  stores its `first` and `precision` in two columns per date.
- **`Item.schedule(PlannedDate start, PlannedDate end)`** compares
  `end.last()` with `start.first()`. A null date is "no date"; a Precision
  without a date is dropped (stored as Day).
- **Schema**: changeset `014-item-date-precision.sql`, two `VARCHAR(8) NOT
  NULL DEFAULT 'DAY'` columns. Existing rows become Day, nothing else moves.
- **JSON**: `startPrecision` / `endPrecision` as lower-case keys
  (`day|week|month|quarter`), like Status. Missing in a request → `day`.
- **Frontend helpers** `periodStart` / `periodEnd` (`src/lib/dates.ts`) give
  the first and last day; `formatDates` and `place` take an Item-like
  `{ startDate, startPrecision, endDate, endPrecision }` and work on periods.
  Placement gains `fuzzyLeft` / `fuzzyRight` flags; a single non-Day date is a
  bar (band) with both edges fuzzy.
- **Faded edges** are a CSS mask gradient on the bar's fuzzy side, in
  `year-timeline.tsx`; nothing else in the timeline changes.

## Dependency graph

```
Task 1  PlannedDate + Item + schema + APIs (backend)
  └── Task 2  period helper, labels, placement (frontend lib)
        ├── Task 3  Item dialog: Precision switch and inputs
        └── Task 4  Timeline bands and faded edges (editor + Public page + embed + Item page)
```

## Task List

### Phase 1: Model
- [x] Task 1: Precision on Item dates (backend)

### Phase 2: Show and edit
- [x] Task 2: Periods, labels and placement (frontend lib)
- [x] Task 3: Precision in the Item dialog
- [x] Task 4: Bands and faded edges on the timeline

### Checkpoint: Complete (Success Criteria of SPEC-fuzzy-dates.md)
- [x] `./mvnw test` green, `npm run build` clean
- [ ] Criteria 1–4 verified (curl + browser by the human)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Existing Items change meaning | High | Default `DAY` in the changeset; normalisation is a no-op for Day; unit test |
| Week across the new year placed on the wrong year | Med | Placement uses the period's first/last day, not the year of `first`; unit test of `last()` across Dec/Jan |
| Hibernate `validate` rejects the enum columns | Low | `@Enumerated(STRING)` on `VARCHAR(8)`, as Status |
| The Public payload grows by more than the spec says | Med | `PublicPageTest` exact-fields test updated on purpose |
