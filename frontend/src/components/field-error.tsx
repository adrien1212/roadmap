import { CircleAlert } from 'lucide-react'

export function FieldError({ id, children }: { id?: string; children: React.ReactNode }) {
  return (
    <p id={id} className="m-0 flex w-full items-center gap-1.5 text-[13px] text-error-text">
      <CircleAlert className="size-4 shrink-0" aria-hidden />
      {children}
    </p>
  )
}
