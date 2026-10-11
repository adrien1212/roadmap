import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'

export interface Readers {
  counts: { confirmed: number; pending: number }
  /** Confirmed and pending, newest first. */
  subscriptions: SubscribedReader[]
}

export interface SubscribedReader {
  id: number
  email: string
  state: 'pending' | 'confirmed'
  createdAt: string
  confirmedAt: string | null
}

export const readersQuery = (roadmapId: number) =>
  queryOptions({
    queryKey: ['roadmaps', roadmapId, 'subscriptions'],
    queryFn: () => api.get<Readers>(`/api/roadmaps/${roadmapId}/subscriptions`),
  })

/** Counts as unsubscribed. */
export const removeReader = (roadmapId: number, subscriptionId: number) =>
  api.delete(`/api/roadmaps/${roadmapId}/subscriptions/${subscriptionId}`)
