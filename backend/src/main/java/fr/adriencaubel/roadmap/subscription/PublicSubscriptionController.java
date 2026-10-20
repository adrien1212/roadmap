package fr.adriencaubel.roadmap.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Anonymous: Readers subscribe and confirm. */
@RestController
@RequiredArgsConstructor
public class PublicSubscriptionController {

	private final SubscriptionService service;

	/** Always the same answer, whether the address is new, pending or already subscribed. */
	@PostMapping("/api/public/roadmaps/{publicId}/subscriptions")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Message subscribe(@PathVariable String publicId, @RequestBody EmailBody body) {
		service.subscribe(publicId, body.email());
		return new Message("Check your inbox. Confirm your email to start getting updates.");
	}

	/** "Notify me when this ships": the same answer as subscribe, whatever the address. */
	@PostMapping("/api/public/roadmaps/{publicId}/items/{itemId}/subscriptions")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Message subscribeToItem(@PathVariable String publicId, @PathVariable Long itemId, @RequestBody EmailBody body) {
		service.subscribeToItem(publicId, itemId, body.email());
		return new Message("Check your inbox. Confirm your email to hear when this Item ships.");
	}

	/** Names the Item of an Item Subscription on the confirm page (and lets the browser remember it); nulls otherwise. */
	@PostMapping("/api/public/subscriptions/confirm")
	public SubscriptionService.Covered confirm(@RequestBody TokenBody body) {
		return service.confirm(body.token());
	}

	/**
	 * The token is in the query string, so both the unsubscribe page and mail clients' one-click button
	 * (RFC 8058, which posts to the List-Unsubscribe URL) can use the same address.
	 */
	@PostMapping("/api/public/subscriptions/unsubscribe")
	public SubscriptionService.Covered unsubscribe(@RequestParam String token) {
		return service.unsubscribe(token);
	}

	public record EmailBody(String email) {
	}

	public record TokenBody(String token) {
	}

	public record Message(String detail) {
	}
}
