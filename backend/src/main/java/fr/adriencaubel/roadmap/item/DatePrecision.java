package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.common.ValidationException;

import java.util.Locale;

/** How exact one date of an Item is. Says nothing about Status. JSON value is the lowercase key. */
public enum DatePrecision {
	DAY, WEEK, MONTH, QUARTER;

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** A missing Precision is a Day, as every date was before Precisions existed. */
	public static DatePrecision fromKey(String key) {
		if (key == null) {
			return DAY;
		}
		for (DatePrecision precision : values()) {
			if (precision.key().equals(key)) {
				return precision;
			}
		}
		throw new ValidationException("Precision must be day, week, month or quarter, got: " + key);
	}
}
