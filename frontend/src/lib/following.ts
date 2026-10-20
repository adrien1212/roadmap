import { useEffect, useState } from 'react'

/**
 * What this browser confirmed it follows: per publicId, the Roadmap and/or Item ids. Kept in the Reader's own
 * localStorage only, never the token or the address, and never sent anywhere (SPEC-item-updates.md, "Already
 * subscribed"). A convenience, not the truth: it goes stale if the Reader unsubscribes elsewhere.
 */
const KEY = 'roadmap.following'

interface Followed {
  roadmap?: boolean
  items?: number[]
}

function read(): Record<string, Followed> {
  try {
    const value = JSON.parse(localStorage.getItem(KEY) ?? '{}')
    return value && typeof value === 'object' ? value : {}
  } catch {
    return {}
  }
}

function write(all: Record<string, Followed>) {
  try {
    localStorage.setItem(KEY, JSON.stringify(all))
  } catch {
    // Private window or blocked storage: the pages simply don't remember.
  }
}

/** After the confirm page succeeds; itemId null for a Roadmap Subscription. */
export function rememberFollowing(publicId: string, itemId: number | null) {
  const all = read()
  const entry = all[publicId] ?? {}
  if (itemId === null) entry.roadmap = true
  else entry.items = [...new Set([...(entry.items ?? []), itemId])]
  all[publicId] = entry
  write(all)
}

/** After the unsubscribe page succeeds. */
export function forgetFollowing(publicId: string, itemId: number | null) {
  const all = read()
  const entry = all[publicId]
  if (!entry) return
  if (itemId === null) delete entry.roadmap
  else entry.items = (entry.items ?? []).filter((id) => id !== itemId)
  if (!entry.roadmap && !entry.items?.length) delete all[publicId]
  write(all)
}

/** Whether this browser confirmed the Roadmap (itemId null) or this Item. Read after hydration, never on the server. */
export function useFollowing(publicId: string, itemId: number | null): boolean {
  const [following, setFollowing] = useState(false)
  useEffect(() => {
    const entry = read()[publicId]
    setFollowing(itemId === null ? Boolean(entry?.roadmap) : Boolean(entry?.items?.includes(itemId)))
  }, [publicId, itemId])
  return following
}
