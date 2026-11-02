package com.bluelarma.homeportal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

final class PortalUrls {

	static final int MAX_LABEL_LENGTH = 80;

	static final String ABSOLUTE_HTTP_MESSAGE = "Enter an absolute http or https URL.";

	private PortalUrls() {
	}

	static String normalize(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new InvalidUrlException(ABSOLUTE_HTTP_MESSAGE);
		}
		URI uri;
		try {
			uri = new URI(raw.trim());
		}
		catch (URISyntaxException ex) {
			throw new InvalidUrlException(ABSOLUTE_HTTP_MESSAGE);
		}
		String scheme = uri.getScheme();
		if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
			throw new InvalidUrlException(ABSOLUTE_HTTP_MESSAGE);
		}
		if (uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null) {
			throw new InvalidUrlException(ABSOLUTE_HTTP_MESSAGE);
		}
		String path = uri.getPath();
		if (path == null || path.isBlank()) {
			path = "/";
		}
		try {
			return new URI(scheme.toLowerCase(Locale.ROOT), null, uri.getHost().toLowerCase(Locale.ROOT), uri.getPort(),
					path, uri.getQuery(), null).toASCIIString();
		}
		catch (URISyntaxException ex) {
			throw new InvalidUrlException(ABSOLUTE_HTTP_MESSAGE);
		}
	}

	static Optional<String> cleanLabel(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return Optional.empty();
		}
		if (trimmed.length() > MAX_LABEL_LENGTH) {
			throw new InvalidLabelException("Labels must be 80 characters or fewer.");
		}
		return Optional.of(trimmed);
	}

	static String labelFor(String normalizedUrl) {
		URI uri = URI.create(normalizedUrl);
		String host = uri.getHost();
		int dot = host.indexOf('.');
		String name = dot > 0 ? host.substring(0, dot) : host;
		if (!name.isEmpty()) {
			name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
		}
		String label;
		if (uri.getPort() != -1) {
			label = name + " " + uri.getPort();
		}
		else if (uri.getPath() != null && uri.getPath().length() > 1) {
			label = name + uri.getPath();
		}
		else {
			label = name;
		}
		if (label.length() > MAX_LABEL_LENGTH) {
			return label.substring(0, MAX_LABEL_LENGTH);
		}
		return label;
	}

}
