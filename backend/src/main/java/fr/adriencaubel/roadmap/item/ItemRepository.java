package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.roadmap.Roadmap;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Scoped by Roadmap, which is itself loaded scoped by Owner; there is deliberately no bare findById. */
public interface ItemRepository extends Repository<Item, Long> {

	List<Item> findAllByRoadmapOrderByPositionAsc(Roadmap roadmap);

	Optional<Item> findByIdAndRoadmap(Long id, Roadmap roadmap);

	@Query("select max(i.position) from Item i where i.roadmap = :roadmap")
	Integer findLastPosition(Roadmap roadmap);

	Item save(Item item);

	void delete(Item item);
}
