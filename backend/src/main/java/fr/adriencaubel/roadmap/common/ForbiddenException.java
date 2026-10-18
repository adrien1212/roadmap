package fr.adriencaubel.roadmap.common;

/** The signed-in Owner may not do this (e.g. accept an Invitation made for another email); answered with 403. */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}
}
