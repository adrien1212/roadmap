package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.ValidationException;

import java.util.Locale;

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

	/** JSON form: "owner", "admin", "editor". */
	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** An unknown key is a 400. */
	public static Role fromKey(String key) {
		for (Role role : values()) {
			if (role.key().equals(key)) {
				return role;
			}
		}
		throw new ValidationException("Unknown Role: " + key);
	}

	/** An Invitation can carry any Role but OWNER. */
	public boolean invitable() {
		return this != OWNER;
	}
}
