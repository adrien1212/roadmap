package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Owner reads go through a Roadmap already scoped by Owner; Reader reads go through a token. */
public interface SubscriptionRepository extends Repository<Subscription, Long> {

	/** The Roadmap Subscription of an address (not its Item Subscriptions). */
	Optional<Subscription> findByRoadmapAndItemIsNullAndEmail(Roadmap roadmap, String email);

	Optional<Subscription> findByItemAndEmail(Item item, String email);

	Optional<Subscription> findById(Long id);

	Optional<Subscription> findByToken(String token);

	Optional<Subscription> findByIdAndRoadmap(Long id, Roadmap roadmap);

	List<Subscription> findAllByRoadmapOrderByCreatedAtDesc(Roadmap roadmap);

	/** Roadmap Subscriptions only: Readers waiting for one Item never get the others' emails. */
	List<Subscription> findAllByRoadmapAndItemIsNullAndState(Roadmap roadmap, SubscriptionState state);

	List<Subscription> findAllByItemAndState(Item item, SubscriptionState state);

	List<Subscription> findAllByStateAndCreatedAtBefore(SubscriptionState state, Instant before);

	Subscription save(Subscription subscription);

	void delete(Subscription subscription);
}
