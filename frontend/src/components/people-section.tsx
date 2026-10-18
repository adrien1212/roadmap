import { useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'
import { toast } from 'sonner'

import { ConfirmDialog } from '#/components/confirm-dialog'
import { FieldError } from '#/components/field-error'
import { InviteDialog } from '#/components/invite-dialog'
import { Button } from '#/components/ui/button'
import { Input } from '#/components/ui/input'
import { Label } from '#/components/ui/label'
import { formatInstant } from '#/lib/format'
import { meQuery } from '#/lib/me'
import {
  ORGANISATION_NAME_MAX_LENGTH,
  ROLE_LABELS,
  changeRole,
  organisationQuery,
  removePerson,
  renameOrganisation,
  revokeInvitation,
} from '#/lib/organisation'
import type { Person, Role } from '#/lib/organisation'

const panel = 'flex flex-col gap-5 rounded-lg border border-border p-6'
const sectionTitle = 'font-display text-[20px] leading-tight font-semibold tracking-[-0.3px]'
const hint = 'm-0 text-[13px] text-muted-foreground'
const row = 'flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-hairline-soft py-3 first:border-t-0 first:pt-0'

/** The Organisation's name; seen only by its people, never by Readers. */
export function OrganisationSection() {
  const { data: organisation } = useSuspenseQuery(organisationQuery)
  const queryClient = useQueryClient()
  const [name, setName] = useState(organisation.name)
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => renameOrganisation(name),
    onSuccess: (saved) => {
      setName(saved.name)
      setError(null)
      queryClient.invalidateQueries({ queryKey: organisationQuery.queryKey })
      queryClient.invalidateQueries({ queryKey: meQuery.queryKey })
      toast('Settings saved')
    },
    onError: (e) => setError(e.message),
  })

  return (
    <form
      className={panel}
      onSubmit={(e) => {
        e.preventDefault()
        save.mutate()
      }}
    >
      <h2 className={sectionTitle}>Organisation</h2>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="o-name" className="text-[13px] font-medium text-body">
          Organisation name
        </Label>
        <Input
          id="o-name"
          value={name}
          maxLength={ORGANISATION_NAME_MAX_LENGTH}
          onChange={(e) => setName(e.target.value)}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'o-err' : 'o-hint'}
        />
        {error ? (
          <FieldError id="o-err">{error}</FieldError>
        ) : (
          <p id="o-hint" className={hint}>
            Seen by the people of your Organisation only, never by Readers.
          </p>
        )}
      </div>
      <div>
        <Button type="submit" disabled={save.isPending}>
          Save
        </Button>
      </div>
    </form>
  )
}

/** Everyone in the Organisation and the pending Invitations. Roles are recorded, not enforced yet. */
export function PeopleSection() {
  const { data: organisation } = useSuspenseQuery(organisationQuery)
  const { data: me } = useSuspenseQuery(meQuery)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [inviting, setInviting] = useState(false)
  const [removing, setRemoving] = useState<Person | null>(null)

  const refresh = () => queryClient.invalidateQueries({ queryKey: organisationQuery.queryKey })

  const role = useMutation({
    mutationFn: ({ person, next }: { person: Person; next: Exclude<Role, 'owner'> }) => changeRole(person.id, next),
    onSuccess: () => {
      refresh()
      queryClient.invalidateQueries({ queryKey: meQuery.queryKey })
      toast('Role changed')
    },
    onError: (e) => toast.error(e.message),
  })

  const remove = useMutation({
    mutationFn: (person: Person) => removePerson(person.id),
    onSuccess: async (_, person) => {
      if (person.id === me.id) {
        await queryClient.invalidateQueries({ queryKey: meQuery.queryKey })
        await navigate({ to: '/welcome' })
        return
      }
      refresh()
      toast(`${person.name ?? person.email} removed`)
    },
    onError: (e) => toast.error(e.message),
  })

  const revoke = useMutation({
    mutationFn: (id: number) => revokeInvitation(id),
    onSuccess: () => {
      refresh()
      toast('Invitation revoked')
    },
    onError: (e) => toast.error(e.message),
  })

  const leaving = removing?.id === me.id

  return (
    <section className={panel} aria-labelledby="people-title">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="people-title" className={sectionTitle}>
          People
        </h2>
        <Button variant="secondary" onClick={() => setInviting(true)}>
          Invite
        </Button>
      </div>

      <ul className="m-0 flex list-none flex-col p-0">
        {organisation.people.map((person) => {
          const isMe = person.id === me.id
          const isOwner = person.role === 'owner'
          return (
            <li key={person.id} className={row}>
              <div className="flex min-w-0 flex-1 flex-col">
                <span className="truncate text-sm font-medium">
                  {person.name ?? person.email}
                  {isMe && <span className="font-normal text-muted-foreground"> (you)</span>}
                </span>
                <span className="truncate text-[13px] text-muted-foreground" suppressHydrationWarning>
                  {person.email} · joined {formatInstant(person.joinedAt)}
                </span>
              </div>
              {isOwner ? (
                <span className="text-sm text-body">Owner</span>
              ) : (
                <>
                  <Label htmlFor={`role-${person.id}`} className="sr-only">
                    Role of {person.name ?? person.email}
                  </Label>
                  <select
                    id={`role-${person.id}`}
                    value={person.role}
                    disabled={role.isPending}
                    onChange={(e) =>
                      role.mutate({ person, next: e.target.value as Exclude<Role, 'owner'> })
                    }
                    className="h-9 rounded-md border border-input bg-background px-2.5 text-sm"
                  >
                    <option value="editor">{ROLE_LABELS.editor}</option>
                    <option value="admin">{ROLE_LABELS.admin}</option>
                  </select>
                  <Button variant="ghost" onClick={() => setRemoving(person)}>
                    {isMe ? 'Leave' : 'Remove'}
                  </Button>
                </>
              )}
            </li>
          )
        })}
      </ul>

      {organisation.invitations.length > 0 && (
        <div className="flex flex-col gap-2">
          <h3 className="m-0 text-[13px] font-medium text-body">Pending invitations</h3>
          <ul className="m-0 flex list-none flex-col p-0">
            {organisation.invitations.map((invitation) => (
              <li key={invitation.id} className={row}>
                <div className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate text-sm font-medium">{invitation.email}</span>
                  <span className="truncate text-[13px] text-muted-foreground" suppressHydrationWarning>
                    {ROLE_LABELS[invitation.role]} · invited by {invitation.invitedBy} · expires{' '}
                    {formatInstant(invitation.expiresAt)}
                  </span>
                </div>
                <Button variant="ghost" disabled={revoke.isPending} onClick={() => revoke.mutate(invitation.id)}>
                  Revoke
                </Button>
              </li>
            ))}
          </ul>
        </div>
      )}

      <p className={hint}>Roles are recorded for now; everyone in the Organisation can still do everything.</p>

      <InviteDialog open={inviting} onOpenChange={setInviting} organisationName={organisation.name} />
      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(open) => !open && setRemoving(null)}
        title={leaving ? `Leave ${organisation.name}?` : `Remove ${removing?.name ?? removing?.email}?`}
        body={
          leaving
            ? "You lose access to its Roadmaps. What you created stays with the Organisation."
            : 'They lose access to the Roadmaps. What they created stays with the Organisation.'
        }
        cta={leaving ? 'Leave' : 'Remove'}
        onConfirm={() => removing && remove.mutate(removing)}
      />
    </section>
  )
}
