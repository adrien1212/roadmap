import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Mail, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

import { FieldError } from '#/components/field-error'
import { Button } from '#/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '#/components/ui/dialog'
import { Label } from '#/components/ui/label'
import { Input } from '#/components/ui/input'
import { Switch } from '#/components/ui/switch'
import { Textarea } from '#/components/ui/textarea'
import type { Category } from '#/lib/categories'
import {
  STATUSES,
  STATUS_LABEL,
  createItem,
  emptyDraft,
  invalidateItems,
  replaceItem,
  type Item,
  type ItemDraft,
} from '#/lib/items'
import { formatInstant } from '#/lib/format'
import { cn } from '#/lib/utils'

type FieldProblem = { field: 'title' | 'end' | 'form'; message: string }

const hint = 'm-0 text-[13px] text-muted-foreground'
const label = 'text-[13px] font-medium text-body'

/** Create / edit an Item in a dialog centred on the screen. `item` is null for a new Item. */
export function ItemDialog({
  roadmapId,
  item,
  open,
  categories,
  onOpenChange,
  onDelete,
  emailsOnShip,
}: {
  roadmapId: number
  item: Item | null
  open: boolean
  categories: Category[]
  onOpenChange: (open: boolean) => void
  onDelete: (item: Item) => void
  /** How many subscribed Readers an Item becoming Shipped would email right now (0 when it wouldn't). */
  emailsOnShip: number
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {/* Header and footer stay put; the fields scroll when the screen is short. */}
      <DialogContent className="flex max-h-[calc(100dvh-2rem)] flex-col gap-0 p-0 sm:max-w-[560px]">
        {open && (
          // Remount per Item so the form starts from that Item's values.
          <ItemForm
            key={item?.id ?? 'new'}
            roadmapId={roadmapId}
            item={item}
            categories={categories}
            onClose={() => onOpenChange(false)}
            onDelete={onDelete}
            emailsOnShip={emailsOnShip}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function ItemForm({
  roadmapId,
  item,
  categories,
  onClose,
  onDelete,
  emailsOnShip,
}: {
  roadmapId: number
  item: Item | null
  categories: Category[]
  onClose: () => void
  onDelete: (item: Item) => void
  emailsOnShip: number
}) {
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState<ItemDraft>(item ? toDraft(item) : emptyDraft)
  const [problem, setProblem] = useState<FieldProblem | null>(null)
  const set = <K extends keyof ItemDraft>(key: K, value: ItemDraft[K]) => setDraft((d) => ({ ...d, [key]: value }))

  const save = useMutation({
    mutationFn: (body: ItemDraft) => (item ? replaceItem(roadmapId, item.id, body) : createItem(roadmapId, body)),
    onSuccess: (saved) => {
      invalidateItems(queryClient, roadmapId)
      toast(item ? 'Saved' : `Added "${saved.title}"`)
      onClose()
    },
    onError: (e) => setProblem({ field: 'form', message: e.message }),
  })

  function submit(event: React.FormEvent) {
    event.preventDefault()
    const title = draft.title.trim()
    if (!title) {
      setProblem({ field: 'title', message: 'Give the Item a title.' })
      document.getElementById('f-title')?.focus()
      return
    }
    if (draft.startDate && draft.endDate && draft.endDate < draft.startDate) {
      setProblem({ field: 'end', message: 'The end date is before the start date. Move one of them.' })
      document.getElementById('f-end')?.focus()
      return
    }
    setProblem(null)
    save.mutate({ ...draft, title, description: draft.description || null })
  }

  return (
    <>
      <header className="flex items-center justify-between border-b border-hairline-soft px-6 pt-5 pb-4 pr-14">
        <DialogTitle className="font-display text-[22px] leading-[1.3] font-semibold tracking-[-0.3px]">
          {item ? 'Edit Item' : 'New Item'}
        </DialogTitle>
        <DialogDescription className="sr-only">All fields of the Item.</DialogDescription>
      </header>

      <form id="item-form" noValidate onSubmit={submit} className="flex flex-1 flex-col gap-5 overflow-y-auto p-6">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="f-title" className={label}>
            Title
          </Label>
          <Input
            id="f-title"
            autoFocus
            value={draft.title}
            maxLength={140}
            onChange={(e) => set('title', e.target.value)}
            aria-invalid={problem?.field === 'title' || undefined}
            aria-describedby={problem?.field === 'title' ? 'e-title' : undefined}
          />
          {problem?.field === 'title' && <FieldError id="e-title">{problem.message}</FieldError>}
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="f-desc" className={label}>
            Description
          </Label>
          <Textarea
            id="f-desc"
            value={draft.description ?? ''}
            maxLength={10_000}
            onChange={(e) => set('description', e.target.value)}
            placeholder="What is it, in plain words?"
          />
          <p className={hint}>Plain text. Line breaks are kept.</p>
        </div>

        <div className="flex flex-col gap-1.5">
          <span id="l-status" className={label}>
            Status
          </span>
          <div role="radiogroup" aria-labelledby="l-status" className="flex gap-0.5 rounded-md bg-surface-card p-[3px]">
            {STATUSES.map((status) => (
              <button
                key={status}
                type="button"
                role="radio"
                aria-checked={draft.status === status}
                onClick={() => set('status', status)}
                className="h-[34px] flex-1 cursor-pointer rounded-[6px] text-[13px] font-medium text-muted-foreground transition-colors hover:text-foreground aria-checked:bg-background aria-checked:font-semibold aria-checked:text-foreground aria-checked:shadow-soft"
              >
                {STATUS_LABEL[status]}
              </button>
            ))}
          </div>
          {emailsOnShip > 0 && draft.status === 'shipped' && !draft.internal && item?.status !== 'shipped' ? (
            <p className="m-0 flex items-center gap-1.5 text-[13px] text-body">
              <Mail className="size-4 shrink-0" aria-hidden />
              {emailsOnShip === 1
                ? '1 subscribed Reader gets an email about this Item when you save.'
                : `${emailsOnShip} subscribed Readers get an email about this Item when you save.`}
            </p>
          ) : (
            <p className={hint}>Set by hand. It never changes when a date passes.</p>
          )}
          {item?.shippedAt && draft.status === 'shipped' && (
            <p className="m-0 text-[13px] font-medium text-success-text">Shipped {formatInstant(item.shippedAt)}</p>
          )}
        </div>

        <div>
          <div className="grid grid-cols-2 gap-3">
            {(['startDate', 'endDate'] as const).map((key) => {
              const id = key === 'startDate' ? 'f-start' : 'f-end'
              const invalid = key === 'endDate' && problem?.field === 'end'
              return (
                <div key={key} className="flex min-w-0 flex-col gap-1.5">
                  <Label htmlFor={id} className={label}>
                    {key === 'startDate' ? 'Start date' : 'End date'}{' '}
                    <span className="font-normal text-muted-foreground">(optional)</span>
                  </Label>
                  <Input
                    id={id}
                    type="date"
                    value={draft[key] ?? ''}
                    onChange={(e) => set(key, e.target.value || null)}
                    aria-invalid={invalid || undefined}
                    aria-describedby={invalid ? 'e-end' : undefined}
                    className="pr-2 tabular-nums"
                  />
                </div>
              )
            })}
          </div>
          <div className="mt-2">
            {problem?.field === 'end' ? (
              <FieldError id="e-end">{problem.message}</FieldError>
            ) : (
              <p className={hint}>Set both, one, or neither. One date shows as a point on the timeline.</p>
            )}
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="f-cat" className={label}>
            Category
          </Label>
          <select
            id="f-cat"
            value={draft.categoryId ?? ''}
            onChange={(e) => set('categoryId', e.target.value ? Number(e.target.value) : null)}
            className={cn(
              "h-10 w-full cursor-pointer appearance-none rounded-md border border-border bg-background bg-[url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='none' stroke='%236b7280' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m4 6 4 4 4-4'/%3E%3C/svg%3E\")] bg-[right_12px_center] bg-no-repeat pr-9 pl-3.5 text-[15px] outline-none hover:border-[#d1d5db] focus:border-foreground focus:ring-[3px] focus:ring-[rgba(17,17,17,0.08)]",
            )}
          >
            <option value="">No Category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        <label className="flex cursor-pointer items-start gap-3">
          <Switch checked={draft.internal} onCheckedChange={(checked) => set('internal', checked)} className="mt-px" />
          <span>
            <span className="block text-sm font-semibold">Internal</span>
            <span className={hint}>Shown here in the editor, left off the Public page.</span>
          </span>
        </label>

        {problem?.field === 'form' && <FieldError>{problem.message}</FieldError>}
      </form>

      <footer className="flex items-center gap-3 border-t border-hairline-soft px-6 py-4">
        {item && (
          <Button variant="danger" size="icon" aria-label="Delete Item" onClick={() => onDelete(item)}>
            <Trash2 aria-hidden />
          </Button>
        )}
        <span className="flex-1" />
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" form="item-form" disabled={save.isPending}>
          {item ? 'Save' : 'Add Item'}
        </Button>
      </footer>
    </>
  )
}

function toDraft({ title, description, startDate, endDate, status, categoryId, internal }: Item): ItemDraft {
  return { title, description, startDate, endDate, status, categoryId, internal }
}
