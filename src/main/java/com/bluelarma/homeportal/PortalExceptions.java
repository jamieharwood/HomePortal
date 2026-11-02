package com.bluelarma.homeportal;

class InvalidUrlException extends RuntimeException {

	InvalidUrlException(String message) {
		super(message);
	}

}

class InvalidLabelException extends RuntimeException {

	InvalidLabelException(String message) {
		super(message);
	}

}

class DuplicateLinkException extends RuntimeException {

	DuplicateLinkException() {
		super("That URL is already in the portal.");
	}

}

class LinkNotFoundException extends RuntimeException {

	LinkNotFoundException(String id) {
		super("No link with id " + id + ".");
	}

}

class InvalidOrderException extends RuntimeException {

	InvalidOrderException() {
		super("The new order must include every link exactly once.");
	}

}
