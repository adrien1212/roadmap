package fr.adriencaubel.roadmap.common;

/** The request clashes with existing data (e.g. a duplicate name); answered with 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
