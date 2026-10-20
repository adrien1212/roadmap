import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'

export interface Readers {
  /** confirmedOnItems: how many of the confirmed wait for one Item rather than follow the Roadmap. */
  counts: { confirmed: number; pending: number; confirmedOnItems: number }
  /** Confirmed and pending, newest first. */
  subscriptions: SubscribedReader[]
}

export interface SubscribedReader {
  id: number
  email: string
  state: 'pending' | 'confirmed'
  createdAt: string
  confirmedAt: string | null
  /** The one Item of an Item Subscription; null when the Reader follows the whole Roadmap. */
  itemId: number | null
  itemTitle: string | null
}

/** Waiting Readers: confirmed Item Subscriptions of one Item. */
export const waitingReaders = (readers: Readers | undefined, itemId: number) =>
  readers?.subscriptions.filter((s) => s.state === 'confirmed' && s.itemId === itemId).length ?? 0

export const readersQuery = (roadmapId: number) =>
  queryOptions({
    queryKey: ['roadmaps', roadmapId, 'subscriptions'],
    queryFn: () => api.get<Readers>(`/api/roadmaps/${roadmapId}/subscriptions`),
  })

/** Counts as unsubscribed. */
export const removeReader = (roadmapId: number, subscriptionId: number) =>
  api.delete(`/api/roadmaps/${roadmapId}/subscriptions/${subscriptionId}`)
