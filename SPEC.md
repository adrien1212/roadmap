# Spec: Roadmap (MVP)

## Objective

A product team (one person, the **Owner**) maintains dated Roadmaps and presses
Publish to expose each one at a public, unguessable URL for the people who buy
from them (**Readers**). The domain exists to keep those two sides apart: the
Owner who authors, and the anonymous Readers who only read.

### In scope (MVP)
- Owner sign-in with Google (OAuth2 / OpenID Connect).
- Home page at `/` that presents the product and offers Google sign-in.
- Roadmap editor: CRUD on Roadmaps, Items and Categories.
- Publish / unpublish a Roadmap; anonymous Public page.

### Out of scope (MVP)
- Workspaces, multiple people per account, invitations, roles.
- Billing / paying Customers.
- Custom domains, embeds, Reader feedback. (Email subscriptions are in scope
  since `SPEC-reader-updates.md`.)

## Language

These terms are binding for code, API, database and UI naming. Respect the
_Avoid_ lists.

**Owner**:
A person signed in with Google. Owns a set of Roadmaps and one set of Categories.
Can do everything to what they own and nothing to anyone else's.
_Avoid_: user, member, author, editor, admin, account

**Roadmap**:
A named, publishable timeline belonging to one Owner. Holds one set of Items,
and is either Published or Private.
_Avoid_: board, plan, project, timeline

**Item**:
One thing on a Roadmap — the unit a Reader sees as a single row. Has a title
(required), a plain-text description, an optional start date and an optional
end date (day precision, each independent), a Status,
at most one Category, an Internal flag, and a manual position.
_Avoid_: feature, card, arrow, initiative, epic, ticket

**Category**:
An optional single label on an Item (name + colour), shown as a colour chip and
used as a filter. Categories belong to the Owner and are shared across all
their Roadmaps. The editor may group Items into swimlanes by Category; a Public
page never does. A lane is a layout, never a name for the Category itself.
_Avoid_: tag, label, track, group

**Status**:
Where an Item stands — Planned, In progress, or Shipped — as asserted by the
Owner. Never inferred from the Item's dates, which describe a plan rather than a
claim.
_Avoid_: state, stage, phase, progress

**Internal Item**:
An Item the Owner sees in the editor but that is removed from the Public page.
It is not "unpublished" — the Roadmap around it is Published, the Item is
withheld.
_Avoid_: private, hidden, secret, unpublished, internal-only

**Publish**:
The act of making a Roadmap public. Sets `publishedAt`. Publishing again on an
already-public Roadmap only moves `publishedAt` forward.
_Avoid_: deploy, release, push, go live, ship

**Published / Private**:
The two states of a Roadmap. Published means `publishedAt` is set and the public
URL resolves; Private means it is null and the public URL 404s. There is no
third state and no staging area — edits to a Published Roadmap are visible to
Readers immediately.
_Avoid_: draft, live, staged, unpublished

**Public page**:
The anonymous, unauthenticated page at `/r/{publicId}` that renders a Published
Roadmap, with Internal Items removed at serve time.
_Avoid_: public roadmap, shared link, landing page, microsite

**Reader**:
Anyone who opens a Public page. Never authenticated. May choose to leave an
email address to subscribe to a Roadmap; otherwise never identified. Cannot
write anything else.
_Avoid_: customer, viewer, visitor, end user, subscriber, stakeholder

**Subscription**:
A Reader's email address on one Published Roadmap. Pending until the Reader
confirms it from the email they receive, then confirmed. Ends when the Reader
unsubscribes or the Owner removes it; the address is then deleted.
(`SPEC-reader-updates.md`)
_Avoid_: follow, membership, mailing list, newsletter

**Subscribed Reader**:
A Reader with a confirmed Subscription. Only subscribed Readers are emailed,
once per Item that becomes Shipped.
_Avoid_: subscriber, follower, contact, lead, audience

**SMTP settings**:
The Owner's mail server, set once in Settings, used to email subscribed
Readers from the Owner's own address.
_Avoid_: mail provider, email integration

