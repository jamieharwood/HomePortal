package com.bluelarma.homeportal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
		@NotBlank(message = "Enter a URL.") @Size(max = 2000, message = "URLs must be 2000 characters or fewer.") String url,
		@Size(max = 80, message = "Labels must be 80 characters or fewer.") String label) {
}
