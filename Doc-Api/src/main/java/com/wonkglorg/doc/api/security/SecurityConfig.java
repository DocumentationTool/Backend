package com.wonkglorg.doc.api.security;

import com.wonkglorg.doc.api.properties.ApiProperties;
import com.wonkglorg.doc.api.security.filters.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
			.authorizeHttpRequests(auth -> auth.requestMatchers(apiProperties.getWhitelist().toArray(new String[0]))
											   .permitAll()
											   .anyRequest()
											   .authenticated())
			.exceptionHandling(e -> e.authenticationEntryPoint(new UserAuthenticationEntryPoint()))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
	
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer(){
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				// If you want to use the dynamic configuration from apiProperties, uncomment the following
                /*
                for (var origin : apiProperties.getCrossOrigin()) {
                    registry.addMapping(origin.getPath())
                            .allowedMethods(origin.getAllowedMethods().toArray(new String[0]))
                            .allowedHeaders(origin.getAllowedHeaders().toArray(new String[0]))
                            .allowedOrigins(origin.getOrigin());
                }
                */
				
				// Default configuration
				registry.addMapping("/**").allowedOrigins("https://www.markdoc.net") // Set your actual origin here
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed HTTP methods
						.allowedHeaders("*") // Allow all headers
						.allowCredentials(true); // If sending cookies with requests
			}
		};
	}
}
