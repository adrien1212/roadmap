package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.common.ValidationException;

import java.util.Locale;

/** The fixed palette a Category colour is picked from; the JSON value is the lowercase key. */
public enum CategoryColour {
	VIOLET, EMERALD, ORANGE, PINK, SKY, AMBER, LIME, SLATE;

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static CategoryColour fromKey(String key) {
		for (CategoryColour colour : values()) {
			if (colour.key().equals(key)) {
				return colour;
			}
		}
		throw new ValidationException("Colour must be one of the palette keys, got: " + key);
	}
}
