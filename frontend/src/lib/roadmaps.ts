import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'

export interface Roadmap {
  id: number
  name: string
  publicId: string
  /** Set when Published, null when Private. */
  publishedAt: string | null
}

/** As listed on the Roadmaps screen. */
export interface RoadmapSummary extends Roadmap {
  itemCount: number
  internalItemCount: number
}

export const roadmapsQuery = queryOptions({
  queryKey: ['roadmaps'],
  queryFn: () => api.get<RoadmapSummary[]>('/api/roadmaps'),
})

export const roadmapQuery = (id: number) =>
  queryOptions({
    queryKey: ['roadmaps', id],
    queryFn: () => api.get<Roadmap>(`/api/roadmaps/${id}`),
  })

/** Creates "Untitled Roadmap". */
export const createRoadmap = () => api.post<Roadmap>('/api/roadmaps')

export const renameRoadmap = (id: number, name: string) => api.patch<Roadmap>(`/api/roadmaps/${id}`, { name })

export const deleteRoadmap = (id: number) => api.delete(`/api/roadmaps/${id}`)

export const publishRoadmap = (id: number) => api.post<Roadmap>(`/api/roadmaps/${id}/publish`)

export const unpublishRoadmap = (id: number) => api.post<Roadmap>(`/api/roadmaps/${id}/unpublish`)

/** Path of the Public page; prefix with the origin for a shareable URL. */
export const publicPath = (roadmap: Pick<Roadmap, 'publicId'>) => `/r/${roadmap.publicId}`
