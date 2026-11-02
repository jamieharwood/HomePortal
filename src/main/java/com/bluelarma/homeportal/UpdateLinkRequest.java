package com.bluelarma.homeportal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLinkRequest(
		@NotBlank(message = "Enter a label.") @Size(max = 80, message = "Labels must be 80 characters or fewer.") String label) {
}
