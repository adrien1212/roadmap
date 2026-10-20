package fr.adriencaubel.roadmap.subscription;

/** A confirmed address asked to subscribe again: tell its inbox, never the page. */
record AlreadySubscribed(Long subscriptionId) {
}
