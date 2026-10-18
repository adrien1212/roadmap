package fr.adriencaubel.roadmap.organisation;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Owner requests are scoped by Organisation; the anonymous lookup is by the hash of the link's secret. */
public interface InvitationRepository extends Repository<Invitation, Long> {

	Optional<Invitation> findByIdAndOrganisation(Long id, Organisation organisation);

	/** Earlier Invitations for an email, to replace them; the caller keeps the pending ones. */
	List<Invitation> findAllByOrganisationAndEmailAndUsedAtIsNullAndRevokedAtIsNull(Organisation organisation,
			String email);

	Invitation save(Invitation invitation);
}
