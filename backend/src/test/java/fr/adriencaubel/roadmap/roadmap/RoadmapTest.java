package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.organisation.Organisation;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoadmapTest {

	private final Organisation organisation = new Organisation("Acme");

	@Test
	void belongsToItsOrganisation() {
		assertThat(Roadmap.create(organisation, "Q3").getOrganisation()).isSameAs(organisation);
	}

	@Test
	void isNamedUntitledRoadmapWhenNoNameIsGiven() {
		assertThat(Roadmap.create(organisation, null).getName()).isEqualTo("Untitled Roadmap");
	}

	@Test
	void isPrivateWithAPublicIdWhenCreated() {
		Roadmap roadmap = Roadmap.create(organisation, "Q3");

		assertThat(roadmap.getPublishedAt()).isNull();
		assertThat(roadmap.getPublicId()).matches("[A-Za-z0-9_-]{22}");
	}

	@Test
	void keepsItsPublicIdWhenRenamed() {
		Roadmap roadmap = Roadmap.create(organisation, "Q3");
		String publicId = roadmap.getPublicId();

		roadmap.rename("Q4");

		assertThat(roadmap.getPublicId()).isEqualTo(publicId);
	}

	@Test
	void trimsTheName() {
		Roadmap roadmap = Roadmap.create(organisation, "  Q3 plan ");

		assertThat(roadmap.getName()).isEqualTo("Q3 plan");
	}

	@Test
	void requiresANonBlankName() {
		assertThatThrownBy(() -> Roadmap.create(organisation, "  ")).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> Roadmap.create(organisation, "Q3").rename(null)).isInstanceOf(ValidationException.class);
	}

	@Test
	void limitsTheNameTo120Characters() {
		assertThat(Roadmap.create(organisation, "a".repeat(120)).getName()).hasSize(120);
		assertThatThrownBy(() -> Roadmap.create(organisation, "a".repeat(121))).isInstanceOf(ValidationException.class);
	}

	@Test
	void isPublishedAtTheGivenInstant() {
		Roadmap roadmap = Roadmap.create(organisation, "Q3");
		Instant now = Instant.parse("2026-10-11T10:00:00Z");

		roadmap.publish(now);

		assertThat(roadmap.getPublishedAt()).isEqualTo(now);
	}

	@Test
	void movesPublishedAtForwardWhenPublishedAgain() {
		Roadmap roadmap = Roadmap.create(organisation, "Q3");
		roadmap.publish(Instant.parse("2026-10-11T10:00:00Z"));

		roadmap.publish(Instant.parse("2026-10-12T10:00:00Z"));

		assertThat(roadmap.getPublishedAt()).isEqualTo(Instant.parse("2026-10-12T10:00:00Z"));
	}

	@Test
	void neverMovesPublishedAtBackward() {
		Roadmap roadmap = Roadmap.create(organisation, "Q3");
		roadmap.publish(Instant.parse("2026-10-12T10:00:00Z"));

		roadmap.publish(Instant.parse("2026-10-11T10:00:00Z"));

		assertThat(roadmap.getPublishedAt()).isEqualTo(Instant.parse("2026-10-12T10:00:00Z"));
	}

	@Test
	void becomesPrivateWhenUnpublishedAndKeepsItsPublicId() {
		Roadmap roadmap = Roadmap.create(organisation, "Q3");
		String publicId = roadmap.getPublicId();
		roadmap.publish(Instant.parse("2026-10-11T10:00:00Z"));

		roadmap.unpublish();

		assertThat(roadmap.getPublishedAt()).isNull();
		assertThat(roadmap.getPublicId()).isEqualTo(publicId);
	}

	@Test
	void canBeRepublishedAfterUnpublish() {
		Roadmap roadmap = Roadmap.create(organisation, "Q3");
		roadmap.publish(Instant.parse("2026-10-11T10:00:00Z"));
		roadmap.unpublish();

		roadmap.publish(Instant.parse("2026-10-01T10:00:00Z"));

		assertThat(roadmap.getPublishedAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
	}
}
