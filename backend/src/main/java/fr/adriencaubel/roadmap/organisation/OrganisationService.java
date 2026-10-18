package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.owner.CurrentOwner;
import fr.adriencaubel.roadmap.owner.Owner;
import fr.adriencaubel.roadmap.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationService {

	private final OrganisationRepository organisations;
	private final OwnerRepository owners;
	private final CurrentOwner currentOwner;

	/** From the Welcome page: the current Owner creates an Organisation and becomes its Role Owner. */
	public Organisation create(String name) {
		Owner owner = currentOwner.get();
		Organisation organisation = organisations.save(new Organisation(name));
		// Already in one: 409, and the transaction drops the new Organisation.
		owner.joinAs(organisation, Role.OWNER, Instant.now());
		owners.save(owner);
		return organisation;
	}
}
