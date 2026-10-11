import { Toaster as Sonner, type ToasterProps } from "sonner"

/**
 * Confirmation and Undo toasts, styled like the toast of design/roadmap-ui.html.
 * Light only, so no theme provider. Show one with `toast(...)` from "sonner";
 * Undo is `toast("Item deleted", { action: { label: "Undo", onClick } })`.
 */
const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <Sonner
      position="bottom-center"
      duration={5000}
      toastOptions={{
        unstyled: true,
        classNames: {
          toast:
            "flex items-center gap-3.5 rounded-md bg-surface-dark py-2.5 pr-3.5 pl-4 text-sm text-white shadow-overlay",
          actionButton:
            "ml-auto cursor-pointer p-1 font-semibold underline underline-offset-3",
        },
      }}
      {...props}
    />
  )
}

export { Toaster }
