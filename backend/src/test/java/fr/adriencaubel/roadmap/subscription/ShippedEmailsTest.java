package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.item.ItemStatus;
import fr.adriencaubel.roadmap.owner.Owner;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import fr.adriencaubel.roadmap.smtp.SmtpState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShippedEmailsTest {

	private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");

	private final Roadmap roadmap = Roadmap.create(new Owner("dev", "dev", null, null), "Q3");
	private final Subscription confirmed = confirmed("jane@example.com");
	private final Subscription alsoConfirmed = confirmed("joe@example.com");
	private final Subscription pending = Subscription.pending(roadmap, "pending@example.com", NOW);
	private final List<Subscription> all = List.of(confirmed, alsoConfirmed, pending);

	private Subscription confirmed(String email) {
		Subscription subscription = Subscription.pending(roadmap, email, NOW);
		subscription.confirm(NOW);
		return subscription;
	}

	private Item shippedItem(boolean internal) {
		Item item = Item.append(roadmap, "SSO", null);
		item.markInternal(internal);
		item.changeStatus(ItemStatus.SHIPPED, NOW);
		return item;
	}

	@Test
	void emailsEveryConfirmedReaderAndNoPendingOne() {
		roadmap.publish(NOW);

		assertThat(ShippedEmails.recipients(shippedItem(false), SmtpState.CONNECTED, all))
				.containsExactly(confirmed, alsoConfirmed);
	}

	@Test
	void emailsNobodyAboutAnInternalItem() {
		roadmap.publish(NOW);

		assertThat(ShippedEmails.recipients(shippedItem(true), SmtpState.CONNECTED, all)).isEmpty();
	}

	@Test
	void emailsNobodyWhenTheRoadmapIsPrivate() {
		assertThat(ShippedEmails.recipients(shippedItem(false), SmtpState.CONNECTED, all)).isEmpty();
	}

	@Test
	void emailsNobodyWhenTheItemIsNoLongerShipped() {
		roadmap.publish(NOW);
		Item item = shippedItem(false);
		item.changeStatus(ItemStatus.IN_PROGRESS, NOW);

		assertThat(ShippedEmails.recipients(item, SmtpState.CONNECTED, all)).isEmpty();
	}

	@Test
	void emailsNobodyWhenTheSmtpServerIsNotConnected() {
		roadmap.publish(NOW);

		assertThat(ShippedEmails.recipients(shippedItem(false), SmtpState.UNTESTED, all)).isEmpty();
		assertThat(ShippedEmails.recipients(shippedItem(false), SmtpState.NOT_WORKING, all)).isEmpty();
		assertThat(ShippedEmails.recipients(shippedItem(false), null, all)).isEmpty();
	}
}
