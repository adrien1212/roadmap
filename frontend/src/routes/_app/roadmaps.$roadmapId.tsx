import { useState } from 'react'
import { Link, createFileRoute, notFound, useNavigate } from '@tanstack/react-router'
import { useQuery, useSuspenseQuery } from '@tanstack/react-query'
import { ArrowLeft, CircleAlert, ExternalLink, Globe, Lock, Plus, Rows2 } from 'lucide-react'

import { StateBadge } from '#/components/chips'
import { ConfirmDialog } from '#/components/confirm-dialog'
import { CategoryLegend } from '#/components/editor/category-legend'
import { ItemDialog } from '#/components/editor/item-dialog'
import { ItemList } from '#/components/editor/item-list'
import { ItemTimeline } from '#/components/editor/item-timeline'
import { PublishDialog } from '#/components/editor/publish-dialog'
import { useDelayedDelete } from '#/components/editor/use-delayed-delete'
import { useReorder } from '#/components/editor/use-reorder'
import { NotFound } from '#/components/not-found'
import { RoadmapTitle } from '#/components/roadmap-title'
import { Button } from '#/components/ui/button'
import { ViewToggle, type View } from '#/components/view-toggle'
import { YearNav } from '#/components/year-timeline'
import { ApiError } from '#/lib/api'
import { categoriesQuery } from '#/lib/categories'
import { daysAgo, plural } from '#/lib/format'
import { itemsQuery, type Item } from '#/lib/items'
import { publicPath, roadmapQuery } from '#/lib/roadmaps'
import { smtpSettingsQuery } from '#/lib/smtp'
import { readersQuery, waitingReaders } from '#/lib/subscriptions'

export const Route = createFileRoute('/_app/roadmaps/$roadmapId')({
  params: {
    parse: (params) => ({ roadmapId: Number(params.roadmapId) }),
    stringify: (params) => ({ roadmapId: String(params.roadmapId) }),
  },
  /** `?rename=true` opens the name in inline-rename mode (right after "New Roadmap"). */
  validateSearch: (search: Record<string, unknown>): { rename?: boolean } =>
    search.rename === true || search.rename === 'true' ? { rename: true } : {},
  loader: async ({ context: { queryClient }, params: { roadmapId } }) => {
    try {
      await Promise.all([
        queryClient.ensureQueryData(roadmapQuery(roadmapId)),
        queryClient.ensureQueryData(itemsQuery(roadmapId)),
        queryClient.ensureQueryData(categoriesQuery),
        // Email state (Item dialog hint, SMTP warning): prefetched, but never blocks the editor.
        queryClient.prefetchQuery(smtpSettingsQuery),
        queryClient.prefetchQuery(readersQuery(roadmapId)),
      ])
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw notFound()
      throw error
    }
  },
  notFoundComponent: NotFound,
  component: EditorPage,
})

type EditingState = { item: Item | null } | null

