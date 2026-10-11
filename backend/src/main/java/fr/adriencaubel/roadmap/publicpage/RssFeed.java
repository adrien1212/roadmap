package fr.adriencaubel.roadmap.publicpage;

import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.roadmap.Roadmap;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * The Feed of a Published Roadmap: RSS 2.0 listing its Shipped Items, newest first.
 * Internal Items and Items without a Shipped date are left out here too, whatever the caller passes.
 */
final class RssFeed {

	private RssFeed() {
	}

	static String of(Roadmap roadmap, List<Item> recentlyShipped, String pageUrl) {
		List<Item> items = recentlyShipped.stream()
				.filter(item -> !item.isInternal() && item.getShippedAt() != null)
				.sorted(Comparator.comparing(Item::getShippedAt).reversed())
				.toList();

		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rss version=\"2.0\"><channel>");
		tag(xml, "title", roadmap.getName());
		tag(xml, "link", pageUrl);
		tag(xml, "description", "What shipped on " + roadmap.getName());
		if (!items.isEmpty()) {
			tag(xml, "lastBuildDate", rfc1123(items.getFirst().getShippedAt()));
		}
		for (Item item : items) {
			xml.append("<item>");
			tag(xml, "title", item.getTitle());
			tag(xml, "link", pageUrl);
			if (item.getDescription() != null && !item.getDescription().isBlank()) {
				tag(xml, "description", item.getDescription());
			}
			// Stable per Item so feed readers never show it twice.
			xml.append("<guid isPermaLink=\"false\">")
					.append(escape(roadmap.getPublicId() + "-" + item.getId()))
					.append("</guid>");
			tag(xml, "pubDate", rfc1123(item.getShippedAt()));
			xml.append("</item>");
		}
		return xml.append("</channel></rss>").toString();
	}

	private static void tag(StringBuilder xml, String name, String text) {
		xml.append('<').append(name).append('>').append(escape(text)).append("</").append(name).append('>');
	}

	/** Owner text in XML: escape markup and drop control characters XML 1.0 does not allow. */
	static String escape(String text) {
		StringBuilder out = new StringBuilder(text.length());
		for (char c : text.toCharArray()) {
			switch (c) {
				case '&' -> out.append("&amp;");
				case '<' -> out.append("&lt;");
				case '>' -> out.append("&gt;");
				case '"' -> out.append("&quot;");
				case '\'' -> out.append("&apos;");
				default -> {
					if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
						out.append(c);
					}
				}
			}
		}
		return out.toString();
	}

	private static String rfc1123(Instant instant) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atZone(ZoneOffset.UTC));
	}
}
