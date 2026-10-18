package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.roadmap.Roadmap;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Scoped by Roadmap, which is itself loaded scoped by Organisation; there is deliberately no bare findById. */
public interface ItemRepository extends Repository<Item, Long> {

	List<Item> findAllByRoadmapOrderByPositionAsc(Roadmap roadmap);

	Optional<Item> findByIdAndRoadmap(Long id, Roadmap roadmap);

	/** Background jobs only, with an id from a trusted event; Owner requests use findByIdAndRoadmap. */
	Optional<Item> findById(Long id);

	/** For the Feed: non-Internal Items with a Shipped date, newest first. */
	@Query("""
			select i from Item i
			where i.roadmap = :roadmap and i.internal = false
			  and i.status = fr.adriencaubel.roadmap.item.ItemStatus.SHIPPED and i.shippedAt is not null
			order by i.shippedAt desc""")
	List<Item> findRecentlyShipped(Roadmap roadmap, Limit limit);

	@Query("select max(i.position) from Item i where i.roadmap = :roadmap")
	Integer findLastPosition(Roadmap roadmap);

	Item save(Item item);

	void delete(Item item);
}
