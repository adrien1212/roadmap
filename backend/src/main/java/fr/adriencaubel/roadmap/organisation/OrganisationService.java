package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import fr.adriencaubel.roadmap.owner.Owner;
import fr.adriencaubel.roadmap.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationService {

	private final OrganisationRepository organisations;
	private final OwnerRepository owners;
	private final InvitationRepository invitations;
	private final CurrentOwner currentOwner;
	private final CurrentOrganisation currentOrganisation;

	/** From the Welcome page: the current Owner creates an Organisation and becomes its Role Owner. */
	public Organisation create(String name) {
		Owner owner = currentOwner.get();
		Organisation organisation = organisations.save(new Organisation(name));
		// Already in one: 409, and the transaction drops the new Organisation.
		owner.joinAs(organisation, Role.OWNER, Instant.now());
		owners.save(owner);
		return organisation;
	}

	/** What Settings shows: the Organisation, its people and its pending Invitations. */
	public record Overview(Organisation organisation, List<Owner> people, List<Invitation> invitations) {
	}

	@Transactional(readOnly = true)
	public Overview overview() {
		Organisation organisation = currentOrganisation.get();
		return new Overview(organisation, owners.findAllByOrganisationOrderByJoinedAtAscIdAsc(organisation),
				invitations.findAllByOrganisationAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAsc(
						organisation, Instant.now()));
	}

	public Organisation rename(String name) {
		Organisation organisation = currentOrganisation.get();
		organisation.rename(name);
		// Explicit save: the Organisation comes through the Owner, which may be loaded outside this transaction.
		return organisations.save(organisation);
	}

	/** Between Admin and Editor; the Role Owner never changes (409). */
	public Owner changeRole(Long personId, String roleKey) {
		Owner person = person(personId);
		person.changeRole(Role.fromKey(roleKey));
		return owners.save(person);
	}

	/** Removing yourself is leaving. The Role Owner can be neither removed nor leave (409). */
	public void remove(Long personId) {
		Owner person = person(personId);
		person.leave();
		owners.save(person);
	}

	/** Role Owner only. Roadmaps Private, Invitations revoked, everyone out; nothing deleted. */
	public void close(String typedName) {
		Organisation organisation = currentOrganisation.get();
		Instant now = Instant.now();
		organisation.close(currentOwner.get(), typedName, now);
		organisations.save(organisation);
		organisations.unpublishRoadmaps(organisation);
		organisations.revokeInvitations(organisation, now);
		organisations.removeEveryone(organisation);
	}

	private Owner person(Long id) {
		return owners.findByIdAndOrganisation(id, currentOrganisation.get())
				.orElseThrow(() -> new NotFoundException("Person not found"));
	}
}
