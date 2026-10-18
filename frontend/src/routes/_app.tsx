import { Outlet, createFileRoute, redirect } from '@tanstack/react-router'

import { AppBar } from '#/components/app-bar'
import { Toaster } from '#/components/ui/sonner'
import { ApiError } from '#/lib/api'
import { meQuery } from '#/lib/me'

/** Pathless layout for every editor screen; the Public page (/r/*) and Home (/) sit outside it. */
export const Route = createFileRoute('/_app')({
  // Runs before any editor loader: an anonymous visitor goes to the Home page to sign in,
  // an Owner without an Organisation to the Welcome page to create or join one.
  beforeLoad: async ({ context }) => {
    let me
    try {
      me = await context.queryClient.ensureQueryData(meQuery)
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) throw redirect({ to: '/' })
      throw error
    }
    if (!me.organisation) throw redirect({ to: '/welcome' })
  },
  component: AppLayout,
})

function AppLayout() {
  return (
    <>
      <AppBar />
      <Outlet />
      <Toaster />
    </>
  )
}
