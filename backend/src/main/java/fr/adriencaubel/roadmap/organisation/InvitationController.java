package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.owner.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

	private final InvitationService service;

	/** The only response that ever contains the link. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreatedInvitationResponse create(@RequestBody InvitationRequest request) {
		InvitationService.Created created = service.create(request.email(), request.role());
		return new CreatedInvitationResponse(created.invitation().getId(), created.url(),
				created.invitation().getExpiresAt(), created.emailed());
	}

	/** Anonymous: what the join page shows. No full email, so a leaked link says little. */
	@GetMapping("/{secret}")
	public PendingInvitationResponse get(@PathVariable String secret) {
		Invitation invitation = service.pending(secret);
		Owner inviter = invitation.getInvitedBy();
		return new PendingInvitationResponse(inviter.getName() != null ? inviter.getName() : "Someone",
				invitation.getOrganisation().getName(), invitation.getRole().key(), invitation.maskedEmail(),
				invitation.getExpiresAt());
	}

	@PostMapping("/{secret}/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void accept(@PathVariable String secret) {
		service.accept(secret);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revoke(@PathVariable Long id) {
		service.revoke(id);
	}

	public record InvitationRequest(String email, String role) {
	}

	public record PendingInvitationResponse(String inviterName, String organisationName, String role,
			String maskedEmail, Instant expiresAt) {
	}

	public record CreatedInvitationResponse(Long id, String url, Instant expiresAt, boolean emailed) {
	}
}
