import { cn } from '#/lib/utils'

/** Product logo + name ("Product name" until it is chosen). */
export function Wordmark({ className, hideTextOnMobile = false }: { className?: string; hideTextOnMobile?: boolean }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-2 font-display text-xl font-semibold tracking-[-0.4px] whitespace-nowrap',
        className,
      )}
    >
      <i
        aria-hidden
        className="relative inline-block size-[18px] rounded-[5px] bg-current after:absolute after:top-2 after:right-1.5 after:left-1 after:h-0.5 after:rounded-sm after:bg-background"
      />
      <span className={cn(hideTextOnMobile && 'max-sm:sr-only')}>Product name</span>
    </span>
  )
}
