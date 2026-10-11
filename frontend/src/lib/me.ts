import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'

/** The signed-in Owner, from `GET /api/me`. */
export interface Me {
  id: number
  name: string | null
  email: string | null
  provider: string
}

export const meQuery = queryOptions({
  queryKey: ['me'],
  queryFn: () => api.get<Me>('/api/me'),
  staleTime: Infinity,
})

/** Up to two letters from the Owner's name, else from the email; "?" when neither is known. */
export function initials(me: Me): string {
  const source = me.name?.trim() || me.email?.split('@')[0] || ''
  const letters = source
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0].toUpperCase())
    .join('')
  return letters || '?'
}
