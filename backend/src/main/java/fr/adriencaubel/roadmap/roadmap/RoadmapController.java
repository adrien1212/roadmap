package fr.adriencaubel.roadmap.roadmap;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {

	private final RoadmapService service;

	@GetMapping
	public List<RoadmapResponse> list() {
		return service.list().stream().map(RoadmapResponse::of).toList();
	}

	/** The body is optional: no name gives "Untitled Roadmap". */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RoadmapResponse create(@RequestBody(required = false) RoadmapRequest request) {
		return RoadmapResponse.of(service.create(request == null ? null : request.name()));
	}

	@GetMapping("/{id}")
	public RoadmapResponse get(@PathVariable Long id) {
		return RoadmapResponse.of(service.get(id));
	}

	@PatchMapping("/{id}")
	public RoadmapResponse update(@PathVariable Long id, @RequestBody RoadmapRequest request) {
		return RoadmapResponse.of(service.update(id, request.name()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}

	public record RoadmapRequest(String name) {
	}

	public record RoadmapResponse(Long id, String name, String publicId, Instant publishedAt) {

		static RoadmapResponse of(Roadmap roadmap) {
			return new RoadmapResponse(roadmap.getId(), roadmap.getName(), roadmap.getPublicId(),
					roadmap.getPublishedAt());
		}
	}
}
