package fr.adriencaubel.roadmap.owner;

import fr.adriencaubel.roadmap.organisation.Organisation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

	private final CurrentOwner currentOwner;

	/** role and organisation are null until the Owner creates or joins an Organisation (Welcome page). */
	@GetMapping("/api/me")
	@Transactional(readOnly = true)
	public MeResponse me() {
		Owner owner = currentOwner.get();
		Organisation organisation = owner.getOrganisation();
		return new MeResponse(owner.getId(), owner.getName(), owner.getEmail(), owner.getProvider(),
				owner.getRole() == null ? null : owner.getRole().key(),
				organisation == null ? null : new MeOrganisation(organisation.getName()));
	}

	public record MeResponse(Long id, String name, String email, String provider, String role,
			MeOrganisation organisation) {
	}

	public record MeOrganisation(String name) {
	}
}
