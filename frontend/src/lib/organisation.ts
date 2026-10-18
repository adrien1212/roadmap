import { queryOptions } from '@tanstack/react-query'

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

/** The answer to an Invitation: the only time the link is ever shown. */
export interface CreatedInvitation {
  id: number
  url: string
  expiresAt: string
  emailed: boolean
}

export const createInvitation = (email: string, role: Exclude<Role, 'owner'>) =>
  api.post<CreatedInvitation>('/api/invitations', { email, role })

export const revokeInvitation = (id: number) => api.delete(`/api/invitations/${id}`)

/** What the join page shows, before sign-in. */
export interface PendingInvitation {
  inviterName: string
  organisationName: string
  role: Exclude<Role, 'owner'>
  maskedEmail: string
  expiresAt: string
}

export const invitationQuery = (secret: string) =>
  queryOptions({
    queryKey: ['invitation', secret],
    queryFn: () => api.get<PendingInvitation>(`/api/invitations/${encodeURIComponent(secret)}`),
  })

export const acceptInvitation = (secret: string) =>
  api.post<void>(`/api/invitations/${encodeURIComponent(secret)}/accept`)

/** Google sign-in that comes back to the join page and accepts there. */
export const signInToAccept = (secret: string) =>
  `/oauth2/authorization/google?continue=${encodeURIComponent(`/join/${secret}?accept=1`)}`

/** Someone in the Organisation, as People shows them. */
export interface Person {
  id: number
  name: string | null
  email: string | null
  role: Role
  joinedAt: string
}

/** A pending Invitation, without its link (never stored). */
export interface PendingInvitationRow {
  id: number
  email: string
  role: Exclude<Role, 'owner'>
  invitedBy: string
  expiresAt: string
}

export interface OrganisationOverview {
  name: string
  people: Person[]
  invitations: PendingInvitationRow[]
}

export const organisationQuery = queryOptions({
  queryKey: ['organisation'],
  queryFn: () => api.get<OrganisationOverview>('/api/organisation'),
})

export const renameOrganisation = (name: string) => api.put<Organisation>('/api/organisation', { name })

export const changeRole = (personId: number, role: Exclude<Role, 'owner'>) =>
  api.put<Person>(`/api/organisation/people/${personId}/role`, { role })

/** Your own id: Leave. */
export const removePerson = (personId: number) => api.delete(`/api/organisation/people/${personId}`)
