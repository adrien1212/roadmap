import { useEffect, useRef, useState } from 'react'
import { createFileRoute, notFound, useNavigate } from '@tanstack/react-router'
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'

import { FieldError } from '#/components/field-error'
import { Wordmark } from '#/components/wordmark'
import { Button } from '#/components/ui/button'
import { ApiError } from '#/lib/api'
import { formatInstant } from '#/lib/format'
import { meQuery } from '#/lib/me'
import { ROLE_LABELS, acceptInvitation, invitationQuery, signInToAccept } from '#/lib/organisation'

/** Opened from an Invitation link or the Welcome page: one Accept button. Signed out, Accept signs in first. */
export const Route = createFileRoute('/join/$secret')({
  validateSearch: (search: Record<string, unknown>): { accept?: 1 } =>
    search.accept === 1 || search.accept === '1' ? { accept: 1 } : {},
  loader: async ({ context, params }) => {
    const signedIn = await context.queryClient.fetchQuery(meQuery).then(
      () => true,
      () => false,
    )
    try {
      await context.queryClient.ensureQueryData(invitationQuery(params.secret))
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw notFound()
      throw error
    }
    return { signedIn }
  },
  head: () => ({ meta: [{ title: 'Join · Product name' }] }),
  notFoundComponent: () => (
    <Frame>
      <h1 className={heading}>This invitation is no longer valid</h1>
      <p className="text-body">Ask the person who invited you for a new link.</p>
    </Frame>
  ),
  component: JoinPage,
})

const heading = 'font-display text-[24px] leading-tight font-semibold tracking-[-0.4px]'

function Frame({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="mx-auto flex h-16 w-full max-w-[1200px] items-center px-4 sm:px-8">
        <Wordmark />
      </header>
      <main className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="flex w-full max-w-[440px] flex-col items-center gap-4 rounded-lg border border-border p-8 text-center">
          {children}
        </div>
      </main>
    </div>
  )
}

function JoinPage() {
  const { secret } = Route.useParams()
  const { accept: returnedFromSignIn } = Route.useSearch()
  const { signedIn } = Route.useLoaderData()
  const { data: invitation } = useSuspenseQuery(invitationQuery(secret))
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  const accept = useMutation({
    mutationFn: () => acceptInvitation(secret),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: meQuery.queryKey })
      await navigate({ to: '/roadmaps' })
    },
    onError: (e) =>
      setError(
        e instanceof ApiError && e.status === 404
          ? 'This invitation is no longer valid. Ask the person who invited you for a new link.'
          : e.message,
      ),
  })

  // Back from Google sign-in: accept at once, as the Owner already pressed Accept.
  const started = useRef(false)
  useEffect(() => {
    if (!returnedFromSignIn || !signedIn || started.current) return
    started.current = true
    accept.mutate()
  }, [returnedFromSignIn, signedIn, accept])

  const initial = (Array.from(invitation.inviterName.trim())[0] ?? '?').toUpperCase()

  return (
    <Frame>
      <span
        aria-hidden
        className="flex size-10 items-center justify-center rounded-md bg-foreground text-lg font-semibold text-background"
      >
        {initial}
      </span>
      <h1 className={heading}>
        {invitation.inviterName} invited you to join {invitation.organisationName} as{' '}
        {ROLE_LABELS[invitation.role]}.
      </h1>
      <Button
        size="lg"
        disabled={accept.isPending}
        onClick={() => {
          setError(null)
          if (signedIn) accept.mutate()
          else window.location.assign(signInToAccept(secret))
        }}
      >
        Accept
      </Button>
      {error && <FieldError>{error}</FieldError>}
      <p className="m-0 text-[13px] text-muted-foreground" suppressHydrationWarning>
        For {invitation.maskedEmail} · expires {formatInstant(invitation.expiresAt)}
      </p>
    </Frame>
  )
}
