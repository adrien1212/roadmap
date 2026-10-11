package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.owner.Owner;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Every Owner query is scoped by Owner; there is deliberately no bare findById.
 * The one exception is the anonymous Public page lookup by publicId.
 */
public interface RoadmapRepository extends Repository<Roadmap, Long> {

	List<Roadmap> findAllByOwnerOrderByIdAsc(Owner owner);

	Optional<Roadmap> findByIdAndOwner(Long id, Owner owner);

	/** For Readers: only a Published Roadmap matches. */
	Optional<Roadmap> findByPublicIdAndPublishedAtIsNotNull(String publicId);

	/** Item and Internal Item counts per Roadmap; Roadmaps without Items are absent. */
	@Query("""
			select new fr.adriencaubel.roadmap.roadmap.ItemCounts(
			    i.roadmap.id, count(i), sum(case when i.internal = true then 1 else 0 end))
			from Item i where i.roadmap.owner = :owner group by i.roadmap.id""")
	List<ItemCounts> countItems(Owner owner);

	Roadmap save(Roadmap roadmap);

	void delete(Roadmap roadmap);
}
