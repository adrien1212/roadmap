import { queryOptions, type QueryClient } from '@tanstack/react-query'

import { api } from '#/lib/api'
import type { Category } from '#/lib/categories'

export type ItemStatus = 'planned' | 'in_progress' | 'shipped'

export const STATUS_LABEL: Record<ItemStatus, string> = {
  planned: 'Planned',
  in_progress: 'In progress',
  shipped: 'Shipped',
}

export const STATUSES = Object.keys(STATUS_LABEL) as ItemStatus[]

export interface Item {
  id: number
  title: string
  description: string | null
  /** ISO days; each optional and independent. */
  startDate: string | null
  endDate: string | null
  status: ItemStatus
  categoryId: number | null
  internal: boolean
  position: number
}

/** Every field the Owner edits in the drawer. */
export type ItemDraft = Omit<Item, 'id' | 'position'>

export const emptyDraft: ItemDraft = {
  title: '',
  description: null,
  startDate: null,
  endDate: null,
  status: 'planned',
  categoryId: null,
  internal: false,
}

export const itemsQuery = (roadmapId: number) =>
  queryOptions({
    queryKey: ['roadmaps', roadmapId, 'items'],
    queryFn: () => api.get<Item[]>(`/api/roadmaps/${roadmapId}/items`),
  })

export const createItem = (roadmapId: number, draft: ItemDraft) =>
  api.post<Item>(`/api/roadmaps/${roadmapId}/items`, draft)

export const replaceItem = (roadmapId: number, itemId: number, draft: ItemDraft) =>
  api.put<Item>(`/api/roadmaps/${roadmapId}/items/${itemId}`, draft)

export const deleteItem = (roadmapId: number, itemId: number) =>
  api.delete(`/api/roadmaps/${roadmapId}/items/${itemId}`)

/** Rewrites the manual order; itemIds must list every Item of the Roadmap. */
export const reorderItems = (roadmapId: number, itemIds: number[]) =>
  api.put<Item[]>(`/api/roadmaps/${roadmapId}/items/order`, { itemIds })

/** After an Item changes: its Roadmap's list, plus the counts on the Roadmaps and Categories screens. */
export function invalidateItems(queryClient: QueryClient, roadmapId: number) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['roadmaps', roadmapId, 'items'] }),
    queryClient.invalidateQueries({ queryKey: ['roadmaps'], exact: true }),
    queryClient.invalidateQueries({ queryKey: ['categories'] }),
  ])
}

export interface Lane {
  /** null for the "No Category" lane. */
  category: Category | null
  items: Item[]
}

/**
 * Swimlanes by Category: one lane per Category in use, then "No Category".
 * A layout only — each lane keeps the manual order.
 */
export function lanesByCategory(items: Item[], categories: Category[]): Lane[] {
  const known = new Set(categories.map((c) => c.id))
  const lanes: Lane[] = categories.map((category) => ({
    category,
    items: items.filter((item) => item.categoryId === category.id),
  }))
  lanes.push({ category: null, items: items.filter((item) => item.categoryId === null || !known.has(item.categoryId)) })
  return lanes.filter((lane) => lane.items.length > 0)
}

/**
 * The manual order after moving `activeId` to where `overId` is (dnd-kit's arrayMove).
 * Works on the full order, so Items in other swimlanes keep their relative places.
 */
export function moveItem(order: number[], activeId: number, overId: number): number[] {
  const from = order.indexOf(activeId)
  const to = order.indexOf(overId)
  if (from === -1 || to === -1 || from === to) return order
  const next = [...order]
  next.splice(from, 1)
  next.splice(to, 0, activeId)
  return next
}
