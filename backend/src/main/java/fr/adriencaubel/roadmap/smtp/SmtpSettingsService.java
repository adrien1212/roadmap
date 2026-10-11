package fr.adriencaubel.roadmap.smtp;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import fr.adriencaubel.roadmap.owner.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** The current Owner's SMTP settings. The password goes in sealed and never comes out through the API. */
@Service
@RequiredArgsConstructor
@Transactional
public class SmtpSettingsService {

	private final SmtpSettingsRepository settings;
	private final SecretBox secretBox;
	private final Mailer mailer;
	private final CurrentOwner currentOwner;

	@Transactional(readOnly = true)
	public Optional<SmtpSettings> get() {
		return settings.findByOwnerId(currentOwner.get().getId());
	}

	/** A null or blank password keeps the saved one. */
	public SmtpSettings save(String host, int port, SmtpSecurity security, String username, String password,
			String fromName, String fromEmail) {
		if (!secretBox.isAvailable()) {
			throw new SecretBox.SecretUnavailableException();
		}
		Long ownerId = currentOwner.get().getId();
		SmtpSettings saved = settings.findByOwnerId(ownerId).orElseGet(() -> SmtpSettings.forOwner(ownerId));
		saved.change(host, port, security, username, fromName, fromEmail);
		if (password != null && !password.isBlank()) {
			saved.replacePassword(secretBox.encrypt(password));
		}
		return settings.save(saved);
	}

	/**
	 * Sends a test email to the Owner (their account email, else the sender address). Success turns
	 * subscriptions on (connected); any failure, including a refused server, records why (not working).
	 */
	public SmtpSettings test() {
		Owner owner = currentOwner.get();
		SmtpSettings saved = settings.findByOwnerId(owner.getId())
				.orElseThrow(() -> new ValidationException("Save your SMTP settings first."));
		String to = owner.getEmail() != null ? owner.getEmail() : saved.getFromEmail();
		try {
			mailer.send(saved, new OutgoingEmail(to, "Test email: your SMTP settings work",
					"Your SMTP settings work. Readers can now subscribe to your Published Roadmaps.",
					"<p>Your SMTP settings work. Readers can now subscribe to your Published Roadmaps.</p>"));
			saved.markConnected(Instant.now());
		} catch (Mailer.MailFailure | ValidationException e) {
			saved.markNotWorking(e.getMessage());
		}
		return settings.save(saved);
	}
}
