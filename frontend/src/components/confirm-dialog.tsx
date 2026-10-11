import { Button } from '#/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '#/components/ui/dialog'

/** "Delete X?" with a destructive call to action, as in design/roadmap-ui.html. */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  body,
  cta,
  onConfirm,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  body: string
  cta: string
  onConfirm: () => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent role="alertdialog">
        <DialogTitle className="pr-8 font-display text-[22px] leading-[1.3] font-semibold tracking-[-0.3px]">
          {title}
        </DialogTitle>
        <DialogDescription className="text-sm text-body">{body}</DialogDescription>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            variant="destructive"
            onClick={() => {
              onOpenChange(false)
              onConfirm()
            }}
          >
            {cta}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
