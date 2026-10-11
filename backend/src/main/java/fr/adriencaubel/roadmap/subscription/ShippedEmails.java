package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.item.ItemStatus;
import fr.adriencaubel.roadmap.smtp.SmtpState;

import java.util.List;

/** Who gets an email about an Item that became Shipped, decided on its state once the change is committed. */
final class ShippedEmails {

	private ShippedEmails() {
	}

	/** Confirmed Readers only; nobody for an Internal Item, a Private Roadmap, an Item no longer Shipped, or no working SMTP. */
	static List<Subscription> recipients(Item item, SmtpState smtpState, List<Subscription> subscriptions) {
		boolean visibleAndShipped = item.getStatus() == ItemStatus.SHIPPED && !item.isInternal()
				&& item.getRoadmap().getPublishedAt() != null;
		if (!visibleAndShipped || smtpState != SmtpState.CONNECTED) {
			return List.of();
		}
		return subscriptions.stream().filter(Subscription::isConfirmed).toList();
	}
}
