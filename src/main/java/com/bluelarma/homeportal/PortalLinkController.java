package com.bluelarma.homeportal;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
public class PortalLinkController {

	private final PortalLinkService links;

	public PortalLinkController(PortalLinkService links) {
		this.links = links;
	}

	@GetMapping
	public List<PortalLinkResponse> list() {
		return links.list();
	}

	@PostMapping
	public ResponseEntity<PortalLinkResponse> add(@Valid @RequestBody CreateLinkRequest request) {
		PortalLinkResponse created = links.add(request.url(), request.label());
		return ResponseEntity.created(URI.create("/api/links/" + created.id())).body(created);
	}

	@PatchMapping("/{id}")
	public PortalLinkResponse rename(@PathVariable String id, @Valid @RequestBody UpdateLinkRequest request) {
		return links.rename(id, request.label());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		links.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/order")
	public List<PortalLinkResponse> reorder(@Valid @RequestBody ReorderRequest request) {
		return links.reorder(request.ids());
	}

}
