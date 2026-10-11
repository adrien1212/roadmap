import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { FieldError } from '#/components/field-error'
import { Swatches } from '#/components/swatches'
import { Button } from '#/components/ui/button'
import { Input } from '#/components/ui/input'
import { createCategory, nextColour, type Category, type Colour } from '#/lib/categories'
import { categoryNameError, categorySaveError } from '#/lib/category-name'

/** Inline "new Category" form, on the Categories screen and in the editor legend. */
export function CategoryForm({ categories, onClose }: { categories: Category[]; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [colour, setColour] = useState<Colour>(nextColour(categories.length))
  const [error, setError] = useState<string | null>(null)

  const create = useMutation({
    mutationFn: createCategory,
    onSuccess: (category) => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      toast(`Added Category "${category.name}"`)
      onClose()
    },
    onError: (e) => setError(categorySaveError(e, name)),
  })

  function submit(event: React.FormEvent) {
    event.preventDefault()
    const problem = categoryNameError(name, categories)
    if (problem) return setError(problem)
    create.mutate({ name: name.trim(), colour })
  }

  return (
    <form
      onSubmit={submit}
      onKeyDown={(e) => e.key === 'Escape' && onClose()}
      className="my-3 flex flex-wrap items-center gap-3 rounded-md bg-surface-soft p-3"
    >
      <label htmlFor="cat-name" className="sr-only">
        Category name
      </label>
      <Input
        id="cat-name"
        autoFocus
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Category name"
        maxLength={60}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? 'cat-err' : undefined}
        className="w-[220px]"
      />
      <Swatches value={colour} onChange={setColour} />
      <Button type="submit" size="sm" disabled={create.isPending}>
        Add Category
      </Button>
      <Button type="button" variant="ghost" size="sm" onClick={onClose}>
        Cancel
      </Button>
      {error && <FieldError id="cat-err">{error}</FieldError>}
    </form>
  )
}
