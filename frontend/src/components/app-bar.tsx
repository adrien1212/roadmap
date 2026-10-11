import { Link } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'

import { Wordmark } from '#/components/wordmark'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '#/components/ui/dropdown-menu'
import { initials, meQuery } from '#/lib/me'

const navLink =
  'inline-flex h-[34px] items-center rounded-md px-3 text-sm font-medium text-muted-foreground no-underline hover:bg-surface-soft hover:text-foreground aria-[current=page]:bg-surface-card aria-[current=page]:text-foreground'

/** The editor's top bar: wordmark, main nav and the Owner menu (design/roadmap-ui.html). */
export function AppBar() {
  const { data: me } = useSuspenseQuery(meQuery)

  return (
    <div className="mx-auto max-w-[1200px] px-4 sm:px-8">
      <header className="flex h-16 items-center justify-between gap-4 border-b border-hairline-soft">
        <div className="flex items-center">
          <Link to="/roadmaps" aria-label="Product name" className="text-foreground no-underline">
            <Wordmark hideTextOnMobile />
          </Link>
          <nav aria-label="Main" className="ml-2 flex gap-1 sm:ml-7">
            <Link to="/roadmaps" className={navLink}>
              Roadmaps
            </Link>
            <Link to="/categories" className={navLink}>
              Categories
            </Link>
            <Link to="/settings" className={navLink}>
              Settings
            </Link>
          </nav>
        </div>
        <DropdownMenu>
          <DropdownMenuTrigger
            aria-label="Your account"
            className="flex size-8 cursor-pointer items-center justify-center rounded-full bg-cat-emerald text-[13px] font-semibold text-foreground"
          >
            {initials(me)}
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" sideOffset={8} className="min-w-[220px] rounded-lg p-1.5 shadow-overlay">
            <DropdownMenuLabel className="px-2.5 pt-2 pb-2.5 font-normal">
              <div className="text-base font-semibold">Signed in</div>
              <div className="text-[13px] font-medium text-muted-foreground">with {providerLabel(me.provider)}</div>
            </DropdownMenuLabel>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>
    </div>
  )
}

function providerLabel(provider: string): string {
  return provider.charAt(0).toUpperCase() + provider.slice(1)
}