function EditorPage() {
  const { roadmapId } = Route.useParams()
  const { rename } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data: roadmap } = useSuspenseQuery(roadmapQuery(roadmapId))
  const { data: allItems } = useSuspenseQuery(itemsQuery(roadmapId))
  const { data: categories } = useSuspenseQuery(categoriesQuery)

  const [renaming, setRenaming] = useState(rename === true)
  const [view, setView] = useState<View>('timeline')
  const [year, setYear] = useState(() => new Date().getFullYear())
  const [lanes, setLanes] = useState(false)
  const [editing, setEditing] = useState<EditingState>(null)
  const [confirmingDelete, setConfirmingDelete] = useState<Item | null>(null)
  const [publishing, setPublishing] = useState(false)
  const { hidden, remove } = useDelayedDelete(roadmapId)
  const reorder = useReorder(roadmapId)
  // Email state, for the Item dialog hint and the warning; not needed to render the editor.
  const { data: smtp } = useQuery(smtpSettingsQuery)
  const { data: readers } = useQuery({ ...readersQuery(roadmapId), enabled: roadmap.publishedAt !== null })
  // Roadmap Subscriptions get every Shipped email; Item Subscriptions only their Item's (an address on both gets two).
  const subscribed = readers?.counts.confirmed ?? 0
  const onRoadmap = subscribed - (readers?.counts.confirmedOnItems ?? 0)
  const waiting = editing?.item ? waitingReaders(readers, editing.item.id) : 0
  const emailsOnShip = roadmap.publishedAt && smtp?.state === 'connected' ? onRoadmap + waiting : 0

  // Items waiting out their Undo window are already gone from the Owner's view.
  const items = allItems.filter((item) => !hidden.has(item.id))
  const internalCount = items.filter((item) => item.internal).length

  function onRenamingChange(editing: boolean) {
    setRenaming(editing)
    if (!editing && rename) navigate({ search: {}, replace: true })
  }

  const newItem = () => setEditing({ item: null })

  return (
    <main className="mx-auto max-w-[1200px] px-4 pb-35 sm:px-8">
      <div className="mt-6">
        <Button variant="ghost" size="sm" asChild className="-ml-2.5">
          <Link to="/roadmaps" activeOptions={{ exact: true }}>
            <ArrowLeft aria-hidden />
            Roadmaps
          </Link>
        </Button>
      </div>

      <div className="mt-2.5 mb-1 flex flex-wrap items-center justify-between gap-4">
        <div className="flex min-w-[260px] flex-1 items-center gap-1.5">
          <RoadmapTitle roadmap={roadmap} editing={renaming} onEditingChange={onRenamingChange} />
        </div>
        <div className="flex flex-wrap items-center gap-3">
          {roadmap.publishedAt ? (
            <StateBadge published label={`Published ${daysAgo(roadmap.publishedAt)}`} />
          ) : (
            <span className="inline-flex items-center gap-1.5 text-[13px] font-medium text-muted-foreground">
              <Lock className="size-4" aria-hidden />
              Private
            </span>
          )}
          {roadmap.publishedAt && (
            <Button variant="secondary" asChild>
              <a href={publicPath(roadmap)} target="_blank" rel="noreferrer">
                <ExternalLink aria-hidden />
                View Public page
              </a>
            </Button>
          )}
          <Button variant={roadmap.publishedAt ? 'secondary' : 'default'} onClick={() => setPublishing(true)}>
            {roadmap.publishedAt ? (
              <>
                <Globe aria-hidden />
                Share
              </>
            ) : (
              'Publish'
            )}
          </Button>
        </div>
      </div>
      <p className="text-sm text-muted-foreground tabular-nums">
        {plural(items.length, 'Item')}
        {internalCount > 0 && ` · ${internalCount} Internal, left off the Public page`}
      </p>

      {smtp?.state === 'not_working' && subscribed > 0 && (
        <div role="alert" className="mt-4 flex items-start gap-2 rounded-md border border-[#fecaca] bg-[#fef2f2] px-4 py-3 text-sm text-error-text">
          <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
          <span>
            Emails to subscribed Readers aren't being sent: {smtp.lastError}{' '}
            <Link to="/settings" className="font-semibold underline">
              Fix it in Settings
            </Link>
          </span>
        </div>
      )}

      <CategoryLegend categories={categories} items={items} />

      <div className="sticky top-0 z-10 flex flex-wrap items-center gap-3 border-b border-hairline-soft bg-background/95 py-3.5">
        <ViewToggle view={view} onChange={setView} />
        <button
          aria-pressed={lanes}
          onClick={() => setLanes(!lanes)}
          className="inline-flex h-8 cursor-pointer items-center gap-[7px] rounded-full border border-border bg-background px-3.5 text-[13px] font-medium text-body transition-colors hover:border-[#d1d5db] hover:text-foreground aria-pressed:border-foreground aria-pressed:bg-foreground aria-pressed:font-semibold aria-pressed:text-white"
        >
          <Rows2 className="size-4" aria-hidden />
          Swimlanes by Category
        </button>
        <span className="flex-1" />
        {view === 'timeline' && <YearNav year={year} onChange={setYear} />}
        <Button onClick={newItem}>
          <Plus aria-hidden />
          Item
        </Button>
      </div>

      {items.length ? (
        view === 'timeline' ? (
          <ItemTimeline
            items={items}
            categories={categories}
            lanes={lanes}
            year={year}
            onEdit={(item) => setEditing({ item })}
          />
        ) : (
          <ItemList
            items={items}
            categories={categories}
            lanes={lanes}
            onEdit={(item) => setEditing({ item })}
            onDelete={setConfirmingDelete}
            onMove={(activeId, overId) => reorder(allItems, activeId, overId)}
          />
        )
      ) : (
        <div className="mt-6 flex flex-col items-center gap-3 rounded-lg border border-dashed border-[#d1d5db] px-6 py-14 text-center">
          <h2 className="font-display text-[22px] font-semibold tracking-[-0.3px]">This Roadmap has no Items yet</h2>
          <p className="max-w-[420px] text-body">
            Add the first thing you're working on. Dates are optional, and you can set the Status by hand at any time.
          </p>
          <Button onClick={newItem}>
            <Plus aria-hidden />
            Add Item
          </Button>
        </div>
      )}

      <ItemDialog
        roadmapId={roadmapId}
        item={editing?.item ?? null}
        open={editing !== null}
        categories={categories}
        onOpenChange={(open) => !open && setEditing(null)}
        onDelete={setConfirmingDelete}
        emailsOnShip={emailsOnShip}
        waitingReaders={waiting}
        publicId={roadmap.publishedAt ? roadmap.publicId : null}
      />

      <PublishDialog roadmap={roadmap} internalCount={internalCount} open={publishing} onOpenChange={setPublishing} />

      <ConfirmDialog
        open={confirmingDelete !== null}
        onOpenChange={(open) => !open && setConfirmingDelete(null)}
        title={`Delete "${confirmingDelete?.title}"?`}
        body={`It's removed from this Roadmap${
          roadmap.publishedAt && !confirmingDelete?.internal ? ' and from its Public page' : ''
        }.`}
        cta="Delete Item"
        onConfirm={() => {
          if (!confirmingDelete) return
          setEditing(null)
          remove(confirmingDelete)
        }}
      />
    </main>
  )
}
