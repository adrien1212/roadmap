import { api } from '#/lib/api'

/** What an Owner is in their Organisation; recorded, not enforced yet. */
export type Role = 'owner' | 'admin' | 'editor'

export const ROLE_LABELS: Record<Role, string> = { owner: 'Owner', admin: 'Admin', editor: 'Editor' }

export const ORGANISATION_NAME_MAX_LENGTH = 80

export interface Organisation {
  name: string
}

/** From the Welcome page: the current Owner becomes its Role Owner. */
export const createOrganisation = (name: string) => api.post<Organisation>('/api/organisation', { name })

/**
 * The secret of an Invitation link, from a pasted `…/join/{secret}` link or the secret alone;
 * null when the text is neither.
 */
export function invitationSecret(text: string): string | null {
  const trimmed = text.trim()
  const fromLink = trimmed.match(/\/join\/([A-Za-z0-9_-]+)\/?(?:[?#].*)?$/)
  if (fromLink) return fromLink[1]
  return /^[A-Za-z0-9_-]{20,}$/.test(trimmed) ? trimmed : null
}
