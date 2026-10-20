import { EmailForm } from '#/components/recently-shipped'
import { publicPath } from '#/lib/roadmaps'
import { subscribeToItem } from '#/lib/public-page'

/**
 * "Notify me when this ships" on the Item page: an Item Subscription, emailed each time this Item becomes
 * Shipped until the Reader unsubscribes. Shown only while subscriptions are open and the Item is not Shipped.
 */
export function NotifyMe({
  publicId,
  itemId,
  roadmapName,
  brandName,
}: {
  publicId: string
  itemId: number
  roadmapName: string
  brandName: string | null
}) {
  return (
    <aside aria-labelledby="reader-updates" className="flex flex-col gap-3 rounded-lg bg-surface-soft p-5">
      <EmailForm
        heading="Notify me when this ships"
        cta="Notify me"
        hint={`An email when this Item ships, from ${brandName ?? 'the owner of this Roadmap'}. They receive your email address. Unsubscribe in one click.`}
        done="Check your inbox. Confirm your email to hear when this Item ships."
        send={(email) => subscribeToItem(publicId, itemId, email)}
      />
      <a
        href={publicPath({ publicId })}
        className="text-[13px] font-medium text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
      >
        Or get every update from {roadmapName}
      </a>
    </aside>
  )
}
