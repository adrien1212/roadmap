import * as React from "react"
import { cn } from "#/lib/utils"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-10 w-full min-w-0 rounded-md border border-border bg-background px-3.5 text-[15px] text-foreground transition-[border-color,box-shadow] outline-none placeholder:text-muted-foreground hover:border-[#d1d5db] disabled:cursor-not-allowed disabled:opacity-50",
        "focus:border-foreground focus:ring-[3px] focus:ring-[rgba(17,17,17,0.08)] focus-visible:outline-none",
        "aria-invalid:border-error aria-invalid:ring-[3px] aria-invalid:ring-[rgba(239,68,68,0.12)]",
        className
      )}
      {...props}
    />
  )
}

export { Input }
