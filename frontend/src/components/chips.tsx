import { EyeOff } from 'lucide-react'

import { colourVar, type Colour } from '#/lib/categories'
import { cn } from '#/lib/utils'

const chip = 'inline-flex items-center gap-1.5 rounded-full px-2.5 py-[3px] text-[13px] leading-[1.4] font-medium whitespace-nowrap'

export function CategoryChip({ name, colour, children }: { name: string; colour: Colour; children?: React.ReactNode }) {
  return (
    <span className={cn(chip, 'text-foreground')} style={{ background: colourVar(colour) }}>
      {name}
      {children}
    </span>
  )
}

export function NeutralChip({ children, className }: { children: React.ReactNode; className?: string }) {
  return <span className={cn(chip, 'bg-surface-card text-body', className)}>{children}</span>
}

export function InternalChip() {
  return (
    <NeutralChip className="px-2 py-px">
      <EyeOff className="size-3.5" aria-hidden />
      Internal
    </NeutralChip>
  )
}

export function CategoryDot({ colour }: { colour: Colour | null }) {
  return (
    <span
      aria-hidden
      className="inline-block size-2 shrink-0 rounded-full"
      style={{ background: colour ? colourVar(colour) : 'var(--surface-strong)' }}
    />
  )
}
