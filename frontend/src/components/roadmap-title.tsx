import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { renameRoadmap, type Roadmap } from '#/lib/roadmaps'

const title = 'font-display text-[28px] leading-[1.2] font-semibold tracking-[-0.5px]'

/** The editor's Roadmap name, renamed inline: click (or Enter) to edit, Enter or blur saves, Escape cancels. */
export function RoadmapTitle({
  roadmap,
  editing,
  onEditingChange,
}: {
  roadmap: Roadmap
  editing: boolean
  onEditingChange: (editing: boolean) => void
}) {
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState(roadmap.name)

  const rename = useMutation({
    mutationFn: (name: string) => renameRoadmap(roadmap.id, name),
    onSuccess: (updated) => {
      queryClient.setQueryData(['roadmaps', roadmap.id], updated)
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] })
    },
    onError: (e) => toast(e.message),
  })

  function finish(save: boolean) {
    const name = draft.trim()
    if (save && name && name !== roadmap.name) rename.mutate(name)
    onEditingChange(false)
  }

  if (editing) {
    return (
      <>
        <label htmlFor="rename" className="sr-only">
          Roadmap name
        </label>
        <input
          id="rename"
          autoFocus
          onFocus={(e) => e.target.select()}
          value={draft}
          maxLength={120}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={() => finish(true)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') e.currentTarget.blur()
            if (e.key === 'Escape') {
              setDraft(roadmap.name)
              onEditingChange(false)
            }
          }}
          className={`${title} -ml-[7px] h-[46px] w-[min(560px,100%)] rounded-sm border border-foreground px-1.5 shadow-[0_0_0_3px_rgba(17,17,17,0.08)] outline-none`}
        />
      </>
    )
  }

  const name = rename.isPending ? rename.variables : roadmap.name
  return (
    <h1
      role="button"
      tabIndex={0}
      aria-label={`Rename ${name}`}
      onClick={() => {
        setDraft(roadmap.name)
        onEditingChange(true)
      }}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          setDraft(roadmap.name)
          onEditingChange(true)
        }
      }}
      className={`${title} -ml-1.5 cursor-text truncate rounded-sm px-1.5 py-0.5 hover:bg-surface-soft`}
    >
      {name}
    </h1>
  )
}
