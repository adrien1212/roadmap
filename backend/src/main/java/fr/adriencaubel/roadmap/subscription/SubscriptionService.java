package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.EmailAddress;
import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import fr.adriencaubel.roadmap.roadmap.RoadmapRepository;
import fr.adriencaubel.roadmap.roadmap.RoadmapService;
import fr.adriencaubel.roadmap.smtp.SmtpSettingsRepository;
import fr.adriencaubel.roadmap.smtp.SmtpState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Readers subscribe and confirm. Answers never reveal whether an address is already subscribed. */
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

	private final RoadmapRepository roadmaps;
	private final RoadmapService ownerRoadmaps;
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

	/** Ends the Subscription: the address is deleted. */
	public void unsubscribe(String token) {
		Subscription subscription = subscriptions.findByToken(token == null ? "" : token)
				.orElseThrow(() -> new NotFoundException("This link has expired"));
		subscriptions.delete(subscription);
	}

	/** The Owner's view: confirmed and pending Readers (newest first, expired pending left out) and their counts. */
	@Transactional(readOnly = true)
	public ReadersOverview overview(Long roadmapId) {
		Roadmap roadmap = ownerRoadmaps.get(roadmapId);
		Instant now = Instant.now();
		List<Subscription> current = subscriptions.findAllByRoadmapOrderByCreatedAtDesc(roadmap).stream()
				.filter(s -> !s.isExpired(now))
				.toList();
		long confirmed = current.stream().filter(Subscription::isConfirmed).count();
		return new ReadersOverview(confirmed, current.size() - confirmed, current);
	}

	/** The Owner removes a Reader: the address is deleted. */
	public void remove(Long roadmapId, Long subscriptionId) {
		Roadmap roadmap = ownerRoadmaps.get(roadmapId);
		Subscription subscription = subscriptions.findByIdAndRoadmap(subscriptionId, roadmap)
				.orElseThrow(() -> new NotFoundException("Subscription not found"));
		subscriptions.delete(subscription);
	}

	public record ReadersOverview(long confirmed, long pending, List<Subscription> subscriptions) {
	}

	/** Readers can subscribe when the Roadmap is Published and its Organisation's SMTP server is connected. */
	@Transactional(readOnly = true)
	public boolean emailsOpen(Roadmap roadmap) {
		return roadmap.getPublishedAt() != null && smtpSettings.findByOrganisationId(roadmap.getOrganisation().getId())
				.map(s -> s.getState() == SmtpState.CONNECTED).orElse(false);
	}
}
