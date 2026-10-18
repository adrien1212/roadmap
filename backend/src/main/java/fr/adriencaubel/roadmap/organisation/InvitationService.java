package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.ForbiddenException;
import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import fr.adriencaubel.roadmap.owner.Owner;
import fr.adriencaubel.roadmap.owner.OwnerRepository;
import fr.adriencaubel.roadmap.smtp.Mailer;
import fr.adriencaubel.roadmap.smtp.SmtpSettings;
import fr.adriencaubel.roadmap.smtp.SmtpSettingsRepository;
import fr.adriencaubel.roadmap.smtp.SmtpState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InvitationService {

	private final InvitationRepository invitations;
	private final OrganisationRepository organisations;
	private final OwnerRepository owners;
	private final SmtpSettingsRepository smtpSettings;
	private final Mailer mailer;
	private final CurrentOwner currentOwner;
	private final CurrentOrganisation currentOrganisation;

	@Value("${app.public-url}")
	private String publicUrl;

	/** The link is in the result only: it is never stored, and shown once. */
	public record Created(Invitation invitation, String url, boolean emailed) {
	}

	/** Replaces any pending Invitation for the same email, then emails the link when the SMTP settings work. */
	public Created create(String email, String roleKey) {
		Organisation organisation = currentOrganisation.get();
		Owner inviter = currentOwner.get();
		Instant now = Instant.now();
		String secret = InvitationSecret.generate();
		Invitation invitation = Invitation.create(organisation, inviter, email, Role.fromKey(roleKey), secret, now);
		if (owners.existsByOrganisationAndEmailIgnoreCase(organisation, invitation.getEmail())) {
			throw new ConflictException(invitation.getEmail() + " is already in this Organisation");
		}
		invitations.findAllByOrganisationAndEmailAndUsedAtIsNullAndRevokedAtIsNull(organisation, invitation.getEmail())
				.forEach(earlier -> earlier.revoke(now));
		invitations.save(invitation);
		String url = publicUrl + "/join/" + secret;
		return new Created(invitation, url, send(invitation, inviter, url));
	}

	/** For the join page, before sign-in. Unknown, used, revoked, replaced and expired are the same 404. */
	@Transactional(readOnly = true)
	public Invitation pending(String secret) {
		return invitations.findBySecretHash(InvitationSecret.hash(secret))
				.filter(invitation -> invitation.isPending(Instant.now()))
				.orElseThrow(() -> new NotFoundException("This invitation is no longer valid"));
	}

	/**
	 * Only for the Google account with the Invitation's verified email; nothing is used up otherwise.
	 * Someone in an Organisation without Roadmaps moves (it is deleted when they were alone in it);
	 * with Roadmaps, they are refused.
	 */
	public void accept(String secret) {
		Invitation invitation = pending(secret);
		Owner owner = currentOwner.get();
		if (!invitation.isFor(owner)) {
			throw new ForbiddenException("This invitation is for " + invitation.maskedEmail()
					+ ". Sign in with that Google account.");
		}
		Organisation target = invitation.getOrganisation();
		Organisation current = owner.getOrganisation();
		if (current != null && current.getId().equals(target.getId())) {
			throw new ConflictException("You are already in " + target.getName());
		}
		Instant now = Instant.now();
		if (invitations.markUsed(invitation.getId(), now) == 0) {
			throw new NotFoundException("This invitation is no longer valid");
		}
		if (current != null) {
			moveOut(owner, current);
		}
		owner.joinAs(target, invitation.getRole(), now);
		owners.save(owner);
	}

	private void moveOut(Owner owner, Organisation current) {
		if (organisations.hasRoadmaps(current)) {
			throw new ConflictException("You already belong to " + current.getName()
					+ ". Leave it, or ask to be invited with another Google account.");
		}
		boolean alone = organisations.countPeople(current) == 1;
		owner.moveOut(alone);
		owners.saveAndFlush(owner);
		if (alone) {
			organisations.deleteCategories(current);
			organisations.deleteSmtpSettings(current.getId());
			organisations.deleteEmpty(current.getId());
		}
	}

	public void revoke(Long id) {
		invitations.findByIdAndOrganisation(id, currentOrganisation.get())
				.orElseThrow(() -> new NotFoundException("Invitation not found"))
				.revoke(Instant.now());
	}

	/** False when the email could not be sent: the Owner copies the link instead, so it is never an error. */
	private boolean send(Invitation invitation, Owner inviter, String url) {
		Organisation organisation = invitation.getOrganisation();
		SmtpSettings settings = smtpSettings.findByOrganisationId(organisation.getId()).orElse(null);
		if (settings == null || settings.getState() != SmtpState.CONNECTED) {
			return false;
		}
		String inviterName = inviter.getName() != null ? inviter.getName() : inviter.getEmail();
		try {
			mailer.send(settings, InvitationEmail.of(invitation.getEmail(), inviterName, organisation.getName(),
					invitation.getRole(), url, invitation.getExpiresAt()), organisation.getBrandName());
			return true;
		} catch (RuntimeException e) {
			log.warn("Invitation email not sent for Organisation {}: {}", organisation.getId(), e.getMessage());
			return false;
		}
	}
}
