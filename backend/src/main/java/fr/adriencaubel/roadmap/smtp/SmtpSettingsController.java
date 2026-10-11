package fr.adriencaubel.roadmap.smtp;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** "Email to Readers" in Settings. The password is write-only: responses only say whether one is saved. */
@RestController
@RequestMapping("/api/smtp-settings")
@RequiredArgsConstructor
public class SmtpSettingsController {

	private final SmtpSettingsService service;

	@GetMapping
	public SmtpSettingsResponse get() {
		return service.get().map(SmtpSettingsResponse::of).orElse(SmtpSettingsResponse.NONE);
	}

	@PutMapping
	public SmtpSettingsResponse save(@RequestBody SmtpSettingsRequest request) {
		return SmtpSettingsResponse.of(service.save(request.host(), request.port() == null ? 0 : request.port(),
				SmtpSecurity.fromKey(request.security()), request.username(), request.password(), request.fromName(),
				request.fromEmail()));
	}

	public record SmtpSettingsRequest(String host, Integer port, String security, String username, String password,
			String fromName, String fromEmail) {
	}

	/** state: none | untested | connected | not_working. Never carries the password. */
	public record SmtpSettingsResponse(String state, String host, Integer port, String security, String username,
			boolean passwordSaved, String fromName, String fromEmail, String lastError, Instant testedAt) {

		static final SmtpSettingsResponse NONE =
				new SmtpSettingsResponse("none", null, null, null, null, false, null, null, null, null);

		static SmtpSettingsResponse of(SmtpSettings s) {
			return new SmtpSettingsResponse(s.getState().key(), s.getHost(), s.getPort(), s.getSecurity().key(),
					s.getUsername(), s.hasPassword(), s.getFromName(), s.getFromEmail(), s.getLastError(), s.getTestedAt());
		}
	}
}
