import { useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'

import { FieldError } from '#/components/field-error'
import { Button } from '#/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '#/components/ui/dialog'
import { Input } from '#/components/ui/input'
import { Label } from '#/components/ui/label'
import { closeOrganisation } from '#/lib/organisation'

const consequences =
  'Every Roadmap becomes Private, so its Public page and Feed stop working, and pending invitations are revoked. ' +
  'Everyone in the Organisation, you included, leaves it. Nothing is deleted.'

/** Role Owner only, at the bottom of Settings; confirmed by typing the Organisation name. */
export function CloseOrganisation({ organisationName }: { organisationName: string }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [typed, setTyped] = useState('')
  const [error, setError] = useState<string | null>(null)

  const close = useMutation({
    mutationFn: () => closeOrganisation(typed),
    onSuccess: async () => {
      queryClient.clear()
      await navigate({ to: '/welcome' })
    },
    onError: (e) => setError(e.message),
  })

  return (
    <section
      aria-labelledby="close-title"
      className="mt-12 flex flex-col gap-4 rounded-lg border border-error/40 p-6"
    >
      <h2 id="close-title" className="font-display text-[20px] leading-tight font-semibold tracking-[-0.3px]">
        Close organisation
      </h2>
      <p className="m-0 max-w-[640px] text-sm text-body">{consequences}</p>
      <div>
        <Button variant="destructive" onClick={() => setOpen(true)}>
          Close organisation
        </Button>
      </div>

      <Dialog
        open={open}
        onOpenChange={(next) => {
          setOpen(next)
          if (!next) {
            setTyped('')
            setError(null)
          }
        }}
      >
        <DialogContent role="alertdialog">
          <form
            className="flex flex-col gap-5"
            onSubmit={(e) => {
              e.preventDefault()
              close.mutate()
            }}
          >
            <DialogTitle className="pr-8 font-display text-[22px] leading-[1.3] font-semibold tracking-[-0.3px]">
              Close {organisationName}?
            </DialogTitle>
            <DialogDescription className="text-sm text-body">{consequences}</DialogDescription>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="close-name" className="text-[13px] font-medium text-body">
                Type <strong>{organisationName}</strong> to confirm
              </Label>
              <Input
                id="close-name"
                autoComplete="off"
                value={typed}
                onChange={(e) => {
                  setTyped(e.target.value)
                  setError(null)
                }}
                aria-invalid={error ? true : undefined}
                aria-describedby={error ? 'close-err' : undefined}
              />
              {error && <FieldError id="close-err">{error}</FieldError>}
            </div>
            <div className="flex justify-end gap-3">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button
                type="submit"
                variant="destructive"
                disabled={typed.trim() !== organisationName || close.isPending}
              >
                Close organisation
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </section>
  )
}
