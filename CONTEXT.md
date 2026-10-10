# Roadmap

A SaaS where a product team maintains a dated timeline and presses publish to
expose it at a public URL for the people who buy from *them*. The vocabulary
below exists mostly to keep those two sides apart: the team that authors a
roadmap, and the anonymous people who read it.

## Language

### Tenancy

**Workspace**:
The account boundary. Owns Roadmaps and Members, and is the first segment of
every URL.
_Avoid_: organization, org, team, account, tenant

**Member**:
A person with access to a Workspace. There is one flat role — every Member can
do everything.
_Avoid_: user, collaborator, seat, editor, admin

**Customer**:
A team that pays for a Workspace. Never the person reading a Public page.
_Avoid_: client, account, tenant

### Content

**Roadmap**:
A named, publishable timeline belonging to a Workspace. Holds one set of Items,
and is either Published or Private.
_Avoid_: board, plan, project, timeline

**Item**:
One dated thing on a Roadmap — the unit a Reader sees as a single row.
_Avoid_: feature, card, arrow, initiative, epic, ticket

**Category**:
An optional single label on an Item, used as a colour chip and a filter control.
An Item has at most one. The editor may group Items into swimlanes by Category;
a Public page never does. A lane is a layout, never a name for the Category
itself.
_Avoid_: tag, label, track, group

**Status**:
Where an Item stands — Planned, In progress, or Shipped — as asserted by a
Member. Never inferred from the Item's dates, which describe a plan rather than
a claim.
_Avoid_: state, stage, phase, progress

**Internal Item**:
An Item that Members see in the editor but that is removed from the Public page.
It is not "unpublished" — the Roadmap around it is Published, the Item is
withheld.
_Avoid_: private, hidden, secret, unpublished, internal-only

### Publishing

**Publish**:
The act of making a Roadmap public. Sets `publishedAt`; a Roadmap with a
`publishedAt` is served at its public URL, one with `publishedAt` null is not.
Publishing again on an already-public Roadmap only moves `publishedAt` forward.
_Avoid_: deploy, release, push, go live, ship

**Published / Private**:
The two states of a Roadmap. Published means `publishedAt` is set and the public
URL resolves; Private means it is null and the public URL 404s. There is no
third state and no staging area — a Roadmap has one set of Items, and edits to a
Published Roadmap are visible to Readers immediately.
_Avoid_: draft, live, staged, unpublished

**Public page**:
The anonymous, unauthenticated page that renders a Published Roadmap, with
Internal Items removed at serve time.
_Avoid_: public roadmap, shared link, landing page, microsite

**Reader**:
Anyone who opens a Public page. Never authenticated, never identified, and
cannot write anything.
_Avoid_: customer, viewer, visitor, end user, subscriber, stakeholder
