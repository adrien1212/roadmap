package fr.adriencaubel.roadmap.smtp;

import fr.adriencaubel.roadmap.common.ValidationException;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Refuses SMTP servers that are not public mail servers. The Owner chooses the host our server
 * connects to, so without this check the feature would let anyone reach machines on our network.
 * Called right before every connection (test and each send).
 */
public class SmtpServerGuard {

	static final Set<Integer> ALLOWED_PORTS = Set.of(25, 465, 587, 2525);

	@FunctionalInterface
	public interface Resolver {
		InetAddress[] resolve(String host) throws UnknownHostException;
	}

	private final boolean allowPrivateHosts;
	private final Resolver resolver;

	/** @param allowPrivateHosts true only in local dev (Mailpit on localhost). */
	public SmtpServerGuard(boolean allowPrivateHosts, Resolver resolver) {
		this.allowPrivateHosts = allowPrivateHosts;
		this.resolver = resolver;
	}

	public void check(String host, int port) {
		if (!ALLOWED_PORTS.contains(port)) {
			throw new ValidationException("Port must be 25, 465, 587 or 2525");
		}
		if (host == null || host.isBlank()) {
			throw new ValidationException("SMTP host is required");
		}
		InetAddress[] addresses;
		try {
			addresses = resolver.resolve(host.strip());
		} catch (UnknownHostException e) {
			throw new ValidationException("Couldn't find the SMTP host " + host.strip());
		}
		if (allowPrivateHosts) {
			return;
		}
		for (InetAddress address : addresses) {
			if (isNotPublic(address)) {
				throw new ValidationException("This SMTP host is on a private network. Use your mail provider's public SMTP host.");
			}
		}
	}

	static boolean isNotPublic(InetAddress address) {
		if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
				|| address.isSiteLocalAddress() || address.isMulticastAddress()) {
			return true;
		}
		byte[] b = address.getAddress();
		if (address instanceof Inet4Address) {
			int first = b[0] & 0xff, second = b[1] & 0xff;
			return first == 0                                     // 0.0.0.0/8
					|| (first == 100 && second >= 64 && second <= 127) // 100.64.0.0/10, carrier-grade NAT
					|| (first == 255);                            // broadcast
		}
		if (address instanceof Inet6Address) {
			return (b[0] & 0xfe) == 0xfc;                         // fc00::/7, unique local
		}
		return true;
	}
}
