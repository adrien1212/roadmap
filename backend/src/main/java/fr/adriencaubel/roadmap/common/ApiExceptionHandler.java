package fr.adriencaubel.roadmap.common;

import fr.adriencaubel.roadmap.smtp.SecretBox;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to RFC 9457 problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ValidationException.class)
	ProblemDetail validation(ValidationException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	/** No SMTP_SECRET_KEY on this server: Email to Readers can't be configured. */
	@ExceptionHandler(SecretBox.SecretUnavailableException.class)
	ProblemDetail secretUnavailable(SecretBox.SecretUnavailableException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
	}

	/** Safety net for unique constraints hit by a concurrent request. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail dataIntegrity(DataIntegrityViolationException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflicts with existing data");
	}
}
