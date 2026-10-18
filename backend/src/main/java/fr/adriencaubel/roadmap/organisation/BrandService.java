package fr.adriencaubel.roadmap.organisation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The current Organisation's Brand. */
@Service
@RequiredArgsConstructor
@Transactional
public class BrandService {

	private final CurrentOrganisation currentOrganisation;
	private final OrganisationRepository organisations;

	@Transactional(readOnly = true)
	public String get() {
		return currentOrganisation.get().getBrandName();
	}

	public String change(String name) {
		Organisation organisation = currentOrganisation.get();
		organisation.changeBrand(name);
		// Explicit save: the Organisation comes through the Owner, which may be loaded outside this transaction.
		return organisations.save(organisation).getBrandName();
	}
}
