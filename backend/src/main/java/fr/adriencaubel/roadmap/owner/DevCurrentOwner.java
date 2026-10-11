package fr.adriencaubel.roadmap.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Temporary {@link CurrentOwner}: every request runs as the dev/dev Owner. Deleted in Task 21. */
@Component
@RequiredArgsConstructor
public class DevCurrentOwner implements CurrentOwner {

	private static final String PROVIDER = "dev";
	private static final String SUBJECT = "dev";

	private final OwnerRepository owners;

	@Override
	@Transactional
	public Owner get() {
		return owners.findByProviderAndProviderSubject(PROVIDER, SUBJECT)
				.orElseGet(() -> owners.save(new Owner(PROVIDER, SUBJECT, "dev@example.com", "Dev Owner")));
	}
}
