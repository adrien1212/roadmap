package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.item.ItemStatus;
import fr.adriencaubel.roadmap.smtp.SmtpState;

import java.util.List;
import java.util.stream.Stream;

/** Who gets an email about an Item that became Shipped, decided on its state once the change is committed. */
final class ShippedEmails {

	private ShippedEmails() {
	}

	/**
	 * The Roadmap's Subscriptions, then those waiting for this Item: confirmed only, one email per Subscription,
	 * never merged (an address on both gets two). Nobody for an Internal Item, a Private Roadmap, an Item no
	 * longer Shipped, or no working SMTP.
	 */
	static List<Subscription> recipients(Item item, SmtpState smtpState, List<Subscription> roadmapSubscriptions,
			List<Subscription> itemSubscriptions) {
		boolean visibleAndShipped = item.getStatus() == ItemStatus.SHIPPED && !item.isInternal()
				&& item.getRoadmap().getPublishedAt() != null;
		if (!visibleAndShipped || smtpState != SmtpState.CONNECTED) {
			return List.of();
		}
		Stream<Subscription> onRoadmap = roadmapSubscriptions.stream().filter(s -> !s.isForItem());
		Stream<Subscription> onItem = itemSubscriptions.stream().filter(s -> s.isForItem() && covers(s, item));
		return Stream.concat(onRoadmap, onItem).filter(Subscription::isConfirmed).toList();
	}

	private static boolean covers(Subscription subscription, Item item) {
		Item covered = subscription.getItem();
		return covered == item || (item.getId() != null && item.getId().equals(covered.getId()));
	}
}
