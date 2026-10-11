package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.owner.Owner;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Every query is scoped by Owner; there is deliberately no bare findById. */
public interface RoadmapRepository extends Repository<Roadmap, Long> {

	List<Roadmap> findAllByOwnerOrderByIdAsc(Owner owner);

	Optional<Roadmap> findByIdAndOwner(Long id, Owner owner);

	Roadmap save(Roadmap roadmap);

	void delete(Roadmap roadmap);
}
