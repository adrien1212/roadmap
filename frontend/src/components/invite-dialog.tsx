import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Check, Copy } from 'lucide-react'
import { toast } from 'sonner'

import { FieldError } from '#/components/field-error'
import { Button } from '#/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '#/components/ui/dialog'
import { Input } from '#/components/ui/input'
import { Label } from '#/components/ui/label'
import { createInvitation } from '#/lib/organisation'
import type { CreatedInvitation, Role } from '#/lib/organisation'
import { cn } from '#/lib/utils'

const ROLES: { role: Exclude<Role, 'owner'>; label: string; description: string }[] = [
  { role: 'editor', label: 'Editor', description: 'Adds and edits Items.' },
  { role: 'admin', label: 'Admin', description: 'Same rights as the Owner.' },
]

const title = 'pr-8 font-display text-[22px] leading-[1.3] font-semibold tracking-[-0.3px]'

/** Invite one email with a Role; then shows the link once (design: SPEC-organisation.md, Invite popup). */
export function InviteDialog({
  open,
  onOpenChange,
  organisationName,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  organisationName: string
}) {
  const [created, setCreated] = useState<{ invitation: CreatedInvitation; email: string } | null>(null)

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next)
        if (!next) setCreated(null)
      }}
    >
      <DialogContent>
        {created ? (
          <CreatedStep {...created} onDone={() => onOpenChange(false)} />
        ) : (
          <InviteStep
            organisationName={organisationName}
            onCreated={(invitation, email) => setCreated({ invitation, email })}
            onCancel={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function InviteStep({
  organisationName,
  onCreated,
  onCancel,
}: {
  organisationName: string
  onCreated: (invitation: CreatedInvitation, email: string) => void
  onCancel: () => void
}) {
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Exclude<Role, 'owner'>>('editor')
  const [error, setError] = useState<string | null>(null)
  const shownEmail = email.trim() || 'this email'

  const invite = useMutation({
    mutationFn: () => createInvitation(email, role),
    onSuccess: (invitation) => {
      queryClient.invalidateQueries({ queryKey: ['organisation'] })
      onCreated(invitation, email.trim().toLowerCase())
    },
    onError: (e) => setError(e.message),
  })

  return (
    <form
      className="flex flex-col gap-5"
      onSubmit={(e) => {
        e.preventDefault()
        invite.mutate()
      }}
    >
      <DialogTitle className={title}>Invite to {organisationName}</DialogTitle>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="i-email" className="text-[13px] font-medium text-body">
          Email
        </Label>
        <Input
          id="i-email"
          type="email"
          autoComplete="off"
          placeholder="jane@example.com"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value)
            setError(null)
          }}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'i-email-err' : undefined}
          required
        />
        {error && <FieldError id="i-email-err">{error}</FieldError>}
      </div>
      <fieldset className="flex flex-col gap-2">
        <legend className="mb-1.5 text-[13px] font-medium text-body">Role</legend>
        {ROLES.map((option) => (
          <label
            key={option.role}
            className={cn(
              'flex cursor-pointer items-start gap-3 rounded-md border border-border px-3.5 py-3',
              role === option.role && 'border-foreground',
            )}
          >
            <input
              type="radio"
              name="role"
              value={option.role}
              checked={role === option.role}
              onChange={() => setRole(option.role)}
              className="mt-1 accent-foreground"
            />
            <span className="flex flex-col">
              <span className="text-sm font-medium">{option.label}</span>
              <span className="text-[13px] text-muted-foreground">{option.description}</span>
            </span>
          </label>
        ))}
      </fieldset>
      <DialogDescription className="text-[13px] text-muted-foreground">
        The link expires in 7 days and works only for {shownEmail}.
      </DialogDescription>
      <div className="flex justify-end gap-3">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={invite.isPending}>
          Invite
        </Button>
      </div>
    </form>
  )
}

function CreatedStep({ invitation, email, onDone }: { invitation: CreatedInvitation; email: string; onDone: () => void }) {
  const [copied, setCopied] = useState(false)

  return (
    <div className="flex flex-col gap-5">
      <DialogTitle className={title}>{invitation.emailed ? 'Invitation sent' : 'Invitation created'}</DialogTitle>
      <DialogDescription className="flex items-start gap-2 text-sm text-body">
        {invitation.emailed ? (
          <>
            <Check className="mt-0.5 size-4 shrink-0 text-success-text" aria-hidden />
            Sent to {email}.
          </>
        ) : (
          "Email isn't set up. Copy the link and send it."
        )}
      </DialogDescription>
      <div className="flex gap-2">
        <Label htmlFor="i-link" className="sr-only">
          Invitation link
        </Label>
        <Input id="i-link" readOnly value={invitation.url} onFocus={(e) => e.target.select()} />
        <Button
          type="button"
          variant="secondary"
          onClick={async () => {
            await navigator.clipboard.writeText(invitation.url)
            setCopied(true)
            toast('Link copied')
          }}
        >
          {copied ? <Check aria-hidden /> : <Copy aria-hidden />}
          Copy
        </Button>
      </div>
      <p className="m-0 text-[13px] text-muted-foreground">This link is shown only once.</p>
      <div className="flex justify-end">
        <Button type="button" onClick={onDone}>
          Done
        </Button>
      </div>
    </div>
  )
}
