package fr.adriencaubel.roadmap.smtp;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Sends one email through an Owner's SMTP server. A sender is built per call from the settings;
 * the server is checked by the guard right before connecting.
 */
@Component
public class Mailer {

	private static final String TIMEOUT_MS = "10000";

	private final SmtpServerGuard guard;
	private final SecretBox secretBox;
	/** STARTTLS is required in production; Mailpit (dev) has no TLS. */
	private final boolean requireStartTls;

	public Mailer(SmtpServerGuard guard, SecretBox secretBox,
			@Value("${app.smtp.allow-private-hosts:false}") boolean allowPrivateHosts) {
		this.guard = guard;
		this.secretBox = secretBox;
		this.requireStartTls = !allowPrivateHosts;
	}

	/** @throws MailFailure with a message fit to show the Owner */
	public void send(SmtpSettings settings, OutgoingEmail email) {
		guard.check(settings.getHost(), settings.getPort());
		JavaMailSenderImpl sender = sender(settings);
		try {
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(new InternetAddress(settings.getFromEmail(), settings.getFromName(), StandardCharsets.UTF_8.name()));
			helper.setTo(email.to());
			helper.setSubject(email.subject());
			helper.setText(email.text(), email.html());
			for (Map.Entry<String, String> header : email.headers().entrySet()) {
				message.addHeader(header.getKey(), header.getValue());
			}
			sender.send(message);
		} catch (MailAuthenticationException e) {
			throw new MailFailure("The SMTP server refused the username or password.", e);
		} catch (MailException e) {
			throw new MailFailure("Couldn't send through " + settings.getHost() + " on port " + settings.getPort() + ": "
					+ e.getMostSpecificCause().getMessage(), e);
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new MailFailure("The email could not be built: " + e.getMessage(), e);
		}
	}

	private JavaMailSenderImpl sender(SmtpSettings settings) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(settings.getHost());
		sender.setPort(settings.getPort());
		sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
		if (settings.getUsername() != null) {
			sender.setUsername(settings.getUsername());
		}
		if (settings.hasPassword()) {
			sender.setPassword(secretBox.decrypt(settings.sealedPassword()));
		}
		Properties props = sender.getJavaMailProperties();
		props.put("mail.smtp.auth", String.valueOf(settings.getUsername() != null));
		props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
		props.put("mail.smtp.timeout", TIMEOUT_MS);
		props.put("mail.smtp.writetimeout", TIMEOUT_MS);
		if (settings.getSecurity() == SmtpSecurity.SSL) {
			props.put("mail.smtp.ssl.enable", "true");
			props.put("mail.smtp.ssl.checkserveridentity", "true");
		} else {
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.starttls.required", String.valueOf(requireStartTls));
			props.put("mail.smtp.ssl.checkserveridentity", "true");
		}
		return sender;
	}

	/** Sending failed; the message is shown to the Owner. */
	public static class MailFailure extends RuntimeException {
		MailFailure(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
