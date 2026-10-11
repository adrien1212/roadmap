package fr.adriencaubel.roadmap.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Pending Subscriptions not confirmed within 7 days are deleted (not counted as unsubscribed). */
@Slf4j
@Component
@RequiredArgsConstructor
class PendingExpiry {

	private final SubscriptionRepository subscriptions;

	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void deleteExpired() {
		List<Subscription> expired = subscriptions.findAllByStateAndCreatedAtBefore(SubscriptionState.PENDING,
				Instant.now().minus(Subscription.PENDING_LIFETIME));
		expired.forEach(subscriptions::delete);
		if (!expired.isEmpty()) {
			log.info("Deleted {} expired pending Subscriptions", expired.size());
		}
	}
}
