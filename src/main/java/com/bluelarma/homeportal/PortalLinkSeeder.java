package com.bluelarma.homeportal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PortalLinkSeeder implements ApplicationRunner {

	static final List<String> INITIAL_URLS = List.of("http://storenator.bluelarma.com:8081/",
			"http://storenator.bluelarma.com:8082/", "http://storenator.bluelarma.com:8083/");

	private static final Logger log = LoggerFactory.getLogger(PortalLinkSeeder.class);

	private final PortalLinkRepository links;

	private final PortalLinkService portalLinks;

	public PortalLinkSeeder(PortalLinkRepository links, PortalLinkService portalLinks) {
		this.links = links;
		this.portalLinks = portalLinks;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (links.count() > 0) {
			return;
		}
		portalLinks.seedIfEmpty(INITIAL_URLS);
		log.info("Seeded {} initial portal links", INITIAL_URLS.size());
	}

}
