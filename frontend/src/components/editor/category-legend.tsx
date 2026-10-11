import { useState } from 'react'
import { Plus } from 'lucide-react'

import { CategoryForm } from '#/components/category-form'
import { CategoryChip } from '#/components/chips'
import type { Category } from '#/lib/categories'
import type { Item } from '#/lib/items'

/** The Owner's Categories with how many Items of this Roadmap use each, and an inline "+ Category". */
export function CategoryLegend({ categories, items }: { categories: Category[]; items: Item[] }) {
  const [adding, setAdding] = useState(false)
  const count = (category: Category) => items.filter((item) => item.categoryId === category.id).length

  return (
    <>
      <div aria-label="Categories" className="mt-5 mb-1 flex flex-wrap items-center gap-2">
        {categories.map((category) => (
          <CategoryChip key={category.id} name={category.name} colour={category.colour}>
            <span className="font-normal tabular-nums opacity-75">{count(category)}</span>
          </CategoryChip>
        ))}
        {!adding && (
          <button
            onClick={() => setAdding(true)}
            className="inline-flex h-7 cursor-pointer items-center gap-1 rounded-full border border-dashed border-[#d1d5db] px-3 text-[13px] font-medium text-body hover:border-foreground hover:text-foreground"
          >
            <Plus className="size-4" aria-hidden />
            Category
          </button>
        )}
      </div>
      {adding && <CategoryForm categories={categories} onClose={() => setAdding(false)} />}
    </>
  )
}
