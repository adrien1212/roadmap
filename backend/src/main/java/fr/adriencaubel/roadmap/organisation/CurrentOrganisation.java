package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The scoping seam: feature code asks this for the current Owner's Organisation
 * and scopes every query by it. Without an Organisation there is nothing of theirs: 404.
 */
@Component
@RequiredArgsConstructor
public class CurrentOrganisation {

	private final CurrentOwner currentOwner;

	public Organisation get() {
		Organisation organisation = currentOwner.get().getOrganisation();
		if (organisation == null) {
			throw new NotFoundException("No Organisation yet");
		}
		return organisation;
	}
}
