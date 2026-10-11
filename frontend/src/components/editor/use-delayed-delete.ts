import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { deleteItem, invalidateItems, type Item } from '#/lib/items'

const UNDO_WINDOW_MS = 5000

/**
 * Item delete with Undo, client side: the Item is hidden at once and an "Undo"
 * toast shows for 5 s; the DELETE is only sent when that window ends.
 */
export function useDelayedDelete(roadmapId: number) {
  const queryClient = useQueryClient()
  const [hidden, setHidden] = useState<ReadonlySet<number>>(new Set())

  const show = (id: number) =>
    setHidden((current) => {
      const next = new Set(current)
      next.delete(id)
      return next
    })

  function remove(item: Item) {
    setHidden((current) => new Set(current).add(item.id))
    const timer = setTimeout(async () => {
      try {
        await deleteItem(roadmapId, item.id)
        await invalidateItems(queryClient, roadmapId)
      } catch (e) {
        toast(e instanceof Error ? e.message : 'The Item could not be deleted.')
      }
      show(item.id)
    }, UNDO_WINDOW_MS)
    toast('Item deleted', {
      duration: UNDO_WINDOW_MS,
      action: {
        label: 'Undo',
        onClick: () => {
          clearTimeout(timer)
          show(item.id)
        },
      },
    })
  }

  return { hidden, remove }
}
