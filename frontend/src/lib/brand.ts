import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'

/** The Owner's Brand: the product name Readers see. Optional (null). */
export interface Brand {
  name: string | null
}

export const BRAND_MAX_LENGTH = 60

export const brandQuery = queryOptions({
  queryKey: ['brand'],
  queryFn: () => api.get<Brand>('/api/brand'),
})

/** Blank clears the Brand. */
export const saveBrand = (name: string) => api.put<Brand>('/api/brand', { name })

/** First character of the name, upper-cased; by code point so an emoji or accented letter stays whole. */
export function letterMark(name: string): string {
  return (Array.from(name.trim())[0] ?? '').toUpperCase()
}

/** The Public page's title and og:title: "Roadmap · Brand", or the Roadmap name alone. */
export function pageTitle(roadmapName: string, brandName: string | null): string {
  return brandName ? `${roadmapName} · ${brandName}` : roadmapName
}

/** The Public page's og:description. */
export function previewDescription(brandName: string | null): string {
  return brandName
    ? `What ${brandName} has shipped, is working on and plans.`
    : 'What has shipped, is in progress and is planned.'
}
