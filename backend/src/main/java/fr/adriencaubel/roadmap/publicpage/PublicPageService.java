package fr.adriencaubel.roadmap.publicpage;

import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.item.ItemRepository;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import fr.adriencaubel.roadmap.roadmap.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Anonymous read side for Readers; never scoped by Owner, never exposes one. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPageService {

	private final RoadmapRepository roadmaps;
	private final ItemRepository items;

	/** Private and unknown publicIds give the same 404. */
	public PublicPage get(String publicId) {
		Roadmap roadmap = roadmaps.findByPublicIdAndPublishedAtIsNotNull(publicId)
				.orElseThrow(() -> new NotFoundException("Roadmap not found"));
		return PublicPage.of(roadmap, items.findAllByRoadmapOrderByPositionAsc(roadmap));
	}
}
