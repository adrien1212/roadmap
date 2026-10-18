package fr.adriencaubel.roadmap.owner;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Runs at each Google sign-in: the first one creates the Owner, later ones refresh its email and name.
 * An Owner is identified by provider (the registration id, "google") + the OIDC subject.
 */
@Service
public class OwnerProvisioningService extends OidcUserService {

	private final OwnerRepository owners;

	public OwnerProvisioningService(OwnerRepository owners) {
		this.owners = owners;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
		OidcUser user = super.loadUser(request);
		String provider = request.getClientRegistration().getRegistrationId();
		String subject = user.getSubject();

		Owner owner = owners.findByProviderAndProviderSubject(provider, subject)
				.orElseGet(() -> create(provider, subject, user));
		owner.refreshProfile(user.getEmail(), user.getFullName(), Boolean.TRUE.equals(user.getEmailVerified()));
		owners.save(owner);
		return user;
	}

	private Owner create(String provider, String subject, OidcUser user) {
		try {
			return owners.save(new Owner(provider, subject, user.getEmail(), user.getFullName()));
		} catch (DataIntegrityViolationException e) {
			// Two first sign-ins at once: the other one created it.
			return owners.findByProviderAndProviderSubject(provider, subject).orElseThrow(() -> e);
		}
	}
}
