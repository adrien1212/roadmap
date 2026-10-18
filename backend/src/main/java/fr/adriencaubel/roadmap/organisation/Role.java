package fr.adriencaubel.roadmap.organisation;

/**
 * What an Owner is in their Organisation. Recorded, not enforced yet (module `roles`):
 * only the rules on the Role Owner hold today.
 */
public enum Role {
	/** Created the Organisation; exactly one, cannot be removed, leave, or change Role. */
	OWNER,
	/** The same rights as the Owner. */
	ADMIN,
	/** Adds and edits Items. */
	EDITOR;

	/** An Invitation can carry any Role but OWNER. */
	public boolean invitable() {
		return this != OWNER;
	}
}
