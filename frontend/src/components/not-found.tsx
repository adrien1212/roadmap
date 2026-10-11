/** "Page not found", for unknown routes and for Public pages that are Private or unknown. */
export function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-2.5 p-8 text-center">
      <span aria-hidden className="font-display text-8xl leading-none tracking-[-3px] text-surface-strong">
        404
      </span>
      <h1 className="font-display text-[28px] font-semibold tracking-[-0.5px]">Page not found</h1>
      <p className="max-w-[380px] text-body">Check the link, or ask whoever shared it for a new one.</p>
    </main>
  )
}
