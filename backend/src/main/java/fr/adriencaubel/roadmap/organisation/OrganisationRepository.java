package fr.adriencaubel.roadmap.organisation;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface OrganisationRepository extends Repository<Organisation, Long> {

	Organisation save(Organisation organisation);

	@Query("select count(r) > 0 from Roadmap r where r.organisation = :organisation")
	boolean hasRoadmaps(Organisation organisation);

	@Query("select count(o) from Owner o where o.organisation = :organisation")
	long countPeople(Organisation organisation);

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
