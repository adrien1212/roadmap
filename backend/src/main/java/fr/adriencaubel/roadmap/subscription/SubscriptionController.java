package fr.adriencaubel.roadmap.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** The Owner's subscribed Readers of one Roadmap; another Owner's Roadmap gives 404. */
@RestController
@RequestMapping("/api/roadmaps/{roadmapId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

	private final SubscriptionService service;

	@GetMapping
	public ReadersResponse list(@PathVariable Long roadmapId) {
		SubscriptionService.ReadersOverview overview = service.overview(roadmapId);
		return new ReadersResponse(new Counts(overview.confirmed(), overview.pending()),
				overview.subscriptions().stream().map(SubscriptionResponse::of).toList());
	}

	@DeleteMapping("/{subscriptionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long roadmapId, @PathVariable Long subscriptionId) {
		service.remove(roadmapId, subscriptionId);
	}

	public record ReadersResponse(Counts counts, List<SubscriptionResponse> subscriptions) {
	}

	public record Counts(long confirmed, long pending) {
	}

	/** No token: it is the Reader's key, not the Owner's. */
	public record SubscriptionResponse(Long id, String email, String state, Instant createdAt, Instant confirmedAt) {

		static SubscriptionResponse of(Subscription s) {
			return new SubscriptionResponse(s.getId(), s.getEmail(), s.getState().key(), s.getCreatedAt(), s.getConfirmedAt());
		}
	}
}
