package fr.adriencaubel.roadmap.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The current Owner's Brand. */
@Service
@RequiredArgsConstructor
@Transactional
public class BrandService {

	private final CurrentOwner currentOwner;
	private final OwnerRepository owners;

	@Transactional(readOnly = true)
	public String get() {
		return currentOwner.get().getBrandName();
	}

	public String change(String name) {
		Owner owner = currentOwner.get();
		owner.changeBrand(name);
		// Explicit save: a real CurrentOwner may hand back an Owner loaded outside this transaction.
		return owners.save(owner).getBrandName();
	}
}
