# Spec: fuzzy-dates

> Module `fuzzy-dates` of `ROADMAP.md`. Status: **approved 2026-10-20.**
> `SPEC.md` stays the source of truth for everything not changed here; the
> changes listed under "Changes to SPEC.md" have been applied to it.

## Objective

Today every date on an Item is a day. Many teams won't publish "March 14" to
their buyers when all they know is "March" or "Q3", so they leave Items
undated or don't publish at all. This module lets the Owner give each date a
**Precision**: a day (as today), a week, a month or a quarter. Readers see
"Q3 2026" drawn as a band over the quarter, not a false-precise day.

This serves the positioning ("publish with confidence"): more Roadmaps get
published, with dates the Owner can stand behind.

## Language (additions)

**Precision**:
How exact one date of an Item is: **Day**, **Week**, **Month** or **Quarter**.
Each of the start and end dates has its own. A Week, Month or Quarter date
stands for the whole period: as a start, its first day; as an end, its last
day. A Week runs Monday to Sunday. Precision
says how sure the plan is; it never says anything about Status.
_Avoid_: granularity, resolution, accuracy, estimate, fuzzy (in the UI)

"Fuzzy dates" remains the module id only; the UI says "Day", "Week",
"Month", "Quarter".

## Requirements

### Item (Owner, editor)
- Each date of the Item dialog is **one dropdown**, its choices computed from
  today and grouped by Precision:
  - No date;
  - Week: This week, Next week, In two weeks (each with its Monday, e.g.
    "Next week (Oct 26)");
  - Month: This month, Next month, In two months (e.g. "Next month (Nov
    2026)");
  - Quarter: Q1, Q2, Q3 and Q4 of the current year, then Q1 and Q2 of the
    next year;
  - Day: "Exact date…", which shows a date picker below the dropdown.
- A choice is stored as the absolute period it names when saved: "Next month"
  picked in October 2026 is Nov 2026, and stays Nov 2026. The dropdown is a way
  to pick, not a relative date.
- A saved period that is no longer among the choices (last month, last year's
  Q3) appears as an extra option with its label ("Sep 2026") and is kept
  unless the Owner picks another.
- Start and end are still independent: none, start only, end only or both, and
  each can have its own Precision (start Mar 14, end Q3 is valid).
- **Rule:** when both are set, the end period must not finish before the start
  period begins: last day of the end ≥ first day of the start. "Start Q3, end
  Jul 2026" is valid; "start Q3, end Jun 2026" is not.
- Existing Items keep their dates with Precision Day. Nothing changes for them.

### Display (editor and Public page)
- Labels:

  | Dates | Label |
  |---|---|
  | Week | `Week of Mar 9, 2026` |
  | Month | `Mar 2026` |
  | Quarter | `Q3 2026` |
  | Both, same year | `Mar – Q3 2026`, `Jan 4 – Mar 2026`, `Week of Mar 9 – Apr 2026` |
  | Both, different years | `Q4 2026 – Q1 2027` |
  | Start only / end only | `From Q3 2026` / `By Mar 2026` |

  Day-only labels stay as today.
- Weeks are named by their Monday ("Week of …"), not by ISO week numbers,
  which Readers don't use.
- Timeline, both dates: a bar from the first day of the start period to the
  last day of the end period. The end of a Week, Month or Quarter date is drawn with
  a soft (faded) edge, the same at the start, so Readers can tell a planned
  period from a precise day.
- Timeline, single Week, Month or Quarter date: a band over its period with soft
  edges, instead of a point. A single Day date stays a point.
- An Item is on the year shown when its period overlaps it; the "outside the
  year" count and clipping follow the period.
- Manual order, Status, Recently shipped, the Feed and emails are unchanged.
  Shipped dates (`shippedAt`) are always exact and have no Precision.

### API
- Owner API (`/api/roadmaps/{id}/items`): Items gain `startPrecision` and
  `endPrecision`: `day` | `week` | `month` | `quarter` (JSON), `DAY` | `WEEK` |
  `MONTH` | `QUARTER` (enum). `startDate` / `endDate` stay ISO dates.
