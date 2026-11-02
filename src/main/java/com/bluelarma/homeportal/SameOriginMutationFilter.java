package com.bluelarma.homeportal;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Blocks browser requests that come from another site. Same-origin fetches and
 * non-browser clients (which do not send Sec-Fetch-Site) still work.
 */
@Component
public class SameOriginMutationFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (isApiMutation(request) && isCrossSite(request)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"message\":\"Cross-site changes are blocked.\"}");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private static boolean isApiMutation(HttpServletRequest request) {
		if (request.getRequestURI() == null || !request.getRequestURI().startsWith("/api/")) {
			return false;
		}
		String method = request.getMethod();
		return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
	}

	private static boolean isCrossSite(HttpServletRequest request) {
		String site = request.getHeader("Sec-Fetch-Site");
		return site != null && !"same-origin".equals(site) && !"none".equals(site);
	}

}
