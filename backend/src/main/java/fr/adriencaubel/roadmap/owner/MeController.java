package fr.adriencaubel.roadmap.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

	private final CurrentOwner currentOwner;

	@GetMapping("/api/me")
	public MeResponse me() {
		Owner owner = currentOwner.get();
		return new MeResponse(owner.getId(), owner.getName(), owner.getEmail(), owner.getProvider());
	}

	public record MeResponse(Long id, String name, String email, String provider) {
	}
}
