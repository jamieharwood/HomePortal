package com.bluelarma.homeportal;

public record PortalLinkResponse(String id, String label, String url, int position) {

	static PortalLinkResponse from(PortalLink link) {
		return new PortalLinkResponse(link.getId(), link.getLabel(), link.getUrl(), link.getPosition());
	}

}
