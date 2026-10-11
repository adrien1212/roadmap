import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'

/** Palette keys, in the order a new Category is offered them. Same keys as the backend. */
export const COLOURS = ['violet', 'emerald', 'orange', 'pink', 'sky', 'amber', 'lime', 'slate'] as const
export type Colour = (typeof COLOURS)[number]

export interface Category {
  id: number
  name: string
  colour: Colour
}

/** As listed on the Categories screen: with the number of Items using it across all Roadmaps. */
export interface CategoryWithCount extends Category {
  itemCount: number
}

export const categoriesQuery = queryOptions({
  queryKey: ['categories'],
  queryFn: () => api.get<CategoryWithCount[]>('/api/categories'),
})

/** The colour offered to the next new Category: the palette in turn. */
export function nextColour(categoryCount: number): Colour {
  return COLOURS[categoryCount % COLOURS.length]
}

export const colourVar = (colour: Colour) => `var(--cat-${colour})`

export const createCategory = (body: { name: string; colour: Colour }) => api.post<Category>('/api/categories', body)

export const updateCategory = (id: number, body: { name?: string; colour?: Colour }) =>
  api.patch<Category>(`/api/categories/${id}`, body)

export const deleteCategory = (id: number) => api.delete(`/api/categories/${id}`)
