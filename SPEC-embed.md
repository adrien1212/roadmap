# Spec: embed

> Module `embed` of `ROADMAP.md`. `SPEC.md` stays the source of truth for
> everything not changed here; the changes listed under "Changes to SPEC.md"
> have been applied to it.

## Objective

Let the Owner show a Published Roadmap inside their own site or app with an
`<iframe>`, without sending Readers away to the Public page.

## Requirements

- `/r/{publicId}/embed` renders the Items of a Published Roadmap only:
  timeline and list views, year navigation, rows that expand to their
  description, Status and Category chips on each Item.
- Left out (they stay on the Public page): Brand mark and header, Recently
  shipped, the RSS / subscribe box, the Category and Status filters, the footer.
- The Roadmap name, top left, links to the Public page in a new tab, so a
  Reader can still reach filters, the Feed and the subscribe box.
- Same data as the Public page: it calls
  `GET /api/public/roadmaps/{publicId}`; no new endpoint. Internal Items never
  reach the browser; a Private or unknown `publicId` gives the same 404.
- `robots: noindex`: the Public page is the one to index.
- Any site may frame it (no `X-Frame-Options` / `frame-ancestors` on it).
- The Publish dialog of a Published Roadmap shows the embed code and "Copy
  embed code":
  `<iframe src="{origin}/r/{publicId}/embed" title="{Roadmap name}" width="100%" height="600" style="border:0" loading="lazy"></iframe>`
- Unpublishing makes the embed 404 like the Public page; republishing brings it
  back at the same URL.

## Changes to SPEC.md (applied)

- Out of scope: embeds removed.
- Public page: the embed and "Copy embed code".

## Not in this module

- Auto-resizing the iframe to its content (needs a script on the host page).
- Choosing which parts to show, or theming the embed.
- Restricting which sites may frame it.

## Testing strategy

Frontend: no automated tests (as in `SPEC.md`). Manual:
- The embed code pasted into a local HTML file shows the Items.
- An Internal Item is absent; a Private Roadmap's embed 404s.
