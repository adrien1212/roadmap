import { letterMark } from '#/lib/brand'
import { cn } from '#/lib/utils'

/** The Brand as Readers see it: a letter mark (ink tile) and the product name. */
export function BrandMark({ name, className }: { name: string; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-2.5 text-[15px] font-semibold text-foreground', className)}>
      <span
        aria-hidden
        className="inline-flex size-7 shrink-0 items-center justify-center rounded-sm bg-foreground font-display text-[15px] text-white"
      >
        {letterMark(name)}
      </span>
      {name}
    </span>
  )
}
