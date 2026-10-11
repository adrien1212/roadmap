package fr.adriencaubel.roadmap.common;

/** Invalid input from the Owner; answered with 400. */
public class ValidationException extends RuntimeException {

	public ValidationException(String message) {
		super(message);
	}
}
