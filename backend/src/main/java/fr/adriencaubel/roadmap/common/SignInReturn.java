package fr.adriencaubel.roadmap.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Where an Owner lands after Google sign-in: back on the join page they started from
 * (`/oauth2/authorization/google?continue=/join/{secret}`), otherwise their Roadmaps.
 * Only a join page path is kept, so `continue` cannot redirect anywhere else.
 */
public class SignInReturn implements OAuth2AuthorizationRequestResolver, AuthenticationSuccessHandler {

	static final String DEFAULT = "/roadmaps";
	private static final String ATTRIBUTE = SignInReturn.class.getName();
	private static final Pattern JOIN_PAGE = Pattern.compile("/join/[A-Za-z0-9_-]+(\\?accept=1)?");

	private final DefaultOAuth2AuthorizationRequestResolver resolver;

	public SignInReturn(ClientRegistrationRepository clients) {
		this.resolver = new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
	}

	/** The path to return to, or null when it is anything but a join page. */
	static String safe(String path) {
		return path != null && JOIN_PAGE.matcher(path).matches() ? path : null;
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
		OAuth2AuthorizationRequest authorization = resolver.resolve(request);
		if (authorization != null) {
			remember(request);
		}
		return authorization;
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
		OAuth2AuthorizationRequest authorization = resolver.resolve(request, clientRegistrationId);
		if (authorization != null) {
			remember(request);
		}
		return authorization;
	}

	private void remember(HttpServletRequest request) {
		String path = safe(request.getParameter("continue"));
		HttpSession session = request.getSession();
		if (path == null) {
			session.removeAttribute(ATTRIBUTE);
		} else {
			session.setAttribute(ATTRIBUTE, path);
		}
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		HttpSession session = request.getSession(false);
		String path = session == null ? null : safe((String) session.getAttribute(ATTRIBUTE));
		if (session != null) {
			session.removeAttribute(ATTRIBUTE);
		}
		response.sendRedirect(path != null ? path : DEFAULT);
	}
}
