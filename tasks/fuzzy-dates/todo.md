# Tasks: fuzzy-dates

Spec: `SPEC-fuzzy-dates.md`. Plan: `tasks/fuzzy-dates/plan.md`. All tasks are
_Assignee: Claude_. Backend rules are written test first (plain JUnit, no
Spring context).

## Phase 1: Model

### Task 1: Precision on Item dates (backend)

**Description:** Each date of an Item gets a Precision, normalised to the
first day of its period; the end ≥ start rule compares periods.

**Acceptance criteria:**
- [x] `DatePrecision` enum (`DAY`, `WEEK`, `MONTH`, `QUARTER`, lower-case
      keys) and `PlannedDate.of(date, precision)` / `first()` / `last()`.
- [x] `Item.schedule(PlannedDate, PlannedDate)`; `startPrecision` /
      `endPrecision` stored; no date → Day.
- [x] Changeset `014-item-date-precision.sql` (default `DAY`).
- [x] `ItemRequest`, `ItemResponse` and `PublicItem` carry `startPrecision` /
      `endPrecision`.

**Verification:**
- [x] Unit tests first: `PlannedDateTest`, `ItemTest`, `PublicPageTest`
- [x] `./mvnw test`

**Dependencies:** None
**Files likely touched:** `item/DatePrecision.java`, `item/PlannedDate.java`,
`Item.java`, `ItemRequest.java`, `ItemService.java`, `ItemController.java`,
`PublicPage.java`, `014-item-date-precision.sql`, `db.changelog-master.yaml`
**Estimated scope:** M

## Phase 2: Show and edit

### Task 2: Periods, labels and placement (frontend lib)

**Acceptance criteria:**
- [x] `src/lib/dates.ts`: `Precision`, `periodStart(date, precision)`,
      `periodEnd(first, precision)`, `formatDate`, `endsBeforeStart`.
- [x] `formatDates(item)` gives the labels of the spec's table.
- [x] `place(item, year)` works on periods; single non-Day date → band;
      `fuzzyLeft` / `fuzzyRight` flags.
- [x] `Item`, `ItemDraft`, `PublicItem` types gain the Precisions.

**Verification:** `npx tsc --noEmit`, `npm run build`
**Dependencies:** Task 1
**Estimated scope:** M

### Task 3: Precision in the Item dialog

**Acceptance criteria:**
- [x] Each date is one dropdown: No date; This / Next / In two weeks; This /
      Next / In two months; Q1–Q4 of the current year and Q1–Q2 of the next;
      "Exact date…" with a
      date picker. A saved period outside these choices stays selectable.
- [x] The client-side end ≥ start check compares periods (same message as the
      backend).

**Verification:** build; manual in the browser (human)
**Dependencies:** Task 2
**Estimated scope:** M

### Task 4: Bands and faded edges on the timeline

**Acceptance criteria:**
- [x] Bars and bands span periods; fuzzy edges are faded; clipped edges keep
      their current look.
- [x] Labels everywhere (editor list, timeline aria-labels, Public page, embed,
      Item page) use the new `formatDates`.

**Verification:** build; curl the Public page; browser (human)
**Dependencies:** Task 2
**Estimated scope:** S
