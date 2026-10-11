package fr.adriencaubel.roadmap.publicpage;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated API behind the Public page at /r/{publicId}. */
@RestController
@RequiredArgsConstructor
public class PublicPageController {

	private final PublicPageService service;

	@GetMapping("/api/public/roadmaps/{publicId}")
	public PublicPage get(@PathVariable String publicId) {
		return service.get(publicId);
	}
}
