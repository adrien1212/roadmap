package fr.adriencaubel.roadmap.common;

/** Unknown resource, or one that belongs to another Owner; answered with 404. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}
