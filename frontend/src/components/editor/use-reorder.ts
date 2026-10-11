import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { itemsQuery, moveItem, reorderItems, type Item } from '#/lib/items'

/** Moves an Item in the manual order: applied at once, rolled back if the API refuses. */
export function useReorder(roadmapId: number) {
  const queryClient = useQueryClient()
  const { queryKey } = itemsQuery(roadmapId)

  const mutation = useMutation({
    mutationFn: (order: number[]) => reorderItems(roadmapId, order),
    onMutate: async (order) => {
      await queryClient.cancelQueries({ queryKey })
      const previous = queryClient.getQueryData<Item[]>(queryKey)
      if (previous) {
        const byId = new Map(previous.map((item) => [item.id, item]))
        queryClient.setQueryData<Item[]>(
          queryKey,
          order.map((id, position) => ({ ...byId.get(id)!, position })),
        )
      }
      return { previous }
    },
    onError: (e, _, context) => {
      if (context?.previous) queryClient.setQueryData(queryKey, context.previous)
      toast(e.message)
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey }),
  })

  /** `allItems` is the full manual order, Items waiting out an Undo included (the API needs every id). */
  return (allItems: Item[], activeId: number, overId: number) => {
    const order = allItems.map((item) => item.id)
    const next = moveItem(order, activeId, overId)
    if (next !== order) mutation.mutate(next)
  }
}