**Shipped date** (`shippedAt`):
The instant an Item's Status last became Shipped. Set by the app when the Owner
changes the Status to Shipped, cleared when it leaves Shipped. A record of the
Owner's assertion, never inferred from the Item's dates; the Owner does not
edit it directly. (`SPEC-changelog.md`)
_Avoid_: release date, delivery date, done date

**Recently shipped**:
The last 3 non-Internal Items of a Published Roadmap that have a Shipped date,
newest first, shown on its Public page.
_Avoid_: changelog (in the UI), release notes, what's new

**Feed**:
The RSS 2.0 document of a Published Roadmap's recently Shipped Items, for feed
readers. Anonymous, like the Public page.
_Avoid_: newsletter, stream

**Brand**:
The Owner's product name as Readers see it: on every Public page, in its page
title and link preview, and in the Feed. Set once per Owner in Settings;
optional, never filled from the Owner's Google profile. Shown with a letter
mark (its first letter on an ink tile). Unrelated to the app's own placeholder
name "Product name". (`SPEC-branding.md`)
_Avoid_: company, organisation, workspace, account, theme, logo (until uploads
exist)

## Functional requirements

### Owner
- Signs in with Google; first sign-in creates the Owner. An Owner is
  identified by provider (`google`) + provider subject id (OIDC `sub`); the
  provider column keeps the door open for more providers later.
- Sees only their own Roadmaps and Categories; any access to another Owner's
  resource returns 404.

### Categories
- Owner creates, renames, recolours and deletes Categories, from the
  Categories screen or inline from the editor.
- Colour is one key of a fixed palette: `violet`, `emerald`, `orange`, `pink`, `sky`, `amber`, `lime`, `slate`. A new Category
  is offered the next palette colour in turn.
- Category names are trimmed and unique per Owner, compared
  case-insensitively ("Security" and "security" clash).
- The Categories screen shows how many Items use each Category across all
  Roadmaps.
- Deleting a Category clears it from every Item that used it (Items are kept).

### Roadmaps
- Owner creates, renames and deletes Roadmaps. Deleting a Roadmap deletes its
  Items.
- Each Roadmap gets a random, unguessable `publicId` at creation; it never
  changes, so a Roadmap that is unpublished then republished keeps its URL.
- New Roadmaps are Private.
- "New Roadmap" immediately creates "Untitled Roadmap" and opens it in the
  editor with its name in inline-rename mode. Name: required, ≤ 120
  characters.
- The Roadmaps screen shows, per Roadmap: name, Item count (and Internal
  count), and Published/Private with "Published N days ago".

### Items
- Owner creates, edits, deletes Items on a Roadmap. Items are edited in a
  dialog centred on the screen.
- Deleting an Item shows an "Undo" toast for 5 seconds; the delete is only
  sent to the backend once that window ends (client-side delay).
- Title required, ≤ 140 characters. Description is plain text (line breaks
  kept). Start and end dates are each
  optional and independent (none, start only, end only, or both); when both
  are set, end ≥ start.
- New Items default to Status Planned, no Category, not Internal, and are
  appended at the end of the manual order.
- Owner reorders Items by drag and drop, or with the arrow keys on the drag
  handle; the order drives the list view only.
- Editor offers timeline and list views. Both can be grouped into swimlanes
  by Category (plus a "No Category" lane); swimlanes change the layout,
  never the manual order.
- The editor timeline lists undated Items in a separate "No dates" section
  and shows a "Today" line in the current year.

### Settings
- `/settings` (editor layout, "Settings" in the app bar nav): the Owner sets
  their Brand ("Product name", trimmed, ≤ 60 characters, empty clears it) and
  sees a preview of the Public page header and link preview. No file upload.
  API: `GET` / `PUT /api/brand`.
- "Email to Readers": the Owner's SMTP settings (password write-only and
  encrypted), "Send test email", and a state. Only SMTP ports and public hosts
  are accepted. Until a test succeeds, Public pages offer RSS only.
