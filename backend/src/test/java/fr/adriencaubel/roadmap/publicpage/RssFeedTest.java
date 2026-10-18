package fr.adriencaubel.roadmap.publicpage;

import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.item.ItemStatus;
import fr.adriencaubel.roadmap.organisation.Organisation;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RssFeedTest {

	private static final String PAGE = "https://example.com/r/abc";

	private final Organisation organisation = new Organisation("Acme product team");
	private final Roadmap roadmap = Roadmap.create(organisation, "Product roadmap 2026");

	private long nextId = 1;

	private Item shipped(String title, String shippedAt, boolean internal) {
		Item item = Item.append(roadmap, title, null);
		ReflectionTestUtils.setField(item, "id", nextId++);
		item.markInternal(internal);
		item.changeStatus(ItemStatus.SHIPPED, Instant.parse(shippedAt));
		return item;
	}

	private static Document parse(String xml) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static List<String> texts(Document doc, String tag) {
		NodeList nodes = doc.getElementsByTagName(tag);
		List<String> out = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			out.add(nodes.item(i).getTextContent());
		}
		return out;
	}

	@Test
	void describesTheRoadmapInTheChannel() throws Exception {
		Document doc = parse(RssFeed.of(roadmap, List.of(shipped("SSO", "2026-10-02T08:00:00Z", false)), PAGE));

		Element rss = doc.getDocumentElement();
		assertThat(rss.getTagName()).isEqualTo("rss");
		assertThat(rss.getAttribute("version")).isEqualTo("2.0");
		assertThat(texts(doc, "title").getFirst()).isEqualTo("Product roadmap 2026");
		assertThat(texts(doc, "link").getFirst()).isEqualTo(PAGE);
		assertThat(texts(doc, "lastBuildDate")).containsExactly("Fri, 2 Oct 2026 08:00:00 GMT");
	}

	@Test
	void namesTheBrandInTheChannelTitle() throws Exception {
		organisation.changeBrand("Acme");

		Document doc = parse(RssFeed.of(roadmap, List.of(), PAGE));

		assertThat(texts(doc, "title").getFirst()).isEqualTo("Product roadmap 2026 · Acme");
	}

	@Test
	void listsItemsNewestFirst() throws Exception {
		List<Item> items = List.of(
				shipped("Older", "2026-03-09T10:00:00Z", false),
				shipped("Newest", "2026-10-02T08:00:00Z", false),
				shipped("Middle", "2026-06-20T12:00:00Z", false));

		Document doc = parse(RssFeed.of(roadmap, items, PAGE));

		assertThat(texts(doc, "title")).containsExactly("Product roadmap 2026", "Newest", "Middle", "Older");
	}

	@Test
	void neverListsAnInternalItem() throws Exception {
		List<Item> items = List.of(
				shipped("SSO", "2026-10-02T08:00:00Z", false),
				shipped("Billing migration", "2026-10-03T08:00:00Z", true));

		String xml = RssFeed.of(roadmap, items, PAGE);

		assertThat(xml).doesNotContain("Billing migration");
		assertThat(parse(xml).getElementsByTagName("item").getLength()).isEqualTo(1);
	}

	@Test
	void leavesOutItemsWithoutAShippedDate() throws Exception {
		Item planned = Item.append(roadmap, "Dark mode", null);

		assertThat(parse(RssFeed.of(roadmap, List.of(planned), PAGE)).getElementsByTagName("item").getLength()).isZero();
	}

	@Test
	void escapesOwnerText() throws Exception {
		Item item = shipped("Billing & <Invoices>", "2026-10-02T08:00:00Z", false);
		item.describe("Uses \"quotes\" & 'apostrophes'");

		String xml = RssFeed.of(roadmap, List.of(item), PAGE);

		assertThat(xml).contains("Billing &amp; &lt;Invoices&gt;");
		Document doc = parse(xml);
		assertThat(texts(doc, "title").get(1)).isEqualTo("Billing & <Invoices>");
		assertThat(texts(doc, "description").get(1)).isEqualTo("Uses \"quotes\" & 'apostrophes'");
	}

	@Test
	void givesEachItemAStableGuidAndItsShippedDate() throws Exception {
		Item item = shipped("SSO", "2026-10-02T08:00:00Z", false);

		Document doc = parse(RssFeed.of(roadmap, List.of(item), PAGE));

		Element guid = (Element) doc.getElementsByTagName("guid").item(0);
		assertThat(guid.getTextContent()).isEqualTo(roadmap.getPublicId() + "-" + item.getId());
		assertThat(guid.getAttribute("isPermaLink")).isEqualTo("false");
		assertThat(texts(doc, "pubDate")).containsExactly("Fri, 2 Oct 2026 08:00:00 GMT");
		assertThat(texts(doc, "link")).containsExactly(PAGE, PAGE);
	}

	@Test
	void isAValidEmptyFeedWhenNothingShipped() throws Exception {
		Document doc = parse(RssFeed.of(roadmap, List.of(), PAGE));

		assertThat(doc.getElementsByTagName("channel").getLength()).isEqualTo(1);
		assertThat(doc.getElementsByTagName("item").getLength()).isZero();
		assertThat(doc.getElementsByTagName("lastBuildDate").getLength()).isZero();
	}
}
