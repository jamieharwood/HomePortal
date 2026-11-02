package com.bluelarma.homeportal;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class PortalLinkService {

	private final PortalLinkRepository links;

	public PortalLinkService(PortalLinkRepository links) {
		this.links = links;
	}

	public List<PortalLinkResponse> list() {
		return responses();
	}

	public PortalLinkResponse add(String rawUrl, String rawLabel) {
		return PortalLinkResponse.from(create(rawUrl, rawLabel));
	}

	public void seedIfEmpty(List<String> urls) {
		if (links.count() > 0) {
			return;
		}
		for (String url : urls) {
			create(url, null);
		}
	}

	public PortalLinkResponse rename(String id, String rawLabel) {
		PortalLink link = required(id);
		String label = PortalUrls.cleanLabel(rawLabel)
			.orElseThrow(() -> new InvalidLabelException("Enter a label."));
		link.setLabel(label);
		return PortalLinkResponse.from(links.save(link));
	}

	public void delete(String id) {
		if (!links.existsById(id)) {
			throw new LinkNotFoundException(id);
		}
		links.deleteById(id);
		List<PortalLink> remaining = links.findAllByOrderByPositionAscIdAsc();
		for (int i = 0; i < remaining.size(); i++) {
			remaining.get(i).setPosition(i);
		}
		links.saveAll(remaining);
	}

	public List<PortalLinkResponse> reorder(List<String> ids) {
		List<PortalLink> current = links.findAllByOrderByPositionAscIdAsc();
		if (ids == null || ids.size() != current.size() || new HashSet<>(ids).size() != ids.size()) {
			throw new InvalidOrderException();
		}
		var byId = current.stream().collect(Collectors.toMap(PortalLink::getId, Function.identity()));
		if (!byId.keySet().equals(new HashSet<>(ids))) {
			throw new InvalidOrderException();
		}
		for (int i = 0; i < ids.size(); i++) {
			byId.get(ids.get(i)).setPosition(i);
		}
		links.saveAll(current);
		return responses();
	}

	private List<PortalLinkResponse> responses() {
		return links.findAllByOrderByPositionAscIdAsc().stream().map(PortalLinkResponse::from).toList();
	}

	private PortalLink create(String rawUrl, String rawLabel) {
		String url = PortalUrls.normalize(rawUrl);
		if (links.existsByUrl(url)) {
			throw new DuplicateLinkException();
		}
		String label = PortalUrls.cleanLabel(rawLabel).orElseGet(() -> PortalUrls.labelFor(url));
		return links.save(new PortalLink(url, label, links.nextPosition()));
	}

	private PortalLink required(String id) {
		return links.findById(id).orElseThrow(() -> new LinkNotFoundException(id));
	}

}