- Per Roadmap, `/roadmaps/{id}/readers` lists confirmed and pending Readers
  with their dates and the confirmed / pending counts; the Owner
  can remove a Reader. Details in `SPEC-reader-updates.md`.

### Publishing
- Publish sets `publishedAt = now`; Unpublish sets it to null.
- `/r/{publicId}` on a Private Roadmap returns 404, indistinguishable from an
  unknown id.

### Home page
- `/` presents the product (layout and style: `design/ui-design.html`, Landing
  page) and is where an anonymous Owner signs in, with a single
  "Continue with Google" button.
- Copy uses the Language terms (Owner, Roadmap, Item, Reader, Publish,
  Internal Item, Public page) — never "customers" or "users".
- The only date it mentions is the published date; no "last updated" stamp.
- A signed-in Owner opening `/` goes to their Roadmaps.

### Public page
- Shows the Roadmap name, its published date, and its non-Internal Items;
  with a Brand, its letter mark and name above the Roadmap name.
- Page title "{Roadmap name} · {Brand}" (Roadmap name alone without a Brand),
  and link preview tags `og:title`, `og:description`, `og:type`,
  `twitter:card` (summary, no image).
- Public API payload (`GET /api/public/roadmaps/{publicId}`): Roadmap name,
  `publishedAt`, the Owner's Brand name (`brandName`, or null), whether Readers
  can subscribe by email (`subscriptionsOpen`), the Categories used by its non-Internal Items (id, name,
  colour), and its non-Internal Items in manual order (id, title,
  description, start date, end date, status, category id, Shipped date).
  Nothing else — no Owner data other than its Brand, no `internal` flag, no
  Internal Items.
- Recently shipped section between the header and the filters (up to 3 Items
  with their Shipped date; hidden when none; ignores the filters), next to a
  "Follow what ships" RSS link.
- Feed: `GET /api/public/roadmaps/{publicId}/feed.xml`, RSS 2.0, up to 20
  non-Internal Items with a Shipped date, newest first; Private or unknown →
  the same 404 as the Public page; linked from the Public page head.
  Details in `SPEC-changelog.md`.
- With working SMTP settings, a subscribe box (email, double opt-in; it says
  the Brand receives the address) replaces the RSS-only box. Subscribed Readers
  get one email per Item that becomes Shipped, with one-click unsubscribe.
  Confirm and unsubscribe pages at `/r/{publicId}/confirm` and
  `/r/{publicId}/unsubscribe`. Details in `SPEC-reader-updates.md`.
- Timeline view spans one calendar year, by months, opening on the current
  year; the Reader can navigate to previous and next years. Items with both
  dates are drawn as bars (clipped to the year shown); Items with a single
  date as a point; Items with no date are not on the timeline.
- The timeline shows a "Today" line in the current year; clicking an Item
  row expands its description. A note below says how many Items have no
  dates or fall outside the year shown, with a link to the List view.
- List view: all non-Internal Items in manual order, with their
  descriptions.
- Filters by Category (only Categories in use) and by Status; no swimlanes.
- Internal Items are never sent to the browser (filtered by the backend, not
  the frontend).

## Tech Stack

- Frontend: TanStack Start (React 19, SSR), TanStack Router (file-based),
  TanStack Query, Tailwind v4, shadcn/ui. npm.
  - UI components come from shadcn/ui only (no other component library
    such as PrimeReact or MUI), restyled to the tokens of
    `design/ui-design.html`. Outside shadcn: `@dnd-kit` for drag and drop
    and a custom year timeline component.
- Backend: Spring Boot 4 (Web MVC, Data JPA), Java 27, MariaDB, Lombok.
  Spring Security OAuth2 client for Google (to be added) — Spring owns
  the whole login flow and the session; the frontend holds no tokens.
  Liquibase for schema migrations. Local MariaDB via
  `docker compose up -d` (root `docker-compose.yml`).
  `spring-boot-starter-mail` sends emails to subscribed Readers through the
  Owner's SMTP server; the SMTP password is encrypted under the
  `SMTP_SECRET_KEY` environment variable. Mailpit (in `docker-compose.yml`)
  catches emails locally.
