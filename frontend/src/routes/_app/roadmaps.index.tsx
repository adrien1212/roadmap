import { useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'
import { ExternalLink, MoreHorizontal, Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

import { StateBadge } from '#/components/chips'
import { ConfirmDialog } from '#/components/confirm-dialog'
import { Button } from '#/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '#/components/ui/dropdown-menu'
import { daysAgo, plural } from '#/lib/format'
import { createRoadmap, deleteRoadmap, publicPath, roadmapsQuery, type RoadmapSummary } from '#/lib/roadmaps'

export const Route = createFileRoute('/_app/roadmaps/')({
  loader: ({ context }) => context.queryClient.ensureQueryData(roadmapsQuery),
  component: RoadmapsPage,
})

function RoadmapsPage() {
  const { data: roadmaps } = useSuspenseQuery(roadmapsQuery)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [deleting, setDeleting] = useState<RoadmapSummary | null>(null)

  const create = useMutation({
    mutationFn: createRoadmap,
    onSuccess: (roadmap) => {
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] })
      navigate({ to: '/roadmaps/$roadmapId', params: { roadmapId: roadmap.id }, search: { rename: true } })
    },
    onError: (e) => toast(e.message),
  })

  const remove = useMutation({
    mutationFn: (roadmap: RoadmapSummary) => deleteRoadmap(roadmap.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] })
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      toast('Roadmap deleted')
    },
    onError: (e) => toast(e.message),
  })

  const newRoadmap = (
    <Button onClick={() => create.mutate()} disabled={create.isPending}>
      <Plus aria-hidden />
      New Roadmap
    </Button>
  )

  return (
    <main className="mx-auto max-w-[1200px] px-4 pb-30 sm:px-8">
      <div className="mt-12 mb-6 flex flex-wrap items-end justify-between gap-4">
        <h1 className="font-display text-[28px] leading-tight font-semibold tracking-[-0.5px]">Roadmaps</h1>
        {newRoadmap}
      </div>

      {roadmaps.length ? (
        <ul className="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-4">
          {roadmaps.map((roadmap) => (
            <RoadmapCard key={roadmap.id} roadmap={roadmap} onDelete={() => setDeleting(roadmap)} />
          ))}
          <li>
            <button
              onClick={() => create.mutate()}
              disabled={create.isPending}
              className="flex min-h-[168px] w-full cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-[#d1d5db] text-sm font-semibold text-body transition-colors hover:border-foreground hover:bg-surface-soft hover:text-foreground"
            >
              <Plus className="size-4" aria-hidden />
              New Roadmap
            </button>
          </li>
        </ul>
      ) : (
        <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[#d1d5db] px-6 py-14 text-center">
          <h2 className="font-display text-[22px] font-semibold tracking-[-0.3px]">No Roadmaps yet</h2>
          <p className="max-w-[420px] text-body">
            A Roadmap is Private when you create it. Add Items, then publish it when it's ready for Readers.
          </p>
          {newRoadmap}
        </div>
      )}

      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={`Delete ${deleting?.name}?`}
        body={
          deleting
            ? `Its ${plural(deleting.itemCount, 'Item')} ${deleting.itemCount === 1 ? 'is' : 'are'} deleted with it.` +
              (deleting.publishedAt ? ' Its Public page stops working.' : '') +
              " This can't be undone."
            : ''
        }
        cta="Delete Roadmap"
        onConfirm={() => deleting && remove.mutate(deleting)}
      />
    </main>
  )
}

function RoadmapCard({ roadmap, onDelete }: { roadmap: RoadmapSummary; onDelete: () => void }) {
  const params = { roadmapId: roadmap.id }
  return (
    <li className="relative flex min-h-[168px] flex-col gap-[18px] rounded-lg bg-surface-card p-6 transition-colors hover:bg-[#efefef]">
      {/* The whole card opens the editor; the menu sits above this link. */}
      <Link
        to="/roadmaps/$roadmapId"
        params={params}
        aria-label={`Open ${roadmap.name}`}
        className="absolute inset-0 rounded-lg"
      />
      <div className="flex items-start justify-between gap-3">
        <h2 className="font-display text-[22px] leading-[1.3] font-semibold tracking-[-0.3px] break-words">
          {roadmap.name}
        </h2>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon-sm" aria-label={`More for ${roadmap.name}`} className="relative z-10">
              <MoreHorizontal aria-hidden />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-[220px] rounded-lg p-1.5 shadow-overlay">
            <DropdownMenuItem asChild>
              <Link to="/roadmaps/$roadmapId" params={params}>
                <Pencil aria-hidden />
                Open editor
              </Link>
            </DropdownMenuItem>
            {roadmap.publishedAt && (
              <DropdownMenuItem asChild>
                <a href={publicPath(roadmap)} target="_blank" rel="noreferrer">
                  <ExternalLink aria-hidden />
                  View Public page
                </a>
              </DropdownMenuItem>
            )}
            <DropdownMenuItem variant="destructive" onSelect={onDelete}>
              <Trash2 aria-hidden />
              Delete Roadmap
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
      <div className="mt-auto flex items-center justify-between gap-2">
        <span className="text-sm text-muted-foreground tabular-nums">
          {plural(roadmap.itemCount, 'Item')}
          {roadmap.internalItemCount > 0 && ` · ${roadmap.internalItemCount} Internal`}
        </span>
        <StateBadge
          published={roadmap.publishedAt !== null}
          label={roadmap.publishedAt ? `Published ${daysAgo(roadmap.publishedAt)}` : undefined}
        />
      </div>
    </li>
  )
}
