package com.wonkglorg.doc.api.controller;

import com.wonkglorg.doc.api.service.RepoService;
import com.wonkglorg.doc.core.FileRepository;
import com.wonkglorg.doc.core.objects.RepoId;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseIntegrationTest{
	
	private static boolean deleteOnExit = false;
	
	private static Map<RepoId, FileRepository> repositories = new HashMap<>();
	
	@Autowired
	protected TestRestTemplate request;
	
	@Autowired
	protected RepoService repoService;
	
	public BaseIntegrationTest(boolean deleteOnExit) {
		BaseIntegrationTest.deleteOnExit = deleteOnExit;
	}
	
	@TestConfiguration
	public class TestSecurityConfig{
		@Bean
		public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
			return http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
		}
	}
	
	@PostConstruct
	public void initialize() {
		if(!deleteOnExit){
			return;
		}
		for(RepoId repoId : repoService.getRepositories().keySet()){
			repositories.put(repoId, repoService.getRepositories().get(repoId));
		}
	}
}