- Frontend calls the backend REST API via TanStack Query, always same-origin
  with relative URLs (`/api/...`). A proxy routes `/api/**`, `/oauth2/**` and
  `/login/**` to Spring and everything else to TanStack Start:
  - Dev: Vite `server.proxy` from port 3000 to Spring on 8045; the browser
    never sees 8045.
  - Prod: a reverse proxy does the same on a single domain.
  - No CORS. The Spring session cookie is first-party.
  - The OAuth redirect URI registered at Google uses the public origin
    (`http://localhost:3000/login/oauth2/code/google` in dev), and
    Spring sets `server.forward-headers-strategy=framework`.
  - SSR loaders that call authenticated endpoints must forward the incoming
    request's cookie to Spring.

## Commands

Frontend (`frontend/`):
```
Dev:    npm run dev            # port 3000
Build:  npm run build
Routes: npm run generate-routes
```

Backend (`backend/`):
```
Run:         ./mvnw spring-boot:run
Test:        ./mvnw test
Single test: ./mvnw test -Dtest=ClassName#method
Package:     ./mvnw package
```

## Project Structure

```
frontend/src/routes/        → File-based routes (editor + /r/$publicId)
frontend/src/components/ui  → shadcn components
frontend/src/lib            → Shared utilities, API client
backend/src/main/java/fr/adriencaubel/roadmap/  → Spring app
backend/src/test/java/...   → JUnit tests
```

## Code Style

- Domain names come from **Language** above, in English, in every layer
  (`Roadmap`, `Item`, `Category`, `Owner`, `publishedAt`, `publicId`).
- Status values: `PLANNED`, `IN_PROGRESS`, `SHIPPED` (backend enum) /
  `planned`, `in_progress`, `shipped` (JSON).
- Authenticated API under `/api/...`; anonymous API under `/api/public/...`
  and returns only Reader-safe data.

## Testing Strategy

- Backend: plain JUnit unit tests for domain rules only (date validation,
  Item defaults and position, publish/unpublish). No Spring context, no
  database, no integration or security tests for the MVP.
- Ownership (404) and the Public API (no Internal Items, Private → 404) are
  verified manually against the running app.
- Frontend: no automated tests for the MVP; verified manually in the browser.

## Boundaries

- Always: use the Language terms; filter Internal Items and Private Roadmaps
  on the backend; scope every authenticated query by the current Owner.
- Ask first: adding dependencies, editing an already-applied Liquibase
  changeset (add a new one instead), changing the public URL format.
- Never: expose Owner identity or Internal Items on the Public page; infer
  Status from dates; introduce a draft/staging state; email pending Readers,
  or about Internal Items or Private Roadmaps; return the SMTP password from
  the API; show Subscriptions to Readers.

## Success Criteria

1. An Owner can sign in with Google from the Home page.
2. An Owner can create a Roadmap, add Items (with and without dates), assign
   Categories and Statuses, reorder Items, and see the change persisted.
3. Categories created once are selectable on every Roadmap of that Owner.
4. After Publish, an anonymous browser can open `/r/{publicId}` and sees all
   non-Internal Items in timeline and list views, filterable by Category and
   Status.
5. The public API response for a Published Roadmap contains no Internal Item.
6. After Unpublish, `/r/{publicId}` returns 404; after republish, the same URL
   works again.
7. Owner A gets 404 on any of Owner B's Roadmaps, Items or Categories.

## Deferred (post-MVP)

- Other sign-in providers (GitHub, …) and account linking across them.
- Frontend automated tests (Vitest / Playwright).
- Backend integration and security tests (ownership, Public API leaks).
- Product name and domain: the UI uses the placeholders "Product name" and
  `example.com` until they are chosen.
