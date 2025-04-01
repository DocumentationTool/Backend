package com.wonkglorg.doc.api.security;

import com.wonkglorg.doc.api.properties.ApiProperties;
import com.wonkglorg.doc.api.security.filters.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig{
	
	private final ApiProperties apiProperties;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	
	public SecurityConfig(ApiProperties apiProperties, JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.apiProperties = apiProperties;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}
	
	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(new BCryptPasswordEncoder());
		return new ProviderManager(authProvider);
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.requestMatchers("/**").permitAll())  // allow all paths
			.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()) // Allow OPTIONS method for all paths
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // add JWT filter
		return http.build();
	}
	
	@Profile("prod")
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer(){
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")// All endpoints
						.allowedOriginPatterns("https://www.markdoc.net", "http://localhost:*")//localhost:8080") // allowed origins
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // options specifically to allow cors
						.allowedHeaders("*") //all headers
						.allowCredentials(true); // cookies and credentials
			}
		};
	}
	
	@Profile({"test", "deployment"})
	@Bean
	public WebMvcConfigurer testCorsConfig() {
		return new WebMvcConfigurer(){
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedOriginPatterns("http://localhost:*").allowedMethods("*").allowedHeaders("*").allowCredentials(true);
			}
		};
	}
	
}
