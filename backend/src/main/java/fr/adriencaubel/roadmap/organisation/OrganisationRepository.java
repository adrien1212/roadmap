package fr.adriencaubel.roadmap.organisation;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;

public interface OrganisationRepository extends Repository<Organisation, Long> {

	Organisation save(Organisation organisation);

	@Query("select count(r) > 0 from Roadmap r where r.organisation = :organisation")
	boolean hasRoadmaps(Organisation organisation);

	@Query("select count(o) from Owner o where o.organisation = :organisation")
	long countPeople(Organisation organisation);

	/** Closing: every Roadmap becomes Private, so its Public page and Feed 404. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Roadmap r set r.publishedAt = null where r.organisation = :organisation")
	void unpublishRoadmaps(Organisation organisation);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Invitation i set i.revokedAt = :now
			where i.organisation = :organisation and i.usedAt is null and i.revokedAt is null""")
	void revokeInvitations(Organisation organisation, Instant now);

	/** Closing: everyone leaves, the Role Owner included; they go to the Welcome page. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Owner o set o.organisation = null, o.role = null, o.joinedAt = null where o.organisation = :organisation")
	void removeEveryone(Organisation organisation);

	/** An Organisation left empty and without Roadmaps: its Categories and SMTP settings go, then it does. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Category c where c.organisation = :organisation")
	void deleteCategories(Organisation organisation);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from SmtpSettings s where s.organisationId = :organisationId")
	void deleteSmtpSettings(Long organisationId);

	/** Its Invitations go with it (FK on delete cascade). */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Organisation o where o.id = :id")
	void deleteEmpty(Long id);
}
