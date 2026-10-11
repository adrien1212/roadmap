import { Outlet, createFileRoute } from '@tanstack/react-router'

import { AppBar } from '#/components/app-bar'
import { Toaster } from '#/components/ui/sonner'
import { meQuery } from '#/lib/me'

/** Pathless layout for every editor screen; the Public page (/r/*) and Home (/) sit outside it. */
export const Route = createFileRoute('/_app')({
  loader: ({ context }) => context.queryClient.ensureQueryData(meQuery),
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
