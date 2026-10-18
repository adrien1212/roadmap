package fr.adriencaubel.roadmap.smtp;

import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface SmtpSettingsRepository extends Repository<SmtpSettings, Long> {

	Optional<SmtpSettings> findByOrganisationId(Long organisationId);

	SmtpSettings save(SmtpSettings settings);
}
