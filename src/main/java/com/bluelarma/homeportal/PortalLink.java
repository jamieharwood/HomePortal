package com.bluelarma.homeportal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "portal_links")
public class PortalLink {

	@Id
	private String id;

	@Indexed(unique = true)
	private String url;

	private String label;

	private int position;

	public PortalLink(String url, String label, int position) {
		this.url = url;
		this.label = label;
		this.position = position;
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

}
