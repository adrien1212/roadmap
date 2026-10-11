package fr.adriencaubel.roadmap.subscription;

/** A confirmation email must be sent once the Subscription is saved. */
record ConfirmationRequested(Long subscriptionId) {
}
