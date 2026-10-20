import { useState } from 'react'
import { Link, createFileRoute, notFound } from '@tanstack/react-router'
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'

import { NeutralChip } from '#/components/chips'
import { ConfirmDialog } from '#/components/confirm-dialog'
import { NotFound } from '#/components/not-found'
import { Button } from '#/components/ui/button'
import { ApiError } from '#/lib/api'
import { formatInstant } from '#/lib/format'
import { roadmapQuery } from '#/lib/roadmaps'
import { readersQuery, removeReader, type SubscribedReader } from '#/lib/subscriptions'

export const Route = createFileRoute('/_app/roadmaps/$roadmapId_/readers')({
  params: {
    parse: (params) => ({ roadmapId: Number(params.roadmapId) }),
    stringify: (params) => ({ roadmapId: String(params.roadmapId) }),
  },
  loader: async ({ context: { queryClient }, params: { roadmapId } }) => {
    try {
      await Promise.all([
        queryClient.ensureQueryData(roadmapQuery(roadmapId)),
        queryClient.ensureQueryData(readersQuery(roadmapId)),
      ])
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw notFound()
      throw error
    }
  },
  notFoundComponent: NotFound,
  component: ReadersPage,
})

function ReadersPage() {
  const { roadmapId } = Route.useParams()
  const { data: roadmap } = useSuspenseQuery(roadmapQuery(roadmapId))
  const { data: readers } = useSuspenseQuery(readersQuery(roadmapId))
  const queryClient = useQueryClient()
  const [removing, setRemoving] = useState<SubscribedReader | null>(null)

  const remove = useMutation({
    mutationFn: (reader: SubscribedReader) => removeReader(roadmapId, reader.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: readersQuery(roadmapId).queryKey })
      toast('Reader removed')
    },
    onError: (e) => toast(e.message),
  })

  const onItems = readers.counts.confirmedOnItems
  const counts = [
    {
      label: 'Subscribed',
      value: readers.counts.confirmed,
      split: onItems > 0 ? `${readers.counts.confirmed - onItems} on the Roadmap, ${onItems} on Items` : null,
    },
    { label: 'Waiting to confirm', value: readers.counts.pending, split: null },
  ]

  return (
    <main className="mx-auto max-w-[860px] px-4 pb-30 sm:px-8">
      <div className="mt-6">
        <Button variant="ghost" size="sm" asChild className="-ml-2.5">
          <Link to="/roadmaps/$roadmapId" params={{ roadmapId }}>
            <ArrowLeft aria-hidden />
            {roadmap.name}
          </Link>
        </Button>
      </div>
      <div className="mt-2.5 mb-6">
        <h1 className="font-display text-[28px] leading-tight font-semibold tracking-[-0.5px]">Subscribed Readers</h1>
        <p className="mt-1.5 text-body">
          Readers who get an email when an Item of this Roadmap becomes Shipped, or only when the one Item they
          asked about does.
        </p>
      </div>

      <dl className="mb-8 grid grid-cols-2 gap-3">
        {counts.map(({ label, value, split }) => (
          <div key={label} className="rounded-lg bg-surface-card p-4">
            <dt className="text-[13px] font-medium text-muted-foreground">{label}</dt>
            <dd className="mt-1 font-display text-[28px] font-semibold tabular-nums">{value}</dd>
            {split && <dd className="text-[13px] text-muted-foreground tabular-nums">{split}</dd>}
          </div>
        ))}
      </dl>

      {readers.subscriptions.length ? (
        <ul className="border-t border-hairline-soft">
          {readers.subscriptions.map((reader) => (
            <li
              key={reader.id}
              className="grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-4 border-b border-hairline-soft px-1 py-3"
            >
              <span className="flex min-w-0 flex-col">
                <span className="truncate font-medium">{reader.email}</span>
                <span className="truncate text-[13px] text-muted-foreground">
                  For {reader.itemTitle ? <span className="text-body">{reader.itemTitle}</span> : 'the whole Roadmap'}
                </span>
              </span>
              <span className="flex items-center gap-3">
                {reader.state === 'confirmed' ? (
                  <NeutralChip className="bg-[#ecfdf5] text-success-text">Subscribed</NeutralChip>
                ) : (
                  <NeutralChip>Waiting to confirm</NeutralChip>
                )}
                <span className="text-[13px] text-muted-foreground tabular-nums" suppressHydrationWarning>
                  {formatInstant(reader.confirmedAt ?? reader.createdAt)}
                </span>
              </span>
              <Button variant="ghost" size="sm" onClick={() => setRemoving(reader)}>
                Remove
              </Button>
            </li>
          ))}
        </ul>
      ) : (
        <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[#d1d5db] px-6 py-14 text-center">
          <h2 className="font-display text-[22px] font-semibold tracking-[-0.3px]">No Readers have subscribed yet</h2>
          <p className="max-w-[420px] text-body">
            Readers subscribe from the Public page once your SMTP server works (Settings, Email to Readers).
          </p>
        </div>
      )}

      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(open) => !open && setRemoving(null)}
        title={`Remove ${removing?.email}?`}
        body={`${removing?.email} won't get emails about ${removing?.itemTitle ?? 'this Roadmap'} anymore.`}
        cta="Remove Reader"
        onConfirm={() => removing && remove.mutate(removing)}
      />
    </main>
  )
}
