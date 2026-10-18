import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'
import type { Organisation, Role } from '#/lib/organisation'

/** The signed-in Owner, from `GET /api/me`. No Organisation (and no Role) until the Welcome page. */
export interface Me {
  id: number
  name: string | null
  email: string | null
  provider: string
  role: Role | null
  organisation: Organisation | null
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
