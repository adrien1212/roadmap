import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'
import type { Category } from '#/lib/categories'
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
  endDate: string | null
  status: ItemStatus
  categoryId: number | null
  /** When it became Shipped; null when not Shipped or shipped before Shipped dates existed. */
  shippedAt: string | null
}

/** The Feed (RSS 2.0) of a Published Roadmap. */
export const feedPath = (publicId: string) => `/api/public/roadmaps/${encodeURIComponent(publicId)}/feed.xml`

/** Always answers the same, whether the address is new or already subscribed. */
export const subscribe = (publicId: string, email: string) =>
  api.post<{ detail: string }>(`/api/public/roadmaps/${encodeURIComponent(publicId)}/subscriptions`, { email })

export const confirmSubscription = (token: string) => api.post<void>('/api/public/subscriptions/confirm', { token })

export const publicPageQuery = (publicId: string) =>
  queryOptions({
    queryKey: ['public', publicId],
    queryFn: () => api.get<PublicPage>(`/api/public/roadmaps/${encodeURIComponent(publicId)}`),
  })
