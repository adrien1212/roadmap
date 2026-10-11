package fr.adriencaubel.roadmap.publicpage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;

/** Unauthenticated API behind the Public page at /r/{publicId}. */
@RestController
@RequiredArgsConstructor
public class PublicPageController {

	private static final MediaType RSS = MediaType.parseMediaType("application/rss+xml;charset=UTF-8");

	private final PublicPageService service;

	@GetMapping("/api/public/roadmaps/{publicId}")
	public PublicPage get(@PathVariable String publicId) {
		return service.get(publicId);
	}

	/** RSS 2.0 for feed readers. Links point at the Public page on the public origin (forwarded headers). */
	@GetMapping("/api/public/roadmaps/{publicId}/feed.xml")
	public ResponseEntity<String> feed(@PathVariable String publicId, WebRequest request) {
		String pageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/r/{publicId}").buildAndExpand(publicId).toUriString();
		PublicPageService.Feed feed = service.feed(publicId, pageUrl);
		if (request.checkNotModified(feed.etag())) {
			return null; // 304; feed readers poll often
		}
		return ResponseEntity.ok()
				.contentType(RSS)
				.cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
				.eTag(feed.etag())
				.body(feed.xml());
	}
}
