import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'
import type { Category } from '#/lib/categories'
import type { Precision } from '#/lib/dates'
import type { ItemStatus } from '#/lib/items'

/** What a Reader receives (GET /api/public/roadmaps/{publicId}): no Owner data, no Internal Items. */
export interface PublicPage {
  name: string
  publishedAt: string
  /** The Owner's Brand, the only Owner data a Reader receives; null when unset. */
  brandName: string | null
  /** Whether Readers can subscribe by email (Published, and the Owner's SMTP server works). */
  subscriptionsOpen: boolean
  /** Only the Categories used by the Items below. */
  categories: Category[]
  /** Non-Internal Items, in manual order. */
  items: PublicItem[]
}

export interface PublicItem {
  id: number
  title: string
  description: string | null
  startDate: string | null
  startPrecision: Precision
  endDate: string | null
  endPrecision: Precision
  status: ItemStatus
  categoryId: number | null
  /** When it became Shipped; null when not Shipped or shipped before Shipped dates existed. */
  shippedAt: string | null
}

/** The Item page: one non-Internal Item of a Published Roadmap, the link to send about it. */
export const itemPath = (publicId: string, itemId: number) => `/r/${publicId}/items/${itemId}`

/** The Feed (RSS 2.0) of a Published Roadmap. */
export const feedPath = (publicId: string) => `/api/public/roadmaps/${encodeURIComponent(publicId)}/feed.xml`

/** Always answers the same, whether the address is new or already subscribed. */
export const subscribe = (publicId: string, email: string) =>
  api.post<{ detail: string }>(`/api/public/roadmaps/${encodeURIComponent(publicId)}/subscriptions`, { email })

/** "Notify me when this ships" for one Item; the same answer whatever the address. */
export const subscribeToItem = (publicId: string, itemId: number, email: string) =>
  api.post<{ detail: string }>(
    `/api/public/roadmaps/${encodeURIComponent(publicId)}/items/${itemId}/subscriptions`,
    { email },
  )

/** What a Subscription covers: the title of its Item, or null for the whole Roadmap. */
export interface Covered {
  itemTitle: string | null
}

export const confirmSubscription = (token: string) => api.post<Covered>('/api/public/subscriptions/confirm', { token })

export const unsubscribe = (token: string) =>
  api.post<Covered>(`/api/public/subscriptions/unsubscribe?token=${encodeURIComponent(token)}`)

export const publicPageQuery = (publicId: string) =>
  queryOptions({
    queryKey: ['public', publicId],
    queryFn: () => api.get<PublicPage>(`/api/public/roadmaps/${encodeURIComponent(publicId)}`),
  })
