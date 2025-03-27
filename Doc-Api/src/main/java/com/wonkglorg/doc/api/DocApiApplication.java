package com.wonkglorg.doc.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * The main entry point for the Doc API
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
public class DocApiApplication extends SpringBootServletInitializer{ //extend SpringBootServletInitializer to allow for deployment to a servlet container
	/**
	 * Bypasses permissions and allows full access to all endpoints
	 */
	public static final boolean DEV_MODE = true;
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(DocApiApplication.class);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(DocApiApplication.class, args);
	}
}
