package com.bluelarma.homeportal;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "spring.mongodb.database=homeportal-test")
@AutoConfigureMockMvc
class PortalLinkApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PortalLinkRepository repository;

	@Autowired
	private PortalLinkService service;

	@BeforeEach
	void resetToTheInitialLinks() {
		repository.deleteAll();
		service.seedIfEmpty(PortalLinkSeeder.INITIAL_URLS);
	}

	@Test
	void portalPageKeepsTheLinkListInTheDocument() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));

		mockMvc.perform(get("/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("id=\"link-list\"")))
			.andExpect(content().string(containsString("class=\"rail\"")))
			.andExpect(content().string(containsString("Add URL")))
			.andExpect(content().string(containsString("portal.js")));

		mockMvc.perform(get("/portal.css"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(".rail")));
	}

	@Test
	void listsTheInitialUrlsInOrder() throws Exception {
		mockMvc.perform(get("/api/links"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].url").value(contains("http://storenator.bluelarma.com:8081/",
					"http://storenator.bluelarma.com:8082/", "http://storenator.bluelarma.com:8083/")))
			.andExpect(jsonPath("$[*].label")
				.value(contains("Storenator 8081", "Storenator 8082", "Storenator 8083")))
			.andExpect(jsonPath("$[*].position").value(contains(0, 1, 2)));
	}

	@Test
	void seedingAgainDoesNotDuplicateLinks() throws Exception {
		service.seedIfEmpty(PortalLinkSeeder.INITIAL_URLS);

		mockMvc.perform(get("/api/links")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void addsALinkAtTheEndAndNormalizesIt() throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
			.content("{\"url\":\"HTTP://Example.COM/docs\",\"label\":\"  Docs  \"}"))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", containsString("/api/links/")))
			.andExpect(jsonPath("$.url").value("http://example.com/docs"))
			.andExpect(jsonPath("$.label").value("Docs"))
			.andExpect(jsonPath("$.position").value(3));

		mockMvc.perform(get("/api/links"))
			.andExpect(jsonPath("$.length()").value(4))
			.andExpect(jsonPath("$[3].url").value("http://example.com/docs"));
	}

	@Test
	void derivesALabelWhenNoneIsGiven() throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
			.content("{\"url\":\"http://storenator.bluelarma.com:8084\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.url").value("http://storenator.bluelarma.com:8084/"))
			.andExpect(jsonPath("$.label").value("Storenator 8084"));
	}

	@Test
	void rejectsADuplicateAndANonHttpUrl() throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
			.content("{\"url\":\"http://storenator.bluelarma.com:8081\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("That URL is already in the portal."));

		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"javascript:alert(1)\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(PortalUrls.ABSOLUTE_HTTP_MESSAGE));

		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Enter a URL."));
	}

	@Test
	void removesALinkAndClosesTheGap() throws Exception {
		List<String> ids = ids();

		mockMvc.perform(delete("/api/links/{id}", ids.get(1))).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/links"))
			.andExpect(jsonPath("$[*].url").value(contains("http://storenator.bluelarma.com:8081/",
					"http://storenator.bluelarma.com:8083/")))
			.andExpect(jsonPath("$[*].position").value(contains(0, 1)));
	}

	@Test
	void missingLinkIsNotFound() throws Exception {
		mockMvc.perform(delete("/api/links/{id}", "000000000000000000000000")).andExpect(status().isNotFound());
	}

	@Test
	void renamesALinkWithoutChangingItsUrl() throws Exception {
		String id = ids().getFirst();

		mockMvc.perform(patch("/api/links/{id}", id).contentType(MediaType.APPLICATION_JSON)
			.content("{\"label\":\"  Front door  \"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.label").value("Front door"))
			.andExpect(jsonPath("$.url").value("http://storenator.bluelarma.com:8081/"));
	}

	@Test
	void reordersLinksAndRejectsAPartialOrder() throws Exception {
		List<String> ids = ids();
		String reversed = "[\"" + ids.get(2) + "\",\"" + ids.get(1) + "\",\"" + ids.get(0) + "\"]";

		mockMvc.perform(put("/api/links/order").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":" + reversed + "}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].url").value(contains("http://storenator.bluelarma.com:8083/",
					"http://storenator.bluelarma.com:8082/", "http://storenator.bluelarma.com:8081/")))
			.andExpect(jsonPath("$[*].position").value(contains(0, 1, 2)));

		mockMvc.perform(put("/api/links/order").contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[\"" + ids.get(2) + "\",\"" + ids.get(1) + "\"]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("The new order must include every link exactly once."));

		mockMvc.perform(get("/api/links"))
			.andExpect(jsonPath("$[0].url").value("http://storenator.bluelarma.com:8083/"));
	}

	@Test
	void blocksCrossSiteMutations() throws Exception {
		mockMvc.perform(post("/api/links").header("Sec-Fetch-Site", "cross-site")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"url\":\"http://example.com/\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("Cross-site changes are blocked."));

		mockMvc.perform(get("/api/links")).andExpect(jsonPath("$.length()").value(3));
	}

	private List<String> ids() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/links")).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$[*].id");
	}

}
