import { Trash2 } from 'lucide-react'

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
  /** Renders the drag handle cell of a row (Task 14); empty by default. */
  renderHandle?: (item: Item) => React.ReactNode
  /** Wraps each lane's rows, e.g. in a sortable context. */
  renderLane?: (items: Item[], rows: React.ReactNode) => React.ReactNode
  /** Wraps a row, e.g. to make it sortable. */
  renderRow?: (item: Item, row: React.ReactElement) => React.ReactNode
}

/** The editor's list view: Items in manual order, optionally in swimlanes by Category. */
export function ItemList({
  items,
  categories,
  lanes,
  onEdit,
  onDelete,
  renderHandle = () => <span />,
  renderLane = (_, rows) => rows,
  renderRow = (_, row) => row,
}: ItemListProps) {
  const categoryOf = (item: Item) => categories.find((c) => c.id === item.categoryId) ?? null
  const groups = lanes ? lanesByCategory(items, categories) : [{ category: null, items }]

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
            {renderLane(
              group.items,
              group.items.map((item) => {
                const category = categoryOf(item)
                return renderRow(
                  item,
                  <li
                    key={item.id}
                    className={cn(
                      grid,
                      lanes && gridLanes,
                      'border-b border-hairline-soft bg-background px-1 py-2.5 transition-colors hover:bg-surface-soft',
                    )}
                  >
                    {renderHandle(item)}
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
                  </li>,
                )
              }),
            )}
          </ul>
        </section>
      ))}
      <p className="mt-3 text-[13px] text-muted-foreground">
        Drag rows to set the order Readers see in the List view.
        {lanes && ' Swimlanes only change this layout, never the order.'}
      </p>
    </div>
  )
}
