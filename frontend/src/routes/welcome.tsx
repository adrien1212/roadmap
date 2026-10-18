import { useState } from 'react'
import { createFileRoute, redirect, useNavigate } from '@tanstack/react-router'
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'

import { FieldError } from '#/components/field-error'
import { Wordmark } from '#/components/wordmark'
import { Button } from '#/components/ui/button'
import { Input } from '#/components/ui/input'
import { Label } from '#/components/ui/label'
import { ApiError, signOut } from '#/lib/api'
import { meQuery } from '#/lib/me'
import { ORGANISATION_NAME_MAX_LENGTH, createOrganisation, invitationSecret } from '#/lib/organisation'

/** After sign-in, for an Owner without an Organisation: create one, or join one (design/Onboarding.dc.html). */
export const Route = createFileRoute('/welcome')({
  beforeLoad: async ({ context }) => {
    let me
    try {
      me = await context.queryClient.ensureQueryData(meQuery)
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) throw redirect({ to: '/' })
      throw error
    }
    if (me.organisation) throw redirect({ to: '/roadmaps' })
  },
  head: () => ({ meta: [{ title: 'Welcome · Product name' }] }),
  component: WelcomePage,
})

const card = 'flex flex-col gap-5 rounded-lg bg-surface-card p-6 sm:p-8'
const cardTitle = 'font-display text-[22px] leading-tight font-semibold tracking-[-0.3px]'
const hint = 'm-0 text-sm text-body'

function WelcomePage() {
  const { data: me } = useSuspenseQuery(meQuery)

  return (
    <div className="flex min-h-screen flex-col">
      <header className="mx-auto flex h-16 w-full max-w-[1200px] items-center justify-between px-4 sm:px-8">
        <Wordmark />
        <div className="flex items-center gap-3">
          <span className="text-[13px] text-muted-foreground max-sm:hidden">{me.email}</span>
          <Button variant="ghost" onClick={() => signOut()}>
            Sign out
          </Button>
        </div>
      </header>

      <main className="flex flex-1 flex-col items-center justify-center gap-10 px-4 py-10 sm:px-8">
        <div className="flex flex-col items-center gap-2 text-center">
          <h1 className="font-display text-[32px] leading-tight font-semibold tracking-[-1px] sm:text-[40px]">
            Welcome to Product name
          </h1>
          <p className="text-body">Create an organisation, or join the one you were invited to.</p>
        </div>
        <div className="grid w-full max-w-[744px] grid-cols-1 gap-6 min-[720px]:grid-cols-2">
          <CreateCard />
          <JoinCard />
        </div>
      </main>
    </div>
  )
}

function CreateCard() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)

  const create = useMutation({
    mutationFn: () => createOrganisation(name),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: meQuery.queryKey })
      await navigate({ to: '/roadmaps' })
    },
    onError: (e) => setError(e.message),
  })

  return (
    <form
      className={card}
      onSubmit={(e) => {
        e.preventDefault()
        if (!name.trim()) return setError('Organisation name is required')
        create.mutate()
      }}
    >
      <div className="flex flex-col gap-1.5">
        <h2 className={cardTitle}>Create a new organisation</h2>
        <p className={hint}>Set up your organisation; invite people later.</p>
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="w-name" className="sr-only">
          Organisation name
        </Label>
        <Input
          id="w-name"
          placeholder="Organisation name"
          value={name}
          maxLength={ORGANISATION_NAME_MAX_LENGTH}
          onChange={(e) => {
            setName(e.target.value)
            setError(null)
          }}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'w-name-err' : undefined}
        />
        {error && <FieldError id="w-name-err">{error}</FieldError>}
      </div>
      <Button type="submit" className="w-full" disabled={create.isPending}>
        Create organisation
      </Button>
    </form>
  )
}

function JoinCard() {
  const navigate = useNavigate()
  const [link, setLink] = useState('')
  const [error, setError] = useState<string | null>(null)

  return (
    <form
      className={card}
      onSubmit={(e) => {
        e.preventDefault()
        const secret = invitationSecret(link)
        if (!secret) return setError("This link isn't an invitation link.")
        navigate({ href: `/join/${secret}` })
      }}
    >
      <div className="flex flex-col gap-1.5">
        <h2 className={cardTitle}>Join an organisation</h2>
        <p className={hint}>Paste the invitation link you received.</p>
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="w-link" className="sr-only">
          Invitation link
        </Label>
        <Input
          id="w-link"
          placeholder={`${typeof window === 'undefined' ? '' : window.location.host}/join/…`}
          value={link}
          onChange={(e) => {
            setLink(e.target.value)
            setError(null)
          }}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'w-link-err' : undefined}
          suppressHydrationWarning
        />
        {error && <FieldError id="w-link-err">{error}</FieldError>}
      </div>
      <Button type="submit" variant="secondary" className="w-full">
        Join organisation
      </Button>
    </form>
  )
}
