package com.wonkglorg.doc.api.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.wonkglorg.doc.api.DocApiApplication.DEV_MODE;

/**
 * Validates the requests api key //UNUSED NOT NEEDED ANYMORE
 */
public class ApiKeyFilter extends OncePerRequestFilter {
	private static final String API_KEY_HEADER = "Authorization";
	private static final String API_KEY = "your-secret-api-key";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		//todo:jmd properly enable authentication when setup
		if (DEV_MODE) {
			filterChain.doFilter(request, response);
			return;
		}


		String apiKey = request.getHeader(API_KEY_HEADER);

		if (apiKey == null || !apiKey.equals("Bearer " + API_KEY)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().write("Unauthorized");
			return;
		}

		filterChain.doFilter(request, response);
	}
}
