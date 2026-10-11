package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoadmapService {

	private final RoadmapRepository roadmaps;
	private final CurrentOwner currentOwner;

	@Transactional(readOnly = true)
	public List<Roadmap> list() {
		return roadmaps.findAllByOwnerOrderByIdAsc(currentOwner.get());
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
