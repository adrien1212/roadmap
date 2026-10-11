import { createFileRoute, redirect } from '@tanstack/react-router'

// Until the Home page (Task 18): the dev Owner is always signed in, and a
// signed-in Owner opening / goes to their Roadmaps.
export const Route = createFileRoute('/')({
  beforeLoad: () => {
    throw redirect({ to: '/roadmaps' })
  },
})
