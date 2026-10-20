import { useEffect, useRef } from 'react'
import { Link, createFileRoute, notFound } from '@tanstack/react-router'
import { useMutation, useSuspenseQuery } from '@tanstack/react-query'

import { BrandMark } from '#/components/brand-mark'
import { NotFound } from '#/components/not-found'
import { Button } from '#/components/ui/button'
import { ApiError } from '#/lib/api'
import { confirmSubscription, publicPageQuery } from '#/lib/public-page'

/** Opened from the confirmation email. Confirms in the browser, so a link preview that only fetches the page does not. */
export const Route = createFileRoute('/r/$publicId_/confirm')({
  validateSearch: (search: Record<string, unknown>): { token?: string } =>
    typeof search.token === 'string' ? { token: search.token } : {},
  loader: async ({ context, params }) => {
    try {
      return await context.queryClient.ensureQueryData(publicPageQuery(params.publicId))
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw notFound()
      throw error
    }
  },
  head: ({ loaderData }) => ({ meta: [{ title: loaderData ? `Confirm your email · ${loaderData.name}` : 'Page not found' }] }),
  notFoundComponent: NotFound,
  component: ConfirmPage,
})

function ConfirmPage() {
  const { publicId } = Route.useParams()
  const { token } = Route.useSearch()
  const { data: page } = useSuspenseQuery(publicPageQuery(publicId))
  const confirm = useMutation({ mutationFn: () => confirmSubscription(token ?? '') })
  const started = useRef(false)

  useEffect(() => {
    if (started.current) return
    started.current = true
    confirm.mutate()
  }, [confirm])

  const title = confirm.isSuccess ? "You're subscribed" : confirm.isError ? 'This link has expired' : 'Confirming…'
  const itemTitle = confirm.data?.itemTitle
  const body = confirm.isSuccess
    ? itemTitle
      ? `You'll get an email when ${itemTitle} ships. Every email has a link to unsubscribe.`
      : `You'll get an email when an Item of ${page.name} ships. Every email has a link to unsubscribe.`
    : confirm.isError
      ? 'Subscribe again from the Roadmap to get a new link.'
      : ''

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 p-8 text-center">
      {page.brandName && <BrandMark name={page.brandName} />}
      <h1 aria-live="polite" className="font-display text-[28px] font-semibold tracking-[-0.5px]">
        {title}
      </h1>
      {body && <p className="max-w-[420px] text-body">{body}</p>}
      <Button variant="secondary" asChild>
        <Link to="/r/$publicId" params={{ publicId }}>
          See the Roadmap
        </Link>
      </Button>
    </main>
  )
}
