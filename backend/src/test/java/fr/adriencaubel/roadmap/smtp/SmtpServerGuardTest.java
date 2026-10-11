package fr.adriencaubel.roadmap.smtp;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmtpServerGuardTest {

	/** Resolves literal IPs only, so tests never hit DNS. */
	private static InetAddress[] literal(String host) throws UnknownHostException {
		return new InetAddress[] {InetAddress.getByName(host)};
	}

	private final SmtpServerGuard guard = new SmtpServerGuard(false, SmtpServerGuardTest::literal);

	@ParameterizedTest
	@ValueSource(ints = {25, 465, 587, 2525})
	void allowsSmtpPorts(int port) {
		assertThatNoException().isThrownBy(() -> guard.check("93.184.216.34", port));
	}

	@ParameterizedTest
	@ValueSource(ints = {22, 80, 443, 3306, 6379, 0, 70000})
	void refusesOtherPorts(int port) {
		assertThatThrownBy(() -> guard.check("93.184.216.34", port)).isInstanceOf(ValidationException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"127.0.0.1", "0.0.0.0", "10.1.2.3", "172.16.0.1", "192.168.1.10", "169.254.169.254",
			"100.64.0.1", "224.0.0.1", "255.255.255.255", "::1", "::", "fe80::1", "fc00::1", "fd12:3456::1",
			"::ffff:127.0.0.1"})
	void refusesHostsOnPrivateOrSpecialNetworks(String ip) {
		assertThatThrownBy(() -> guard.check(ip, 587)).isInstanceOf(ValidationException.class);
	}

	@Test
	void refusesAHostnameWhenAnyOfItsAddressesIsPrivate() {
		SmtpServerGuard mixed = new SmtpServerGuard(false,
				host -> new InetAddress[] {InetAddress.getByName("93.184.216.34"), InetAddress.getByName("10.0.0.5")});

		assertThatThrownBy(() -> mixed.check("smtp.example.com", 587)).isInstanceOf(ValidationException.class);
	}

	@Test
	void refusesAHostThatDoesNotResolve() {
		SmtpServerGuard unknown = new SmtpServerGuard(false, host -> {
			throw new UnknownHostException(host);
		});

		assertThatThrownBy(() -> unknown.check("nope.invalid", 587)).isInstanceOf(ValidationException.class);
	}

	@Test
	void refusesABlankHost() {
		assertThatThrownBy(() -> guard.check(" ", 587)).isInstanceOf(ValidationException.class);
	}

	@Test
	void allowsPrivateHostsOnlyWhenConfigured() {
		SmtpServerGuard dev = new SmtpServerGuard(true, SmtpServerGuardTest::literal);

		assertThatNoException().isThrownBy(() -> dev.check("127.0.0.1", 2525)); // Mailpit in dev
		assertThatThrownBy(() -> dev.check("127.0.0.1", 22)).isInstanceOf(ValidationException.class);
	}
}
