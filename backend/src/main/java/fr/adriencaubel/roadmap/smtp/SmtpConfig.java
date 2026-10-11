package fr.adriencaubel.roadmap.smtp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

@Configuration
public class SmtpConfig {

	@Bean
	SecretBox secretBox(@Value("${app.smtp.secret-key:}") String key) {
		return new SecretBox(key);
	}

	@Bean
	SmtpServerGuard smtpServerGuard(@Value("${app.smtp.allow-private-hosts:false}") boolean allowPrivateHosts) {
		return new SmtpServerGuard(allowPrivateHosts, InetAddress::getAllByName);
	}
}
