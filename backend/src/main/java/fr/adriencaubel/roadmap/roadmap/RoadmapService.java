package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.organisation.CurrentOrganisation;
import fr.adriencaubel.roadmap.organisation.Organisation;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoadmapService {

	private final RoadmapRepository roadmaps;
	private final CurrentOwner currentOwner;
	private final CurrentOrganisation currentOrganisation;

	/** Each Roadmap with its Item and Internal Item counts. */
	@Transactional(readOnly = true)
	public List<RoadmapSummary> list() {
		Organisation organisation = currentOrganisation.get();
		Map<Long, ItemCounts> counts = roadmaps.countItems(organisation).stream()
				.collect(Collectors.toMap(ItemCounts::roadmapId, Function.identity()));
		return roadmaps.findAllByOrganisationOrderByIdAsc(organisation).stream()
				.map(roadmap -> {
					ItemCounts count = counts.get(roadmap.getId());
					return new RoadmapSummary(roadmap,
							count == null ? 0 : count.itemCount(),
							count == null ? 0 : count.internalItemCount());
				})
				.toList();
	}

	/** In the current Owner's Organisation; without one, 404 like every scoped request. */
	public Roadmap create(String name) {
		currentOrganisation.get();
		return roadmaps.save(Roadmap.create(currentOwner.get(), name));
	}

	/** A null name is left unchanged. */
	public Roadmap update(Long id, String name) {
		Roadmap roadmap = get(id);
		if (name != null) {
			roadmap.rename(name);
		}
		return roadmap;
	}

	public Roadmap publish(Long id) {
		Roadmap roadmap = get(id);
		roadmap.publish(Instant.now());
		return roadmap;
	}

	public Roadmap unpublish(Long id) {
		Roadmap roadmap = get(id);
		roadmap.unpublish();
		return roadmap;
	}

	/** Its Items go with it (FK on delete cascade). */
	public void delete(Long id) {
		roadmaps.delete(get(id));
	}

	@Transactional(readOnly = true)
	public Roadmap get(Long id) {
		return roadmaps.findByIdAndOrganisation(id, currentOrganisation.get())
				.orElseThrow(() -> new NotFoundException("Roadmap not found"));
	}
}
