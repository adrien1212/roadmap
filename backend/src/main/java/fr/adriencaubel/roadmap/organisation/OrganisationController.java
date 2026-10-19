package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.owner.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

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

	@GetMapping
	public OverviewResponse get() {
		OrganisationService.Overview overview = service.overview();
		return new OverviewResponse(overview.organisation().getName(),
				overview.people().stream().map(PersonResponse::of).toList(),
				overview.invitations().stream().map(InvitationResponse::of).toList());
	}

	@PutMapping
	public OrganisationResponse rename(@RequestBody OrganisationRequest request) {
		return OrganisationResponse.of(service.rename(request.name()));
	}

	@PutMapping("/people/{id}/role")
	public PersonResponse changeRole(@PathVariable Long id, @RequestBody RoleRequest request) {
		return PersonResponse.of(service.changeRole(id, request.role()));
	}

	/** Your own id: Leave. */
	@DeleteMapping("/people/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long id) {
		service.remove(id);
	}

	/** Role Owner only; the body repeats the Organisation name as confirmation. */
	@PostMapping("/close")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void close(@RequestBody OrganisationRequest request) {
		service.close(request.name());
	}

	public record OrganisationRequest(String name) {
	}

	public record RoleRequest(String role) {
	}

	public record OverviewResponse(String name, List<PersonResponse> people, List<InvitationResponse> invitations) {
	}

	public record PersonResponse(Long id, String name, String email, String role, Instant joinedAt) {

		static PersonResponse of(Owner person) {
			return new PersonResponse(person.getId(), person.getName(), person.getEmail(), person.getRole().key(),
					person.getJoinedAt());
		}
	}

	/** Pending only; never the link, which is not stored. */
	public record InvitationResponse(Long id, String email, String role, String invitedBy, Instant expiresAt) {

		static InvitationResponse of(Invitation invitation) {
			Owner inviter = invitation.getInvitedBy();
			return new InvitationResponse(invitation.getId(), invitation.getEmail(), invitation.getRole().key(),
					inviter.getName() != null ? inviter.getName() : inviter.getEmail(), invitation.getExpiresAt());
		}
	}

	public record OrganisationResponse(String name) {

		static OrganisationResponse of(Organisation organisation) {
			return new OrganisationResponse(organisation.getName());
		}
	}
}
