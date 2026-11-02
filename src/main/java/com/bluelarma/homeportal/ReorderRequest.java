package com.bluelarma.homeportal;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record ReorderRequest(
		@NotNull(message = "Include every link in the new order.") List<@NotNull(message = "A link id is missing.") String> ids) {
}
