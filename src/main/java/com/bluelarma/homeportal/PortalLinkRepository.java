package com.bluelarma.homeportal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PortalLinkRepository extends MongoRepository<PortalLink, String> {

	List<PortalLink> findAllByOrderByPositionAscIdAsc();

	boolean existsByUrl(String url);

	Optional<PortalLink> findFirstByOrderByPositionDesc();

	default int nextPosition() {
		return findFirstByOrderByPositionDesc().map(link -> link.getPosition() + 1).orElse(0);
	}

}
