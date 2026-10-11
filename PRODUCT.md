# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Owner**: one person on a product team, signed in with Google or GitHub. Keeps one or more dated Roadmaps current and decides when each one is Published. Works at a desk, in the editor, in short sessions: adding an Item, moving a date, flipping a Status, reordering.
- **Reader**: anyone who opens a Public page, usually someone who buys from the Owner and wants to know what's coming and when. Anonymous, often arriving from a shared link, possibly on a phone. Reads only and can't write anything.

## Product Purpose

Lets an Owner keep dated Roadmaps and press Publish to expose each one at a public, unguessable URL. The product exists to keep the two sides apart: the Owner who authors, and the anonymous Readers who only read. Success means a Reader can open a link and trust what they see, and the Owner can keep it current without keeping a second document.

## Positioning

One Roadmap, two faces. There is no draft copy: edits to a Published Roadmap show up for Readers immediately, Internal Items are withheld at serve time (never sent to the browser), and Status is something the Owner asserts by hand, never inferred from dates.

## Operating Context

- Editor: desktop browser. Timeline and list views, optional swimlanes by Category, drag-and-drop ordering, an Item form.
- Public page: `/r/{publicId}`, any device. Timeline spanning one calendar year by months (opening on the current year, with previous and next years), a list view in manual order, and filters by Category and Status.
- Sign-in is Google or GitHub via OAuth; first sign-in creates the Owner.

## Capabilities and Constraints

- SPEC.md is the source of truth for scope and the binding Language. UI copy uses its terms and avoids its _Avoid_ lists (for example: never "customer", "user", "hidden", "draft", "live", "feature", "tag").
- Item: title (required), plain-text description (no Markdown), optional start and end dates that are independent (none, start only, end only, or both; end ≥ start), Status (Planned / In progress / Shipped), at most one Category, Internal flag, manual position.
- Category: name and colour, unique per Owner, shared across all their Roadmaps. Deleting one clears it from Items.
- Roadmap: Published (`publishedAt` set) or Private (null). No third state. A Private Roadmap's public URL returns a 404 that looks the same as an unknown id.
- Out of scope for the MVP: workspaces, teammates, invitations, roles, billing, custom domains, embeds, Reader subscriptions or feedback, progress percentages.
- Undecided: the product name. Use a neutral placeholder wordmark until one is chosen.

## Brand Commitments

The visual language of the Claude Design prototype (`design/ui-design.html`, a Cal.com-derived kit: Cal Sans display, Inter body, black primary, light gray cards, pill badges) is kept. The user looked at alternative visual worlds on 2026-10-10, rejected them, and chose to polish this look instead.

## Evidence on Hand

No real customers, testimonials, metrics or pricing exist. Any example Roadmap content in designs is synthetic and must read as an example.

## Product Principles

1. The Reader's trust comes first: what's shown is current, dated and never inferred.
2. Two sides, kept apart: authoring affordances never leak onto the Public page, and the Owner's identity is never exposed there.
3. Status is a claim, dates are a plan; the UI never conflates them.
4. Publishing is a switch, not a workflow: no drafts, no staging.
