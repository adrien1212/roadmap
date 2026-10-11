import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'
import type { Category } from '#/lib/categories'
import type { ItemStatus } from '#/lib/items'

/** What a Reader receives (GET /api/public/roadmaps/{publicId}): no Owner data, no Internal Items. */
export interface PublicPage {
  name: string
  publishedAt: string
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
}

export const publicPageQuery = (publicId: string) =>
  queryOptions({
    queryKey: ['public', publicId],
    queryFn: () => api.get<PublicPage>(`/api/public/roadmaps/${encodeURIComponent(publicId)}`),
  })
