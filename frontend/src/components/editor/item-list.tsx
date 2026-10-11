import {
  DndContext,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { GripVertical, Trash2 } from 'lucide-react'

import { CategoryChip, InternalChip, NeutralChip, StatusChip } from '#/components/chips'
import { Button } from '#/components/ui/button'
import type { Category } from '#/lib/categories'
import { formatDates } from '#/lib/format'
import { lanesByCategory, type Item } from '#/lib/items'
import { cn } from '#/lib/utils'

// handle | title | Category | Status | dates | delete — Category and dates drop on narrow screens.
const grid =
  'grid items-center gap-3 grid-cols-[28px_minmax(0,1fr)_112px_40px] min-[900px]:grid-cols-[28px_minmax(0,1fr)_150px_112px_200px_40px]'
const gridLanes = 'min-[900px]:grid-cols-[28px_minmax(0,1fr)_112px_200px_40px]'

export interface ItemListProps {
  items: Item[]
  categories: Category[]
  lanes: boolean
  onEdit: (item: Item) => void
  onDelete: (item: Item) => void
  /** Move `activeId` to where `overId` is in the manual order. */
  onMove: (activeId: number, overId: number) => void
}

/**
 * The editor's list view: Items in manual order, optionally in swimlanes by Category.
 * Rows are reordered by dragging the handle, or with the up / down arrow keys on it;
 * in swimlanes an Item moves past its neighbours in the same lane only.
 */
export function ItemList({ items, categories, lanes, onEdit, onDelete, onMove }: ItemListProps) {
  const categoryOf = (item: Item) => categories.find((c) => c.id === item.categoryId) ?? null
  const groups = lanes ? lanesByCategory(items, categories) : [{ category: null, items }]
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 4 } }))

  function onDragEnd({ active, over }: DragEndEvent) {
    const sameLane = over && active.data.current?.sortable.containerId === over.data.current?.sortable.containerId
    if (over && sameLane && active.id !== over.id) onMove(Number(active.id), Number(over.id))
  }

  function onHandleKey(event: React.KeyboardEvent, laneItems: Item[], item: Item) {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return
    event.preventDefault()
    const index = laneItems.indexOf(item)
    const neighbour = laneItems[event.key === 'ArrowUp' ? index - 1 : index + 1]
    if (!neighbour) return
    onMove(item.id, neighbour.id)
    // Keep the keyboard on the moved Item once it re-renders at its new place.
    requestAnimationFrame(() => document.querySelector<HTMLElement>(`[data-handle="${item.id}"]`)?.focus())
  }

  return (
    <div className="mt-3">
      <div aria-hidden className={cn(grid, lanes && gridLanes, 'px-1 py-2 text-xs font-medium text-muted-foreground')}>
        <span />
        <span>Item</span>
        {!lanes && <span className="max-[899px]:hidden">Category</span>}
        <span>Status</span>
        <span className="max-[899px]:hidden">Dates</span>
        <span />
      </div>
      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
        {groups.map((group) => (
          <section key={group.category?.id ?? 'none'}>
            {lanes && (
              <div className="flex items-center gap-2 px-1 pt-5 pb-2 text-[13px] font-semibold">
                {group.category ? (
                  <CategoryChip name={group.category.name} colour={group.category.colour} />
                ) : (
                  <NeutralChip>No Category</NeutralChip>
                )}
                <span className="font-normal text-muted-foreground tabular-nums">{group.items.length}</span>
              </div>
            )}
            <ul className="border-t border-hairline-soft">
              <SortableContext
                id={`lane-${group.category?.id ?? 'none'}`}
                items={group.items.map((item) => item.id)}
                strategy={verticalListSortingStrategy}
              >
                {group.items.map((item) => {
                  const category = categoryOf(item)
                  return (
                    <SortableRow
                      key={item.id}
                      item={item}
                      className={cn(grid, lanes && gridLanes)}
                      onHandleKey={(event) => onHandleKey(event, group.items, item)}
                    >
                      <button
                        onClick={() => onEdit(item)}
                        className="flex min-w-0 cursor-pointer items-center gap-2 rounded-sm text-left text-[15px] font-medium"
                      >
                        <span className={cn('truncate', item.internal && 'text-body')}>{item.title}</span>
                        {item.internal && <InternalChip />}
                      </button>
                      {!lanes && (
                        <div className="max-[899px]:hidden">
                          {category ? (
                            <CategoryChip name={category.name} colour={category.colour} />
                          ) : (
                            <span className="text-[13px] font-medium text-muted-foreground">No Category</span>
                          )}
                        </div>
                      )}
                      <div>
                        <StatusChip status={item.status} />
                      </div>
                      <span
                        className={cn(
                          'text-sm tabular-nums max-[899px]:hidden',
                          item.startDate || item.endDate ? 'text-body' : 'text-muted-foreground',
                        )}
                      >
                        {formatDates(item.startDate, item.endDate)}
                      </span>
                      <Button variant="ghost" size="icon-sm" onClick={() => onDelete(item)} aria-label={`Delete ${item.title}`}>
                        <Trash2 aria-hidden />
                      </Button>
                    </SortableRow>
                  )
                })}
              </SortableContext>
            </ul>
          </section>
        ))}
      </DndContext>
      <p className="mt-3 text-[13px] text-muted-foreground">
        Drag rows to set the order Readers see in the List view.
        {lanes && ' Swimlanes only change this layout, never the order.'}
      </p>
    </div>
  )
}

function SortableRow({
  item,
  className,
  onHandleKey,
  children,
}: {
  item: Item
  className: string
  onHandleKey: (event: React.KeyboardEvent) => void
  children: React.ReactNode
}) {
  const { setNodeRef, setActivatorNodeRef, listeners, transform, transition, isDragging } = useSortable({
    id: item.id,
  })
  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={cn(
        className,
        'relative border-b border-hairline-soft bg-background px-1 py-2.5 transition-colors hover:bg-surface-soft',
        isDragging && 'z-10 opacity-40',
      )}
    >
      <button
        ref={setActivatorNodeRef}
        {...listeners}
        data-handle={item.id}
        onKeyDown={onHandleKey}
        aria-label={`Move ${item.title}. Use the up and down arrow keys.`}
        title="Drag to reorder"
        className="flex h-8 w-7 cursor-grab touch-none items-center justify-center rounded-sm text-[#9ca3af] hover:bg-surface-card hover:text-foreground active:cursor-grabbing"
      >
        <GripVertical className="size-4" aria-hidden />
      </button>
      {children}
    </li>
  )
}
