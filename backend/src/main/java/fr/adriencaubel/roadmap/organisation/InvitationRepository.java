package fr.adriencaubel.roadmap.organisation;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;

import java.util.List;
import java.util.Optional;

/** Owner requests are scoped by Organisation; the anonymous lookup is by the hash of the link's secret. */
public interface InvitationRepository extends Repository<Invitation, Long> {

	Optional<Invitation> findByIdAndOrganisation(Long id, Organisation organisation);

	/** Earlier Invitations for an email, to replace them; the caller keeps the pending ones. */
	List<Invitation> findAllByOrganisationAndEmailAndUsedAtIsNullAndRevokedAtIsNull(Organisation organisation,
			String email);

	/** For the join page; the caller checks it is pending. */
	Optional<Invitation> findBySecretHash(String secretHash);

	/** Single use under concurrency: 0 when someone else used it first, or it is no longer pending. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Invitation i set i.usedAt = :now
			where i.id = :id and i.usedAt is null and i.revokedAt is null and i.expiresAt > :now""")
	int markUsed(Long id, Instant now);

	Invitation save(Invitation invitation);
}
