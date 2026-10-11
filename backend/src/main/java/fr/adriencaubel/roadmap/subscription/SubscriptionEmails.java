package fr.adriencaubel.roadmap.subscription;

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

/**
 * Sends emails to Readers in the background, after the change that asked for them is committed.
 * A failure drops the email and marks the Owner's SMTP server as not working (they are warned).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SubscriptionEmails {

	private final SubscriptionRepository subscriptions;
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
			String brand = roadmap.getOwner().getBrandName();
			String confirmUrl = publicUrl + "/r/" + roadmap.getPublicId() + "/confirm?token=" + subscription.getToken();
			send(roadmap, EmailContent.confirmation(subscription.getEmail(), roadmap.getName(), brand, confirmUrl));
		});
	}

	private void send(Roadmap roadmap, OutgoingEmail email) {
		SmtpSettings settings = smtpSettings.findByOwnerId(roadmap.getOwner().getId()).orElse(null);
		if (settings == null || settings.getState() != SmtpState.CONNECTED) {
			return;
		}
		try {
			mailer.send(settings, email, roadmap.getOwner().getBrandName());
		} catch (RuntimeException e) {
			log.warn("Email to a Reader dropped for Roadmap {}: {}", roadmap.getId(), e.getMessage());
			settings.markNotWorking(e.getMessage());
			smtpSettings.save(settings);
		}
	}
}
