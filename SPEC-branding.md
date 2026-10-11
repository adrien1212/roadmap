# Spec: branding

> Module `branding` of `ROADMAP.md` (Now release, second module).
> Status: **approved 2026-10-11.** `SPEC.md` stays the source of truth for
> everything not changed here; the changes listed under "Changes to SPEC.md"
> have been applied to it.

## Objective

A Reader who opens a Public page today sees a Roadmap name and nothing that
says whose product it is. Indie and small SaaS teams want their Roadmap to look
like part of their product, and shared links to say what they are.

This module lets the Owner set their **Brand** once, in a new Settings screen,
and shows it on every Public page, in the page title and link preview, and in
the Feed. Emails (`reader-updates`) will use it as the sender name.

**No file upload in this module** (decided 2026-10-11). The Brand is a name; its
mark is a letter tile made from that name. Logo upload is left for later.

## Assumptions

1. The Brand is **optional**. Until it is set, Public pages look as they do
   today. It is never filled in from the Owner's Google name or email, which
   are Owner data and must not reach Readers.
2. One Brand per Owner, shared by all their Roadmaps (decided in `ROADMAP.md`).
3. The letter mark is always the ink tile with a white letter, as in the
   mockup. No colour choice.

## Language (additions)

**Brand**:
The Owner's product name as Readers see it: on every Public page, in its page
title and link preview, in the Feed, and later as the sender of emails. Set once
per Owner in Settings; optional. Shown with a **letter mark**, the first letter
of the name on an ink tile.
_Avoid_: company, organisation, workspace, account, theme, logo (until uploads
exist)

The app's own placeholder name ("Product name", as in "Made with Product name")
is unrelated to the Owner's Brand.

## Requirements

### Settings screen (`/settings`, editor layout)
- A "Settings" entry in the app bar nav, after Categories.
- "Product name" field: trimmed, at most 60 characters; empty clears the Brand.
  Hint: "Shown above every Roadmap name, and as the sender of emails."
- Save shows a "Settings saved" toast.
- Preview beside the form: the Public page header (letter mark, Brand, a
  Roadmap name, its published date) and the link preview (title, origin,
  description), using the Owner's first Published Roadmap, or the first one.
- The note "Public pages always end with "Made with Product name"." stays.
- The "Email to Readers" (SMTP) section shown in the mockup belongs to
  `reader-updates` and is not part of this module.

### API (Owner)
- `GET /api/brand` → `{ "name": string | null }`.
- `PUT /api/brand` with `{ "name": string | null }` → the saved Brand. Name
  trimmed; blank or null clears it; more than 60 characters → 400.
- Scoped by `CurrentOwner`, like every Owner endpoint.

### Public page
- When the Owner has a Brand, the letter mark and Brand name show above the
  Roadmap name.
- `<title>`: "{Roadmap name} · {Brand}", or the Roadmap name alone without a
  Brand.
- Link preview tags: `og:title` (same as the title), `og:description` ("What
  {Brand} has shipped, is working on and plans." / without a Brand: "What has
  shipped, is in progress and is planned."), `og:type` = `website`, and
  `twitter:card` = `summary`. No image.
- The "Made with Product name" footer is unchanged and always shows.

### Public API
- `GET /api/public/roadmaps/{publicId}` adds `brandName` (string or null).
  Still no other Owner data: no Owner name, email, provider or id.

### Feed
- Channel title: "{Roadmap name} · {Brand}", or the Roadmap name alone.

## Changes to SPEC.md (applied)

- Language: add Brand.
- Public API payload: adds `brandName`; "no Owner data" becomes "no Owner data
  other than its Brand".
- Public page: Brand above the name; page title and link preview tags.
- Editor: new Settings screen.

## Tech stack, commands, project structure

Unchanged from `SPEC.md`. No new dependency.

```
backend/src/main/resources/db/changelog/changes/006-owner-brand.sql  → owner.brand_name VARCHAR(60) NULL
backend/.../owner/Owner.java              → brandName + rename rule
backend/.../owner/BrandController.java    → GET/PUT /api/brand
backend/.../publicpage/PublicPage.java    → brandName in the payload
backend/.../publicpage/RssFeed.java       → channel title with the Brand
frontend/src/lib/brand.ts                 → query, save, letter mark
frontend/src/components/brand-mark.tsx    → letter tile + name
frontend/src/routes/_app/settings.tsx     → Settings screen
frontend/src/components/app-bar.tsx       → Settings in the nav
frontend/src/routes/r.$publicId.tsx       → Brand row, title, link preview tags
```

## Code style

As in `SPEC.md`. The Brand rule lives on the entity, like the other name rules:

```java
/** Trimmed, at most 60 characters; blank clears the Brand. */
public void changeBrand(String name) {
    String trimmed = name == null ? "" : name.strip();
    if (trimmed.length() > BRAND_MAX_LENGTH) {
        throw new ValidationException("Product name must be at most " + BRAND_MAX_LENGTH + " characters");
    }
    this.brandName = trimmed.isEmpty() ? null : trimmed;
}
```

## Testing strategy

Backend unit tests (JUnit, no Spring context, written first):
- Brand: trimmed; blank or null clears it; 60 characters accepted, 61 rejected.
- Public mapping: `brandName` mapped; null when unset; the exact-fields test
  updated to the new payload; still no Owner name or email.
- `RssFeed`: channel title with and without a Brand.

Manual, against the running app:
- Set a Brand in Settings: it shows on the Public page, in its `<title>` and
  `og:` tags (curl the SSR HTML) and in the Feed title.
- Clear it: the Public page looks as before.
- The Public payload contains no Owner name, email or provider.

Frontend: no automated tests (as in `SPEC.md`).

## Boundaries

- Always: scope Brand reads and writes by `CurrentOwner`; send Readers the Brand
  only, never other Owner data.
- Ask first: file or image upload; a generated link preview image; letting the
  Owner remove the "Made with Product name" footer.
- Never: fill the Brand from the Owner's Google profile.

## Success criteria

1. An Owner can set, change and clear their Brand in Settings; it persists.
2. With a Brand, every Public page of that Owner shows the letter mark and
   Brand above the Roadmap name, and its title, `og:` tags and Feed title
   include the Brand. Without one, they show the Roadmap name only.
3. The Public payload gains `brandName` and nothing else; no Owner name, email
   or provider is ever sent.
4. `./mvnw test` green with the new unit tests; `npm run build` clean.

## Decided on approval

1. The Brand is **optional**, with no default; publishing never requires it.
2. The letter mark is always the **ink tile**; no colour choice.
