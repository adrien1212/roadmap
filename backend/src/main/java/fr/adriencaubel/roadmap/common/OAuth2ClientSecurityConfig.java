package fr.adriencaubel.roadmap.common;

import fr.adriencaubel.roadmap.owner.OwnerProvisioningService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Google sign-in for Owners; Spring owns the login flow and the session cookie.
 * The browser reaches Spring through the frontend's proxy (/api, /oauth2, /login, /logout),
 * so redirects are built for the public origin (server.forward-headers-strategy=framework).
 */
@Configuration
@EnableWebSecurity
public class OAuth2ClientSecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, OwnerProvisioningService ownerProvisioning) throws Exception {
		return http
				.authorizeHttpRequests(authz -> authz
						// Readers are never signed in: Public page API, Feed, subscribe / confirm / unsubscribe.
						.requestMatchers("/api/public/**").permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers("/actuator/**").permitAll()
						// Everything else is the Owner's.
						.anyRequest().authenticated())
				// The frontend calls /api with fetch: answer 401 (it then shows sign-in), never a redirect to Google.
				.exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
						PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
				// Google only (SPEC): no form login. Each sign-in creates or refreshes the Owner,
				// then the Owner lands on their Roadmaps.
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfo -> userInfo.oidcUserService(ownerProvisioning))
						.defaultSuccessUrl("/roadmaps", true))
				.logout(logout -> logout.logoutSuccessUrl("/"))
				// CSRF for a browser app: the XSRF-TOKEN cookie is echoed in the X-XSRF-TOKEN header.
				// Readers' endpoints are exempt: they hold no session, and mail clients post the
				// one-click unsubscribe (RFC 8058) without any cookie.
				.csrf(csrf -> csrf.spa().ignoringRequestMatchers("/api/public/**"))
				.build();
	}
}
