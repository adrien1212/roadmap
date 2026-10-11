import { useState } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

import { CategoryForm } from '#/components/category-form'
import { CategoryChip } from '#/components/chips'
import { ConfirmDialog } from '#/components/confirm-dialog'
import { FieldError } from '#/components/field-error'
import { Swatches } from '#/components/swatches'
import { Button } from '#/components/ui/button'
import { Input } from '#/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '#/components/ui/popover'
import {
  categoriesQuery,
  colourVar,
  deleteCategory,
  updateCategory,
  type CategoryWithCount,
  type Colour,
} from '#/lib/categories'
import { categoryNameError, categorySaveError } from '#/lib/category-name'
import { plural } from '#/lib/format'

export const Route = createFileRoute('/_app/categories')({
  loader: ({ context }) => context.queryClient.ensureQueryData(categoriesQuery),
  component: CategoriesPage,
})

function CategoriesPage() {
  const { data: categories } = useSuspenseQuery(categoriesQuery)
  const queryClient = useQueryClient()
  const [adding, setAdding] = useState(false)
  const [deleting, setDeleting] = useState<CategoryWithCount | null>(null)

  const remove = useMutation({
    mutationFn: (category: CategoryWithCount) => deleteCategory(category.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      // Items that used it now have no Category.
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] })
      toast('Category deleted')
    },
    onError: (e) => toast(e.message),
  })

  return (
    <main className="mx-auto max-w-[860px] px-4 pb-30 sm:px-8">
      <div className="mt-12 mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-[28px] leading-tight font-semibold tracking-[-0.5px]">Categories</h1>
          <p className="mt-1.5 text-body">Shared by all your Roadmaps. Readers can filter by them on a Public page.</p>
        </div>
        {!adding && (
          <Button onClick={() => setAdding(true)}>
            <Plus aria-hidden />
            New Category
          </Button>
        )}
      </div>

      {adding && <CategoryForm categories={categories} onClose={() => setAdding(false)} />}

      {categories.length ? (
        <ul className="border-t border-hairline-soft">
          {categories.map((category) => (
            <CategoryRow
              key={category.id}
              category={category}
              categories={categories}
              onDelete={() => setDeleting(category)}
            />
          ))}
        </ul>
      ) : (
        !adding && (
          <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[#d1d5db] px-6 py-14 text-center">
            <h2 className="font-display text-[22px] font-semibold tracking-[-0.3px]">No Categories yet</h2>
            <p className="max-w-[420px] text-body">
              Categories label Items with a name and a colour, like Security or Integrations.
            </p>
          </div>
        )
      )}

      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={`Delete ${deleting?.name}?`}
        body={
          deleting?.itemCount
            ? `It's cleared from ${plural(deleting.itemCount, 'Item')} across your Roadmaps. The Items themselves stay.`
            : 'No Items use it.'
        }
        cta="Delete Category"
        onConfirm={() => deleting && remove.mutate(deleting)}
      />
    </main>
  )
}

function CategoryRow({
  category,
  categories,
  onDelete,
}: {
  category: CategoryWithCount
  categories: CategoryWithCount[]
  onDelete: () => void
}) {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(category.name)
  const [error, setError] = useState<string | null>(null)
  const [colourOpen, setColourOpen] = useState(false)

  const update = useMutation({
    mutationFn: (body: { name?: string; colour?: Colour }) => updateCategory(category.id, body),
    onSuccess: (_, body) => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      if (body.name) {
        setEditing(false)
        toast('Category renamed')
      }
    },
    onError: (e, body) => (body.name ? setError(categorySaveError(e, name)) : toast(e.message)),
  })

  function startEditing() {
    setName(category.name)
    setError(null)
    setEditing(true)
  }

  function save() {
    const problem = categoryNameError(name, categories, category.id)
    if (problem) return setError(problem)
    update.mutate({ name: name.trim() })
  }

  return (
    <li className="grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-4 border-b border-hairline-soft px-1 py-3.5">
      <div className="flex min-w-0 items-center gap-3">
        <Popover open={colourOpen} onOpenChange={setColourOpen}>
          <PopoverTrigger
            aria-label={`Change colour of ${category.name}`}
            className="size-[22px] shrink-0 cursor-pointer rounded-full shadow-[inset_0_0_0_1px_rgba(0,0,0,0.08)]"
            style={{ background: colourVar(category.colour) }}
          />
          <PopoverContent align="start" className="w-auto rounded-lg p-2.5 shadow-overlay">
            <Swatches
              value={category.colour}
              onChange={(colour) => {
                setColourOpen(false)
                if (colour !== category.colour) update.mutate({ colour })
              }}
              className="grid grid-cols-4 gap-2"
            />
          </PopoverContent>
        </Popover>
        {editing ? (
          <div className="flex flex-1 flex-col gap-1.5">
            <Input
              autoFocus
              onFocus={(e) => e.target.select()}
              value={name}
              onChange={(e) => setName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') save()
                if (e.key === 'Escape') setEditing(false)
              }}
              maxLength={60}
              aria-label="Category name"
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? `cat-edit-err-${category.id}` : undefined}
              className="h-9 max-w-[320px]"
            />
            {error && <FieldError id={`cat-edit-err-${category.id}`}>{error}</FieldError>}
          </div>
        ) : (
          <CategoryChip name={category.name} colour={category.colour} />
        )}
      </div>
      <span className="text-sm text-muted-foreground tabular-nums">{plural(category.itemCount, 'Item')}</span>
      <div className="flex gap-1">
        {editing ? (
          <>
            <Button size="sm" onClick={save} disabled={update.isPending}>
              Save
            </Button>
            <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </>
        ) : (
          <>
            <Button size="sm" variant="ghost" onClick={startEditing}>
              <Pencil aria-hidden />
              Rename
            </Button>
            <Button size="icon-sm" variant="ghost" onClick={onDelete} aria-label={`Delete ${category.name}`}>
              <Trash2 aria-hidden />
            </Button>
          </>
        )}
      </div>
    </li>
  )
}
