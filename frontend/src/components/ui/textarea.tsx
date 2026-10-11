import * as React from "react"
import { cn } from "#/lib/utils"

function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "flex min-h-28 w-full resize-y rounded-md border border-border bg-background px-3.5 py-2.5 text-[15px] leading-normal text-foreground transition-[border-color,box-shadow] outline-none placeholder:text-muted-foreground hover:border-[#d1d5db] focus:border-foreground focus:ring-[3px] focus:ring-[rgba(17,17,17,0.08)] focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-error",
        className
      )}
      {...props}
    />
  )
}

export { Textarea }
