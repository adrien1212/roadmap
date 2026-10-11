package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import fr.adriencaubel.roadmap.owner.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
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

	/** Each Roadmap with its Item counts and a preview of the current year. */
	@Transactional(readOnly = true)
	public List<RoadmapSummary> list() {
		Owner owner = currentOwner.get();
		Map<Long, ItemCounts> counts = roadmaps.countItems(owner).stream()
				.collect(Collectors.toMap(ItemCounts::roadmapId, Function.identity()));
		Map<Long, List<DatedItem>> datedItems = roadmaps.findDatedItems(owner).stream()
				.collect(Collectors.groupingBy(DatedItem::roadmapId));
		Year year = Year.now();
		return roadmaps.findAllByOwnerOrderByIdAsc(owner).stream()
				.map(roadmap -> {
					ItemCounts count = counts.get(roadmap.getId());
					return new RoadmapSummary(roadmap,
							count == null ? 0 : count.itemCount(),
							count == null ? 0 : count.internalItemCount(),
							RoadmapPreview.select(datedItems.getOrDefault(roadmap.getId(), List.of()), year));
				})
				.toList();
	}

	public Roadmap create(String name) {
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

	/** Its Items go with it (FK on delete cascade). */
	public void delete(Long id) {
		roadmaps.delete(get(id));
	}

	@Transactional(readOnly = true)
	public Roadmap get(Long id) {
		return roadmaps.findByIdAndOwner(id, currentOwner.get())
				.orElseThrow(() -> new NotFoundException("Roadmap not found"));
	}
}