- On save, the backend normalises a Week, Month or Quarter date to the first
  day of its period (`2026-03-14` + `week` → `2026-03-09`; + `month` →
  `2026-03-01`; + `quarter` → `2026-01-01`). A missing Precision means `day`; a Precision without a date is
  ignored and stored as Day.
- Public API payload: each Item gains `startPrecision` and `endPrecision`.
  Nothing else changes.

## Data model (new changeset)

```
014-item-date-precision.sql
item  + start_precision VARCHAR(8) NOT NULL DEFAULT 'DAY'
      + end_precision   VARCHAR(8) NOT NULL DEFAULT 'DAY'
```

The date columns keep the first day of the period; the period's last day is
computed, never stored.

## Code style

As `SPEC.md`. The rule lives in a small value type, testable without Spring:

```java
/** One date of an Item: a day, or the whole week, month or quarter it starts. */
public record PlannedDate(LocalDate first, DatePrecision precision) {
    public static PlannedDate of(LocalDate date, DatePrecision precision) { /* normalise to first day */ }
    public LocalDate last() { /* first, Sunday, end of month, or end of quarter */ }
}

// Item.schedule(PlannedDate start, PlannedDate end):
// both set and end.last() before start.first() → "End date must be on or after the start date"
```

Frontend: `formatDates` and `place` (`src/lib/format.ts`, `src/lib/timeline.ts`)
take the Precision; one helper gives a date's period (first day, last day).

## Testing strategy

Backend unit tests (JUnit, no Spring context, written first):
- Normalisation: any day of a week + Week → its Monday (a Sunday too); any
  day of March + Month → Mar 1; any day of Q3 + Quarter → Jul 1; Day
  unchanged.
- Last day: Week (Sunday, including a week that runs into the next year), Month (including February in a leap year), Quarter (Mar 31, Jun 30,
  Sep 30, Dec 31).
- Rule: start Q3 / end Jul valid; start Q3 / end Jun invalid; start Mar 14 / end
  Mar (month) valid; mixed precisions; either date missing.
- Defaults: no Precision → Day; Precision without a date → Day.
- Public mapping: Precisions are sent; Internal Items still dropped.

Manual, against the running app:
- Each label in the table above, in the editor and on the Public page.
- A Q4 2026 – Q1 2027 Item is clipped at both year edges and counted on both
  years.
- An existing Item opens with Day Precision and is unchanged after save.

Frontend: no automated tests (as in `SPEC.md`).

## Changes to SPEC.md (applied)

- Language: add Precision.
- Items: "an optional start date and an optional end date (day precision, each
  independent)" becomes "…each with its own Precision (Day, Week, Month or
  Quarter)";
  the end ≥ start rule compares periods.
- Public page: bars and bands follow periods; Public API payload adds
  `startPrecision`, `endPrecision`.

## Boundaries

- Always: compare periods, not stored dates, when checking end ≥ start and
  placing an Item on a year.
- Ask first: any other Precision (half-year, year).
- Never: Now / Next / Later (decided 2026-10-20); derive Status from a date's period (an Item in the current quarter is
  not In progress); give `shippedAt` a Precision.

## Success criteria

1. An Owner can set each date of an Item to a day, a week, a month or a quarter, and
   the end ≥ start rule compares the periods.
2. The editor and the Public page label and draw Week, Month and Quarter dates as
   their periods, with soft edges, in timeline and list views.
3. Existing Items are unchanged (Day Precision).
4. The Public API sends the Precisions and still no Internal Item.
5. `./mvnw test` green with the new unit tests; `npm run build` clean.

## Decided (2026-10-20)

1. Precisions are Day, Week, Month and Quarter. Day stays the default, so
   existing Items are unchanged.
2. No Now / Next / Later: it is not a date, and "Now" would overlap Status In
   progress.

3. Soft edges are a fade on the bar's ends (the proposal, approved with the
   spec).
4. One dropdown of choices near today (this / next / in two weeks and months,
   the quarters of the current year and the first two of the next, or an
   exact day) instead of a Precision
   switch with month, quarter and year fields (2026-10-20).
