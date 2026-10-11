import { CategoryChip, CategoryDot, InternalChip, NeutralChip, StatusChip } from '#/components/chips'
import { StatusKey, YearTimeline } from '#/components/year-timeline'
import type { Category } from '#/lib/categories'
import { lanesByCategory, type Item } from '#/lib/items'

/** The editor's timeline view; undated Items are listed below it under "No dates". */
export function ItemTimeline({
  items,
  categories,
  lanes,
  year,
  onEdit,
}: {
  items: Item[]
  categories: Category[]
  lanes: boolean
  year: number
  onEdit: (item: Item) => void
}) {
  const categoryOf = (item: Item) => categories.find((c) => c.id === item.categoryId) ?? null
  const dated = items.filter((item) => item.startDate || item.endDate)
  const undated = items.filter((item) => !item.startDate && !item.endDate)
  const groups = lanes
    ? lanesByCategory(dated, categories).map((lane) => ({
        key: String(lane.category?.id ?? 'none'),
        header: lane.category ? (
          <CategoryChip name={lane.category.name} colour={lane.category.colour} />
        ) : (
          <NeutralChip>No Category</NeutralChip>
        ),
        items: lane.items,
      }))
    : [{ key: 'all', items: dated }]

  return (
    <>
      <div className="mt-2">
        <YearTimeline
          year={year}
          groups={groups}
          stickyHeaderClassName="top-0 min-[640px]:top-[61px]"
          onLabelClick={onEdit}
          isStriped={(item) => item.internal}
          renderLabel={(item) => (
            <>
              <CategoryDot colour={categoryOf(item)?.colour ?? null} />
              <span className="truncate">{item.title}</span>
              {item.internal && <InternalChip />}
            </>
          )}
          empty={<p className="px-1 py-6 text-sm text-muted-foreground">No Items have dates yet.</p>}
        />
      </div>
      <div className="mt-4 flex flex-wrap justify-between gap-4">
        <StatusKey />
        <span className="text-[13px] font-medium text-muted-foreground">Striped marks are Internal.</span>
      </div>

      {undated.length > 0 && (
        <section className="mt-7">
          <h2 className="text-base font-semibold">
            No dates <span className="font-normal text-muted-foreground tabular-nums">{undated.length}</span>
          </h2>
          <p className="m-0 text-[13px] text-muted-foreground">Not on the timeline. Readers see them in the List view.</p>
          <ul className="mt-2 flex flex-wrap gap-2">
            {undated.map((item) => (
              <li key={item.id}>
                <button
                  onClick={() => onEdit(item)}
                  className="inline-flex cursor-pointer items-center gap-2 rounded-md border border-dashed border-[#d1d5db] px-3 py-1.5 text-sm font-medium hover:border-foreground"
                >
                  <CategoryDot colour={categoryOf(item)?.colour ?? null} />
                  {item.title}
                  <StatusChip status={item.status} />
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  )
}
