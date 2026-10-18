import { useState } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'
import { toast } from 'sonner'

import { BrandMark } from '#/components/brand-mark'
import { InviteDialog } from '#/components/invite-dialog'
import { FieldError } from '#/components/field-error'
import { SmtpSettingsForm } from '#/components/smtp-settings-form'
import { Button } from '#/components/ui/button'
import { Input } from '#/components/ui/input'
import { Label } from '#/components/ui/label'
import { BRAND_MAX_LENGTH, brandQuery, pageTitle, previewDescription, saveBrand } from '#/lib/brand'
import { formatInstant } from '#/lib/format'
import { meQuery } from '#/lib/me'
import { roadmapsQuery } from '#/lib/roadmaps'
import { smtpSettingsQuery } from '#/lib/smtp'

export const Route = createFileRoute('/_app/settings')({
  loader: ({ context: { queryClient } }) =>
    Promise.all([
      queryClient.ensureQueryData(brandQuery),
      queryClient.ensureQueryData(roadmapsQuery),
      queryClient.ensureQueryData(smtpSettingsQuery),
    ]),
  component: SettingsPage,
})

const hint = 'm-0 text-[13px] text-muted-foreground'
const panel = 'flex flex-col gap-5 rounded-lg border border-border p-6'

function SettingsPage() {
  const { data: brand } = useSuspenseQuery(brandQuery)
  const { data: roadmaps } = useSuspenseQuery(roadmapsQuery)
  const { data: smtp } = useSuspenseQuery(smtpSettingsQuery)
  const { data: me } = useSuspenseQuery(meQuery)
  const [inviting, setInviting] = useState(false)
  const queryClient = useQueryClient()
  const [name, setName] = useState(brand.name ?? '')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => saveBrand(name),
    onSuccess: (saved) => {
      queryClient.setQueryData(brandQuery.queryKey, saved)
      queryClient.invalidateQueries({ queryKey: ['public'] })
      setName(saved.name ?? '')
      setError(null)
      toast('Settings saved')
    },
    onError: (e) => setError(e.message),
  })

  // The preview follows what is typed, before it is saved.
  const previewName = name.trim() || null
  const sample = roadmaps.find((r) => r.publishedAt) ?? roadmaps[0]
  const sampleName = sample?.name ?? 'Product roadmap'

  return (
    <main className="mx-auto max-w-[1000px] px-4 pb-30 sm:px-8">
      <div className="mt-12 mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-[28px] leading-tight font-semibold tracking-[-0.5px]">Settings</h1>
          <p className="mt-1.5 text-body">Your product, as Readers see it on every Public page and in emails.</p>
        </div>
        {/* Temporary: moves into the People section (organisation Task 14). */}
        <Button variant="secondary" onClick={() => setInviting(true)}>
          Invite
        </Button>
      </div>
      <InviteDialog open={inviting} onOpenChange={setInviting} organisationName={me.organisation?.name ?? ''} />

      <div className="grid grid-cols-1 items-start gap-8 min-[640px]:grid-cols-2">
        <form
          className={panel}
          onSubmit={(e) => {
            e.preventDefault()
            save.mutate()
          }}
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="b-name" className="text-[13px] font-medium text-body">
              Product name
            </Label>
            <Input
              id="b-name"
              value={name}
              maxLength={BRAND_MAX_LENGTH}
              onChange={(e) => setName(e.target.value)}
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? 'b-err' : 'b-hint'}
            />
            {error ? (
              <FieldError id="b-err">{error}</FieldError>
            ) : (
              <p id="b-hint" className={hint}>
                Shown above every Roadmap name, and as the sender of emails.
              </p>
            )}
          </div>
          <p className={hint}>Leave it empty to show the Roadmap name alone.</p>
          <p className={hint}>Public pages always end with "Made with Product name".</p>
          <div>
            <Button type="submit" disabled={save.isPending}>
              Save
            </Button>
          </div>
        </form>

        <section aria-label="Preview" className="flex flex-col gap-4">
          <span className="text-[13px] font-medium text-body">Public page</span>
          <div className={`${panel} gap-2.5`}>
            {previewName && <BrandMark name={previewName} />}
            <span className="font-display text-[22px] leading-[1.3] font-semibold tracking-[-0.3px]">{sampleName}</span>
            <span className="text-[13px] font-medium text-muted-foreground" suppressHydrationWarning>
              {sample?.publishedAt ? `Published ${formatInstant(sample.publishedAt)}` : 'Private'}
            </span>
          </div>

          <span className="text-[13px] font-medium text-body">Link preview when the Public page is shared</span>
          <div className="flex flex-col gap-0.5 rounded-lg border border-border px-3.5 py-3">
            <span className="text-[13px] text-muted-foreground" suppressHydrationWarning>
              {typeof window === 'undefined' ? '' : window.location.host}
            </span>
            <span className="text-base font-semibold">{pageTitle(sampleName, previewName)}</span>
            <span className="text-[13px] text-muted-foreground">{previewDescription(previewName)}</span>
          </div>
        </section>

        <SmtpSettingsForm settings={smtp} brandName={brand.name} />
      </div>
    </main>
  )
}
