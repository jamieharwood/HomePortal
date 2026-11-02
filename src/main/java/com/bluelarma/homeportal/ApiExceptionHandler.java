package com.bluelarma.homeportal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(LinkNotFoundException.class)
	ResponseEntity<ApiError> notFound(LinkNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
	}

	@ExceptionHandler(DuplicateLinkException.class)
	ResponseEntity<ApiError> duplicate(DuplicateLinkException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> conflict() {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("That URL is already in the portal."));
	}

	@ExceptionHandler({ InvalidUrlException.class, InvalidLabelException.class, InvalidOrderException.class })
	ResponseEntity<ApiError> badRequest(RuntimeException ex) {
		return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.findFirst()
			.map(error -> error.getDefaultMessage())
			.orElse("Check the form and try again.");
		return ResponseEntity.badRequest().body(new ApiError(message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> unreadable() {
		return ResponseEntity.badRequest().body(new ApiError("The request body could not be read."));
	}

}
