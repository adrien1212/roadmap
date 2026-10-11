import { ApiError } from '#/lib/api'
import type { Category } from '#/lib/categories'

/** Client-side check before saving: required, and unique ignoring case. Null when fine. */
export function categoryNameError(name: string, categories: Category[], exceptId?: number): string | null {
  const trimmed = name.trim()
  if (!trimmed) return 'Name the Category.'
  const clash = categories.some((c) => c.id !== exceptId && c.name.toLowerCase() === trimmed.toLowerCase())
  return clash ? duplicateMessage(trimmed) : null
}

/** What to show when saving failed; the backend also rejects duplicates (ignoring case and accents). */
export function categorySaveError(error: unknown, name: string): string {
  if (error instanceof ApiError && error.status === 409) return duplicateMessage(name.trim())
  return error instanceof Error ? error.message : 'Something went wrong. Try again.'
}

const duplicateMessage = (name: string) => `You already have a Category called "${name}".`
