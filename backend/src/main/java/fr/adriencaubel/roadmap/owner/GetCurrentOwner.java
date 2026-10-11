package fr.adriencaubel.roadmap.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCurrentOwner implements CurrentOwner {

    private final OwnerRepository owners;

    @Override
    public Owner get() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken token)
                || !(token.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new AuthenticationCredentialsNotFoundException("Sign in with Google first");
        }
        return owners.findByProviderAndProviderSubject(token.getAuthorizedClientRegistrationId(), oidcUser.getSubject())
                .orElseThrow(() -> new IllegalStateException("Owner not provisioned at sign-in"));
    }
}