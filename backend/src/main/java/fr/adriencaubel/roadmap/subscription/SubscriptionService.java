package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.EmailAddress;
import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import fr.adriencaubel.roadmap.roadmap.RoadmapRepository;
import fr.adriencaubel.roadmap.smtp.SmtpSettingsRepository;
import fr.adriencaubel.roadmap.smtp.SmtpState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Readers subscribe and confirm. Answers never reveal whether an address is already subscribed. */
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

	private final RoadmapRepository roadmaps;
	private final SubscriptionRepository subscriptions;
	private final SmtpSettingsRepository smtpSettings;
	private final ApplicationEventPublisher events;

	/**
	 * Creates or reuses a pending Subscription and asks for a confirmation email (sent in the background,
	 * so new and known addresses answer alike, in content and in time).
	 */
	public void subscribe(String publicId, String email) {
		Roadmap roadmap = roadmaps.findByPublicIdAndPublishedAtIsNotNull(publicId)
				.orElseThrow(() -> new NotFoundException("Roadmap not found"));
		if (!emailsOpen(roadmap)) {
			throw new ConflictException("Emails aren't available for this Roadmap right now.");
		}
		String address = EmailAddress.normalise(email);
		Instant now = Instant.now();
		Subscription subscription = subscriptions.findByRoadmapAndEmail(roadmap, address)
				.orElseGet(() -> subscriptions.save(Subscription.pending(roadmap, address, now)));
		if (subscription.isExpired(now)) {
			subscription.restart(now);
		}
		if (subscription.mayResendConfirmation(now)) {
			subscription.confirmationSent(now);
			events.publishEvent(new ConfirmationRequested(subscription.getId()));
		}
	}

	/** An unknown token and an expired one look the same to the Reader. */
	public void confirm(String token) {
		Instant now = Instant.now();
		Subscription subscription = subscriptions.findByToken(token == null ? "" : token)
				.filter(s -> !s.isExpired(now))
				.orElseThrow(() -> new NotFoundException("This link has expired"));
		subscription.confirm(now);
	}

	/** Readers can subscribe when the Roadmap is Published and its Owner's SMTP server is connected. */
	@Transactional(readOnly = true)
	public boolean emailsOpen(Roadmap roadmap) {
		return roadmap.getPublishedAt() != null && smtpSettings.findByOwnerId(roadmap.getOwner().getId())
				.map(s -> s.getState() == SmtpState.CONNECTED).orElse(false);
	}
}
