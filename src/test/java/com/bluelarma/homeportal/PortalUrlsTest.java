package com.bluelarma.homeportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PortalUrlsTest {

	@Test
	void keepsTheInitialUrlsStable() {
		assertThat(PortalUrls.normalize("http://storenator.bluelarma.com:8081/"))
			.isEqualTo("http://storenator.bluelarma.com:8081/");
		assertThat(PortalUrls.normalize("http://storenator.bluelarma.com:8082/"))
			.isEqualTo("http://storenator.bluelarma.com:8082/");
		assertThat(PortalUrls.normalize("http://storenator.bluelarma.com:8083/"))
			.isEqualTo("http://storenator.bluelarma.com:8083/");
	}

	@Test
	void normalizesSchemeHostAndEmptyPath() {
		assertThat(PortalUrls.normalize("HTTP://StoreNator.BlueLarma.com:8084"))
			.isEqualTo("http://storenator.bluelarma.com:8084/");
	}

	@Test
	void preservesAnEncodedPath() {
		assertThat(PortalUrls.normalize("http://example.com/a%20b")).isEqualTo("http://example.com/a%20b");
	}

	@Test
	void rejectsUrlsThatAreNotAbsoluteHttp() {
		assertThatThrownBy(() -> PortalUrls.normalize("javascript:alert(1)")).isInstanceOf(InvalidUrlException.class);
		assertThatThrownBy(() -> PortalUrls.normalize("/relative")).isInstanceOf(InvalidUrlException.class);
		assertThatThrownBy(() -> PortalUrls.normalize("ftp://example.com/")).isInstanceOf(InvalidUrlException.class);
		assertThatThrownBy(() -> PortalUrls.normalize("http://user:secret@example.com/"))
			.isInstanceOf(InvalidUrlException.class);
	}

	@Test
	void derivesALabelFromTheHostAndPort() {
		assertThat(PortalUrls.labelFor("http://storenator.bluelarma.com:8081/")).isEqualTo("Storenator 8081");
		assertThat(PortalUrls.labelFor("http://example.com/docs")).isEqualTo("Example/docs");
	}

	@Test
	void treatsABlankLabelAsMissing() {
		assertThat(PortalUrls.cleanLabel("   ")).isEmpty();
		assertThat(PortalUrls.cleanLabel(null)).isEmpty();
		assertThat(PortalUrls.cleanLabel("  Dock  ")).contains("Dock");
	}

}
