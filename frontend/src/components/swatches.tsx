import { COLOURS, colourVar, type Colour } from '#/lib/categories'
import { cn } from '#/lib/utils'

/** The 8 palette colours as a radio group. */
export function Swatches({
  value,
  onChange,
  className,
}: {
  value: Colour
  onChange: (colour: Colour) => void
  className?: string
}) {
  return (
    <div role="radiogroup" aria-label="Colour" className={cn('flex gap-1.5', className)}>
      {COLOURS.map((colour) => (
        <button
          key={colour}
          type="button"
          role="radio"
          aria-checked={value === colour}
          aria-label={colour}
          onClick={() => onChange(colour)}
          className="size-6 cursor-pointer rounded-full shadow-[inset_0_0_0_1px_rgba(0,0,0,0.08)] transition-transform hover:scale-110 aria-checked:shadow-[0_0_0_2px_var(--background),0_0_0_4px_var(--foreground)]"
          style={{ background: colourVar(colour) }}
        />
      ))}
    </div>
  )
}
