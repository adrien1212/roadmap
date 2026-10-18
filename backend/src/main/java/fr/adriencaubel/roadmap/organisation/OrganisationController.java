package fr.adriencaubel.roadmap.organisation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The current Owner's Organisation. Never shown to Readers. */
@RestController
@RequestMapping("/api/organisation")
@RequiredArgsConstructor
public class OrganisationController {

	private final OrganisationService service;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrganisationResponse create(@RequestBody OrganisationRequest request) {
		return OrganisationResponse.of(service.create(request.name()));
	}

	public record OrganisationRequest(String name) {
	}

	public record OrganisationResponse(String name) {

		static OrganisationResponse of(Organisation organisation) {
			return new OrganisationResponse(organisation.getName());
		}
	}
}
