import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "#/lib/utils"
import { Slot } from "radix-ui"

// Restyled to the buttons of design/roadmap-ui.html (.btn, .btn-*).
const buttonVariants = cva(
  "inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 rounded-md border border-transparent text-sm leading-none font-semibold whitespace-nowrap no-underline transition-colors outline-none disabled:cursor-not-allowed disabled:border-transparent disabled:bg-surface-strong disabled:text-muted-foreground [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-[#242424] active:bg-black",
        secondary: "border-border bg-background text-foreground hover:border-[#d1d5db] hover:bg-surface-soft",
        ghost: "bg-transparent px-2.5 text-body hover:bg-surface-card hover:text-foreground",
        danger: "border-[#fecaca] bg-white text-error-text hover:bg-[#fef2f2]",
        destructive: "bg-error-text text-white hover:bg-[#991b1b]",
        link: "text-foreground underline-offset-4 hover:underline",
      },
      size: {
        default: "h-10 px-[18px]",
        sm: "h-8 px-3 text-[13px]",
        lg: "h-11 px-5",
        icon: "size-9 px-0",
        "icon-sm": "size-8 px-0",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
