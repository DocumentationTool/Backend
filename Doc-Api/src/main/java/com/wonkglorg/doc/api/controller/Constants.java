package com.wonkglorg.doc.api.controller;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Defines all the paths used in the controllers
 */
public class Constants{
	public class ControllerPaths{
		public static final String AUTH = "/auth";
		public static final String API_USER = "/api/user";
		public static final String API_GROUP = "/api/group";
		public static final String API_RESOURCE = "/api/resource";
		public static final String API_REPO = "/api/repo";
	}
	
	@Cacheable("sum")
	public int compute(int a, int b) {
		return a + b;
	}
	
	@Component
	public class Service{}
	
	@Component
	public class Foo{
		public Foo(Service service) {
		}
		
	}
	
}
