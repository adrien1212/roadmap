package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.common.ValidationException;

import java.util.Locale;

/** Where an Item stands, as asserted by the Owner; never inferred from dates. JSON value is the lowercase key. */
public enum ItemStatus {
	PLANNED, IN_PROGRESS, SHIPPED;

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static ItemStatus fromKey(String key) {
		for (ItemStatus status : values()) {
			if (status.key().equals(key)) {
				return status;
			}
		}
		throw new ValidationException("Status must be planned, in_progress or shipped, got: " + key);
	}
}
