import { GanttChart, List } from 'lucide-react'

export type View = 'timeline' | 'list'

/** Timeline / List segmented control. */
export function ViewToggle({ view, onChange }: { view: View; onChange: (view: View) => void }) {
  const button =
    'inline-flex h-[30px] cursor-pointer items-center gap-1.5 rounded-[6px] px-3 text-[13px] font-medium text-muted-foreground transition-colors hover:text-foreground aria-pressed:bg-background aria-pressed:font-semibold aria-pressed:text-foreground aria-pressed:shadow-soft'
  return (
    <div role="group" aria-label="View" className="inline-flex gap-0.5 rounded-md bg-surface-card p-[3px]">
      <button aria-pressed={view === 'timeline'} onClick={() => onChange('timeline')} className={button}>
        <GanttChart className="size-4" aria-hidden />
        Timeline
      </button>
      <button aria-pressed={view === 'list'} onClick={() => onChange('list')} className={button}>
        <List className="size-4" aria-hidden />
        List
      </button>
    </div>
  )
}
