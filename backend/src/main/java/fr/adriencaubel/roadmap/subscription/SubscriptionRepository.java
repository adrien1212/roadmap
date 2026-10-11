package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.roadmap.Roadmap;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Owner reads go through a Roadmap already scoped by Owner; Reader reads go through a token. */
public interface SubscriptionRepository extends Repository<Subscription, Long> {

	Optional<Subscription> findByRoadmapAndEmail(Roadmap roadmap, String email);

	Optional<Subscription> findById(Long id);

	Optional<Subscription> findByToken(String token);

	Optional<Subscription> findByIdAndRoadmap(Long id, Roadmap roadmap);

	List<Subscription> findAllByRoadmapOrderByCreatedAtDesc(Roadmap roadmap);

	List<Subscription> findAllByRoadmapAndState(Roadmap roadmap, SubscriptionState state);

	List<Subscription> findAllByStateAndCreatedAtBefore(SubscriptionState state, Instant before);

	Subscription save(Subscription subscription);

	void delete(Subscription subscription);
}
