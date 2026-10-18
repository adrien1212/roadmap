package fr.adriencaubel.roadmap.organisation;

import org.springframework.data.repository.Repository;

public interface OrganisationRepository extends Repository<Organisation, Long> {

	Organisation save(Organisation organisation);
}
