package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.item.ItemRepository;
import fr.adriencaubel.roadmap.item.ItemShipped;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import fr.adriencaubel.roadmap.smtp.Mailer;
import fr.adriencaubel.roadmap.smtp.OutgoingEmail;
import fr.adriencaubel.roadmap.smtp.SmtpSettings;
import fr.adriencaubel.roadmap.smtp.SmtpSettingsRepository;
import fr.adriencaubel.roadmap.smtp.SmtpState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Sends emails to Readers in the background, after the change that asked for them is committed.
 * A failure drops the email and marks the Owner's SMTP server as not working (they are warned).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SubscriptionEmails {

	private final SubscriptionRepository subscriptions;
	private final ItemRepository items;
	private final SmtpSettingsRepository smtpSettings;
	private final Mailer mailer;

	@Value("${app.public-url}")
	private String publicUrl;

	@Async
	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onConfirmationRequested(ConfirmationRequested event) {
		subscriptions.findById(event.subscriptionId()).ifPresent(subscription -> {
			Roadmap roadmap = subscription.getRoadmap();
			String brand = roadmap.getOrganisation().getBrandName();
			String pageUrl = publicUrl + "/r/" + roadmap.getPublicId();
			String confirmUrl = pageUrl + "/confirm?token=" + subscription.getToken();
			send(roadmap, subscription.isForItem()
					? EmailContent.itemConfirmation(subscription.getEmail(), subscription.getItem().getTitle(), roadmap.getName(),
							brand, confirmUrl, pageUrl + "/unsubscribe?token=" + subscription.getToken())
					: EmailContent.confirmation(subscription.getEmail(), roadmap.getName(), brand, confirmUrl));
		});
	}

	/**
	 * One email per confirmed Subscription, of the Roadmap or of this Item (an address on both gets two);
	 * on the first failure the rest are dropped and the Owner is warned.
	 */
	@Async
	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onItemShipped(ItemShipped event) {
		items.findById(event.itemId()).ifPresent(item -> {
			Roadmap roadmap = item.getRoadmap();
			SmtpSettings settings = smtpSettings.findByOrganisationId(roadmap.getOrganisation().getId()).orElse(null);
			List<Subscription> recipients = ShippedEmails.recipients(item, settings == null ? null : settings.getState(),
					subscriptions.findAllByRoadmapAndItemIsNullAndState(roadmap, SubscriptionState.CONFIRMED),
					subscriptions.findAllByItemAndState(item, SubscriptionState.CONFIRMED));
			String brand = roadmap.getOrganisation().getBrandName();
			String pageUrl = publicUrl + "/r/" + roadmap.getPublicId();
			EmailContent.ShippedItem shipped = new EmailContent.ShippedItem(item.getTitle(), item.getDescription(),
					item.getCategory() == null ? null : item.getCategory().getName());
			for (Subscription subscription : recipients) {
				String unsubscribeUrl = pageUrl + "/unsubscribe?token=" + subscription.getToken();
				String oneClickUrl = publicUrl + "/api/public/subscriptions/unsubscribe?token=" + subscription.getToken();
				OutgoingEmail email = subscription.isForItem()
						? EmailContent.shippedForItem(subscription.getEmail(), shipped, roadmap.getName(), brand, pageUrl,
								unsubscribeUrl, oneClickUrl)
						: EmailContent.shipped(subscription.getEmail(), shipped, roadmap.getName(), brand, pageUrl,
								unsubscribeUrl, oneClickUrl);
				if (!send(roadmap, email)) {
					return;
				}
			}
		});
	}

	/** False when sending failed (the email is dropped and the SMTP server marked not working). */
	private boolean send(Roadmap roadmap, OutgoingEmail email) {
		SmtpSettings settings = smtpSettings.findByOrganisationId(roadmap.getOrganisation().getId()).orElse(null);
		if (settings == null || settings.getState() != SmtpState.CONNECTED) {
			return false;
		}
		try {
			mailer.send(settings, email, roadmap.getOrganisation().getBrandName());
			return true;
		} catch (RuntimeException e) {
			log.warn("Email to a Reader dropped for Roadmap {}: {}", roadmap.getId(), e.getMessage());
			settings.markNotWorking(e.getMessage());
			smtpSettings.save(settings);
			return false;
		}
	}
}
