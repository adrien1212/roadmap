package fr.adriencaubel.roadmap.publicpage;

import fr.adriencaubel.roadmap.category.Category;
import fr.adriencaubel.roadmap.category.CategoryColour;
import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.item.ItemStatus;
import fr.adriencaubel.roadmap.owner.Owner;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicPageTest {

	private final Owner owner = new Owner("dev", "dev", "owner@example.com", "Owner");
	private final Roadmap roadmap = Roadmap.create(owner, "Q3");
	private final Category security = new Category(owner, "Security", CategoryColour.SKY);
	private final Category billing = new Category(owner, "Billing", CategoryColour.PINK);
	private final Category secret = new Category(owner, "Secret", CategoryColour.SLATE);

	private Item item(String title, Category category, boolean internal) {
		Item item = Item.append(roadmap, title, null);
		item.categorize(category);
		item.markInternal(internal);
		return item;
	}

	@Test
	void carriesTheRoadmapNameAndPublishedDate() {
		roadmap.publish(Instant.parse("2026-10-11T10:00:00Z"));

		PublicPage page = PublicPage.of(roadmap, List.of());

		assertThat(page.name()).isEqualTo("Q3");
		assertThat(page.publishedAt()).isEqualTo(Instant.parse("2026-10-11T10:00:00Z"));
	}

	@Test
	void dropsInternalItemsAndKeepsTheManualOrder() {
		PublicPage page = PublicPage.of(roadmap, List.of(
				item("SSO", null, false), item("Pricing v2", null, true), item("Dark mode", null, false)));

		assertThat(page.items()).extracting(PublicPage.PublicItem::title).containsExactly("SSO", "Dark mode");
	}

	@Test
	void listsOnlyCategoriesUsedByNonInternalItemsOnce() {
		PublicPage page = PublicPage.of(roadmap, List.of(
				item("SSO", security, false), item("Invoices", billing, false),
				item("Audit log", security, false), item("Pricing v2", secret, true), item("Dark mode", null, false)));

		assertThat(page.categories()).extracting(PublicPage.PublicCategory::name)
				.containsExactly("Security", "Billing");
		assertThat(page.categories()).extracting(PublicPage.PublicCategory::colour).containsExactly("sky", "pink");
	}

	@Test
	void mapsTheReaderFieldsOfAnItem() {
		Item item = item("SSO", null, false);
		item.describe("Line 1\nLine 2");
		item.schedule(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31));
		item.changeStatus(ItemStatus.IN_PROGRESS);

		PublicPage.PublicItem publicItem = PublicPage.of(roadmap, List.of(item)).items().getFirst();

		assertThat(publicItem.description()).isEqualTo("Line 1\nLine 2");
		assertThat(publicItem.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
		assertThat(publicItem.endDate()).isEqualTo(LocalDate.of(2026, 5, 31));
		assertThat(publicItem.status()).isEqualTo("in_progress");
		assertThat(publicItem.categoryId()).isNull();
	}

	@Test
	void exposesExactlyTheFieldsOfTheSpec() {
		assertThat(fields(PublicPage.class)).containsExactly("name", "publishedAt", "categories", "items");
		assertThat(fields(PublicPage.PublicCategory.class)).containsExactly("id", "name", "colour");
		assertThat(fields(PublicPage.PublicItem.class))
				.containsExactly("id", "title", "description", "startDate", "endDate", "status", "categoryId");
	}

	private static List<String> fields(Class<? extends Record> type) {
		return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
	}
}
