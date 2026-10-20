package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.EmailAddress;
import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.item.ItemRepository;
import fr.adriencaubel.roadmap.item.ItemStatus;
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
	private final ItemRepository items;
	private final SmtpSettingsRepository smtpSettings;
	private final ApplicationEventPublisher events;

	/**
	 * Creates or reuses a pending Subscription and asks for a confirmation email (sent in the background,
	 * so new and known addresses answer alike, in content and in time).
	 */
	public void subscribe(String publicId, String email) {
		Roadmap roadmap = publishedRoadmap(publicId);
		if (!emailsOpen(roadmap)) {
			throw new ConflictException("Emails aren't available for this Roadmap right now.");
		}
		String address = EmailAddress.normalise(email);
		Instant now = Instant.now();
		askToConfirm(subscriptions.findByRoadmapAndItemIsNullAndEmail(roadmap, address)
				.orElseGet(() -> subscriptions.save(Subscription.pending(roadmap, address, now))), now);
	}

	/**
	 * "Notify me when this ships" for one Item: the same flow and the same answer as subscribe. An Internal or
	 * unknown Item is the same 404; an Item already Shipped has nothing left to wait for.
	 */
	public void subscribeToItem(String publicId, Long itemId, String email) {
		Roadmap roadmap = publishedRoadmap(publicId);
		Item item = items.findByIdAndRoadmap(itemId, roadmap)
				.filter(i -> !i.isInternal())
				.orElseThrow(() -> new NotFoundException("Item not found"));
		if (!emailsOpen(roadmap)) {
			throw new ConflictException("Emails aren't available for this Roadmap right now.");
		}
		if (item.getStatus() == ItemStatus.SHIPPED) {
			throw new ConflictException("This Item has already shipped.");
		}
		String address = EmailAddress.normalise(email);
		Instant now = Instant.now();
		askToConfirm(subscriptions.findByItemAndEmail(item, address)
				.orElseGet(() -> subscriptions.save(Subscription.pendingForItem(item, address, now))), now);
	}

	private Roadmap publishedRoadmap(String publicId) {
		return roadmaps.findByPublicIdAndPublishedAtIsNotNull(publicId)
				.orElseThrow(() -> new NotFoundException("Roadmap not found"));
	}

	private void askToConfirm(Subscription subscription, Instant now) {
		if (subscription.isExpired(now)) {
			subscription.restart(now);
		}
		if (subscription.mayResendConfirmation(now)) {
			subscription.confirmationSent(now);
			events.publishEvent(new ConfirmationRequested(subscription.getId()));
		}
		// Already confirmed: the page still says "Check your inbox"; only the inbox learns it is subscribed.
		if (subscription.mayRemindSubscribed(now)) {
			subscription.remindedSubscribed(now);
			events.publishEvent(new AlreadySubscribed(subscription.getId()));
		}
	}

	/** An unknown token and an expired one look the same to the Reader. Returns the Item it covers, if any. */
	public Covered confirm(String token) {
		Instant now = Instant.now();
		Subscription subscription = subscriptions.findByToken(token == null ? "" : token)
				.filter(s -> !s.isExpired(now))
				.orElseThrow(() -> new NotFoundException("This link has expired"));
		subscription.confirm(now);
		return Covered.of(subscription);
	}

	/** Ends the Subscription: the address is deleted. Returns the Item it covered, if any. */
	public Covered unsubscribe(String token) {
		Subscription subscription = subscriptions.findByToken(token == null ? "" : token)
				.orElseThrow(() -> new NotFoundException("This link has expired"));
		Covered covered = Covered.of(subscription);
		subscriptions.delete(subscription);
		return covered;
	}

	/**
	 * What a Subscription covers, for the Reader holding its token: the Item, or nulls for the whole Roadmap and
	 * for an Item that is now Internal (never named to Readers).
	 */
	public record Covered(Long itemId, String itemTitle) {

		static Covered of(Subscription subscription) {
			Item item = subscription.getItem();
			return item == null || item.isInternal() ? new Covered(null, null) : new Covered(item.getId(), item.getTitle());
		}
	}

	/**
	 * The Owner's view: confirmed and pending Readers of the Roadmap and of its Items (newest first, expired
	 * pending left out), their counts, and how many confirmed ones wait for an Item.
	 */
	@Transactional(readOnly = true)
	public ReadersOverview overview(Long roadmapId) {
		Roadmap roadmap = ownerRoadmaps.get(roadmapId);
		Instant now = Instant.now();
		List<Reader> current = subscriptions.findAllByRoadmapOrderByCreatedAtDesc(roadmap).stream()
				.filter(s -> !s.isExpired(now))
				.map(Reader::of)
				.toList();
		long confirmed = current.stream().filter(r -> r.state() == SubscriptionState.CONFIRMED).count();
		long confirmedOnItems = current.stream()
				.filter(r -> r.state() == SubscriptionState.CONFIRMED && r.itemId() != null).count();
		return new ReadersOverview(confirmed, current.size() - confirmed, confirmedOnItems, current);
	}

	/** The Owner removes a Reader: the address is deleted. */
	public void remove(Long roadmapId, Long subscriptionId) {
		Roadmap roadmap = ownerRoadmaps.get(roadmapId);
		Subscription subscription = subscriptions.findByIdAndRoadmap(subscriptionId, roadmap)
				.orElseThrow(() -> new NotFoundException("Subscription not found"));
		subscriptions.delete(subscription);
	}

	public record ReadersOverview(long confirmed, long pending, long confirmedOnItems, List<Reader> readers) {
	}

	/** One Subscription as the Owner sees it, read inside the transaction (the Item is lazy). No token. */
	public record Reader(Long id, String email, SubscriptionState state, Instant createdAt, Instant confirmedAt,
			Long itemId, String itemTitle) {

		static Reader of(Subscription s) {
			Item item = s.getItem();
			return new Reader(s.getId(), s.getEmail(), s.getState(), s.getCreatedAt(), s.getConfirmedAt(),
					item == null ? null : item.getId(), item == null ? null : item.getTitle());
		}
	}

	/** Readers can subscribe when the Roadmap is Published and its Organisation's SMTP server is connected. */
	@Transactional(readOnly = true)
	public boolean emailsOpen(Roadmap roadmap) {
		return roadmap.getPublishedAt() != null && smtpSettings.findByOrganisationId(roadmap.getOrganisation().getId())
				.map(s -> s.getState() == SmtpState.CONNECTED).orElse(false);
	}
}
