package fr.adriencaubel.roadmap.publicpage;

import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.item.ItemRepository;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import fr.adriencaubel.roadmap.roadmap.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/** Anonymous read side for Readers; never scoped by Owner, never exposes one. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPageService {

	static final int FEED_SIZE = 20;

	private final RoadmapRepository roadmaps;
	private final ItemRepository items;

	/** Private and unknown publicIds give the same 404. */
	public PublicPage get(String publicId) {
		Roadmap roadmap = roadmaps.findByPublicIdAndPublishedAtIsNotNull(publicId)
				.orElseThrow(() -> new NotFoundException("Roadmap not found"));
		return PublicPage.of(roadmap, items.findAllByRoadmapOrderByPositionAsc(roadmap));
	}

	/** The Feed of a Published Roadmap, with an ETag of its content. Private and unknown publicIds give the same 404. */
	public Feed feed(String publicId, String pageUrl) {
		Roadmap roadmap = roadmaps.findByPublicIdAndPublishedAtIsNotNull(publicId)
				.orElseThrow(() -> new NotFoundException("Roadmap not found"));
		String xml = RssFeed.of(roadmap, items.findRecentlyShipped(roadmap, Limit.of(FEED_SIZE)), pageUrl);
		return new Feed(xml, "\"" + DigestUtils.md5DigestAsHex(xml.getBytes(StandardCharsets.UTF_8)) + "\"");
	}

	public record Feed(String xml, String etag) {
	}
}
