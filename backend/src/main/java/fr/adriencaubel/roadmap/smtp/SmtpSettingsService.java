package fr.adriencaubel.roadmap.smtp;

import fr.adriencaubel.roadmap.owner.CurrentOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** The current Owner's SMTP settings. The password goes in sealed and never comes out through the API. */
@Service
@RequiredArgsConstructor
@Transactional
public class SmtpSettingsService {

	private final SmtpSettingsRepository settings;
	private final SecretBox secretBox;
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
}
