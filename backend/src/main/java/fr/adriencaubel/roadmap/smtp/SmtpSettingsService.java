package fr.adriencaubel.roadmap.smtp;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.organisation.CurrentOrganisation;
import fr.adriencaubel.roadmap.organisation.Organisation;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** The current Organisation's SMTP settings. The password goes in sealed and never comes out through the API. */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SmtpSettingsService {

	private final SmtpSettingsRepository settings;
	private final SecretBox secretBox;
	private final Mailer mailer;
	private final CurrentOwner currentOwner;
	private final CurrentOrganisation currentOrganisation;

	@Transactional(readOnly = true)
	public Optional<SmtpSettings> get() {
		return settings.findByOrganisationId(currentOrganisation.get().getId());
	}

	/** A null or blank password keeps the saved one. */
	public SmtpSettings save(String host, int port, SmtpSecurity security, String username, String password,
			String fromName, String fromEmail) {
		if (!secretBox.isAvailable()) {
			throw new SecretBox.SecretUnavailableException();
		}
		Long organisationId = currentOrganisation.get().getId();
		SmtpSettings saved = settings.findByOrganisationId(organisationId)
				.orElseGet(() -> SmtpSettings.forOrganisation(organisationId));
		saved.change(host, port, security, username, fromName, fromEmail);
		if (password != null && !password.isBlank()) {
			saved.replacePassword(secretBox.encrypt(password));
		}
		return settings.save(saved);
	}

	/**
	 * Sends a test email to the current Owner (their account email, else the sender address). Success turns
	 * subscriptions on (connected); any failure, including a refused server, records why (not working).
	 */
	public SmtpSettings test() {
		Organisation organisation = currentOrganisation.get();
		SmtpSettings saved = settings.findByOrganisationId(organisation.getId())
				.orElseThrow(() -> new ValidationException("Save your SMTP settings first."));
		String email = currentOwner.get().getEmail();
		String to = email != null ? email : saved.getFromEmail();
		try {
			mailer.send(saved, new OutgoingEmail(to, "Test email: your SMTP settings work",
					"Your SMTP settings work. Readers can now subscribe to your Published Roadmaps.",
					"<p>Your SMTP settings work. Readers can now subscribe to your Published Roadmaps.</p>"),
					organisation.getBrandName());
			saved.markConnected(Instant.now());
		} catch (Mailer.MailFailure | ValidationException e) {
			saved.markNotWorking(e.getMessage());
			log.error("Mail error", e);
		}
		return settings.save(saved);
	}
}
