import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { Check, Clock, Code, Copy, ExternalLink, EyeOff, Globe, Lock, Mail } from 'lucide-react'
import { toast } from 'sonner'

import { Button } from '#/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '#/components/ui/dialog'
import { daysAgo, formatInstant, plural } from '#/lib/format'
import { embedPath, publicPath, publishRoadmap, unpublishRoadmap, type Roadmap } from '#/lib/roadmaps'
import { readersQuery } from '#/lib/subscriptions'

const title = 'pr-8 font-display text-[22px] leading-[1.3] font-semibold tracking-[-0.3px]'

/**
 * Private Roadmap: "Publish X?" with the URL and what publishing means.
 * Published Roadmap: share the link, publish again, view the Public page, or unpublish (confirmed).
 */
export function PublishDialog({
  roadmap,
  internalCount,
  open,
  onOpenChange,
}: {
  roadmap: Roadmap
  internalCount: number
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const [confirmingUnpublish, setConfirmingUnpublish] = useState(false)
  const [copied, setCopied] = useState<'link' | 'embed' | null>(null)
  const readers = useQuery({ ...readersQuery(roadmap.id), enabled: open && roadmap.publishedAt !== null })
  const subscribed = readers.data?.counts.confirmed ?? 0
  useEffect(() => {
    if (!open) setConfirmingUnpublish(false)
  }, [open])

  const origin = typeof window === 'undefined' ? '' : window.location.origin
  const url = origin + publicPath(roadmap)
  const embedCode = `<iframe src="${origin + embedPath(roadmap)}" title="${escapeAttribute(roadmap.name)}" width="100%" height="600" style="border:0" loading="lazy"></iframe>`

  const onSaved = (updated: Roadmap) => {
    queryClient.setQueryData(['roadmaps', roadmap.id], updated)
    queryClient.invalidateQueries({ queryKey: ['roadmaps'], exact: true })
  }

  const publish = useMutation({
    mutationFn: () => publishRoadmap(roadmap.id),
    onSuccess: (updated) => {
      toast(roadmap.publishedAt ? 'Published again. The date now reads today.' : 'Published. Anyone with the link can open it.')
      onSaved(updated)
    },
    onError: (e) => toast(e.message),
  })

  const unpublish = useMutation({
    mutationFn: () => unpublishRoadmap(roadmap.id),
    onSuccess: (updated) => {
      toast('Unpublished. The link now shows "Page not found".')
      onSaved(updated)
      onOpenChange(false)
    },
    onError: (e) => toast(e.message),
  })

  async function copy(what: 'link' | 'embed') {
    try {
      await navigator.clipboard.writeText(what === 'link' ? url : embedCode)
      setCopied(what)
      setTimeout(() => setCopied(null), 1600)
    } catch {
      toast(`Copy failed. Select the ${what === 'link' ? 'link' : 'code'} and copy it.`)
    }
  }

  const urlBox = (
    <code className="min-w-0 flex-1 truncate rounded-md border border-hairline-soft bg-surface-soft px-3 py-2.5 font-mono text-[13px] leading-[1.4]">
      {url}
    </code>
  )

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        {confirmingUnpublish ? (
          <>
            <DialogTitle className={title}>Unpublish {roadmap.name}?</DialogTitle>
            <DialogDescription className="text-sm text-body">
              The link will show "Page not found" until you publish again. When you do, the same link works again.
            </DialogDescription>
            <div className="flex justify-end gap-3">
              <Button variant="secondary" onClick={() => setConfirmingUnpublish(false)}>
                Keep published
              </Button>
              <Button variant="destructive" onClick={() => unpublish.mutate()} disabled={unpublish.isPending}>
                Unpublish
              </Button>
            </div>
          </>
        ) : roadmap.publishedAt ? (
          <>
            <DialogTitle className={title}>{roadmap.name} is published</DialogTitle>
            <div className="flex items-center gap-2">
              {urlBox}
              <Button variant="secondary" onClick={() => copy('link')}>
                {copied === 'link' ? <Check aria-hidden /> : <Copy aria-hidden />}
                {copied === 'link' ? 'Copied' : 'Copy link'}
              </Button>
            </div>
            <DialogDescription className="text-sm text-body">
              Published <span className="tabular-nums">{formatInstant(roadmap.publishedAt)}</span> (
              {daysAgo(roadmap.publishedAt)}). Edits already show to Readers. Publishing again only moves this date to
              today.
            </DialogDescription>
            <div className="flex flex-col gap-2">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm text-body">Embed the Items on your site</span>
                <Button variant="ghost" size="sm" onClick={() => copy('embed')}>
                  {copied === 'embed' ? <Check aria-hidden /> : <Code aria-hidden />}
                  {copied === 'embed' ? 'Copied' : 'Copy embed code'}
                </Button>
              </div>
              <code className="line-clamp-2 rounded-md border border-hairline-soft bg-surface-soft px-3 py-2.5 font-mono text-[12px] leading-[1.5] break-all">
                {embedCode}
              </code>
            </div>
            <p className="flex items-center gap-2 text-sm text-body">
              <Mail className="size-4 shrink-0 text-muted-foreground" aria-hidden />
              <span>
                <span className="tabular-nums">{plural(subscribed, 'Reader')}</span>{' '}
                {subscribed === 1 ? 'is' : 'are'} subscribed.{' '}
                <Link
                  to="/roadmaps/$roadmapId/readers"
                  params={{ roadmapId: roadmap.id }}
                  className="font-medium text-foreground underline"
                  onClick={() => onOpenChange(false)}
                >
                  See subscribed Readers
                </Link>
              </span>
            </p>
            <div className="flex flex-wrap gap-3">
              <Button variant="secondary" className="flex-1" onClick={() => publish.mutate()} disabled={publish.isPending}>
                Publish again
              </Button>
              <Button className="flex-1" asChild>
                <a href={publicPath(roadmap)} target="_blank" rel="noreferrer">
                  <ExternalLink aria-hidden />
                  View Public page
                </a>
              </Button>
            </div>
            <Button variant="ghost" className="self-center text-error-text hover:text-error-text" onClick={() => setConfirmingUnpublish(true)}>
              Unpublish
            </Button>
          </>
        ) : (
          <>
            <DialogTitle className={title}>Publish {roadmap.name}?</DialogTitle>
            <DialogDescription className="text-sm text-body">
              Anyone with this link will be able to open the Roadmap, without signing in.
            </DialogDescription>
            <div className="flex items-center gap-2">{urlBox}</div>
            <ul className="flex flex-col gap-2 rounded-md bg-surface-soft px-3.5 py-3 text-sm text-body">
              <li className="flex items-start gap-2.5">
                <EyeOff className="mt-[3px] size-4 shrink-0 text-muted-foreground" aria-hidden />
                <span>
                  {internalCount
                    ? `${plural(internalCount, 'Internal Item')} ${internalCount === 1 ? 'is' : 'are'} left out.`
                    : 'Internal Items are left out.'}
                </span>
              </li>
              <li className="flex items-start gap-2.5">
                <Clock className="mt-[3px] size-4 shrink-0 text-muted-foreground" aria-hidden />
                <span>Your edits show up right away after this. There's no draft.</span>
              </li>
              <li className="flex items-start gap-2.5">
                <Lock className="mt-[3px] size-4 shrink-0 text-muted-foreground" aria-hidden />
                <span>You can unpublish at any time. The link stays the same.</span>
              </li>
            </ul>
            <div className="flex justify-end gap-3">
              <Button variant="secondary" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button onClick={() => publish.mutate()} disabled={publish.isPending}>
                <Globe aria-hidden />
                Publish
              </Button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

/** The Roadmap name goes into the iframe's title attribute of the embed code. */
function escapeAttribute(text: string) {
  return text.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;')
}
