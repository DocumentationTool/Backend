package com.wonkglorg.doc.api.controller;

import com.wonkglorg.doc.api.service.RepoService;
import com.wonkglorg.doc.core.objects.RepoId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class ResourceControllerTest extends BaseIntegrationTest{
	private static final RestResponse<Map<?, ?>> NonResponse = new RestResponse<>(null, null, new HashMap<>());
	private static final Logger log = LoggerFactory.getLogger(ResourceControllerTest.class);
	public String token;
	
	public ResourceControllerTest() {
		super(false);
	}
	
	@TestConfiguration
	public class TestSecurityConfig{
		@Bean
		public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
			return http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
		}
	}
	
	@Test
	void testGetResources() {
        /*
        //empty request should not be valid
        ResourceRequest invalidRequest = new ResourceRequest();
        //should fail duo to missing required parameters
        Assertions.assertEquals(NonResponse, restTemplate.postForObject("/api/resource/get", invalidRequest, RestResponse.class));

        ResourceRequest validRequestWithWrongRepo = new ResourceRequest();
        validRequestWithWrongRepo.setRepoId("test");
        
        Assertions.assertEquals("Repo 'test' does not exist",
                restTemplate.postForObject("/api/resource/get", validRequestWithWrongRepo, RestResponse.class).error());

        ResourceRequest validRequestWithWrongUser = new ResourceRequest(null, null, "repo1", "test", null, null, false, 0);
        Assertions.assertEquals("User 'test' does not exist",
                restTemplate.postForObject("/api/resource/get", validRequestWithWrongUser, RestResponse.class).error());
                
         */
	}
	
	private void delete(String repoId, Path path) {
		request.postForObject("/api/resource/remove?repoId=" + repoId + "&path=" + path.toString(), null, RestResponse.class);
	}
	
	@Test
	void addResources() {
		RepoId first = repoService.getRepositories().keySet().stream().findFirst().get();
		
		//should fail duo to missing required parameters
		Assertions.assertEquals("Repo 'test' does not exist",
				request.postForObject("/api/resource/add?repoId=test&path=test&createdBy=test", "File Content", RestResponse.class).error());
		
		Assertions.assertEquals("Path 'test' file type is not allowed, only .md files are allowed",
				request.postForObject("/api/resource/add?repoId=%s&path=test&createdBy=test".formatted(first),
						"File Content",
						RestResponse.class).error());
		
		Assertions.assertEquals("Path '..\\test.md' cannot contain '..' to escape the current directory",
				request.postForObject("/api/resource/add?repoId=%s&path=../test.md&createdBy=test".formatted(first),
						"File Content",
						RestResponse.class).error());
		
		Assertions.assertEquals("Path '\\test.md' cannot start with a '/'",
				request.postForObject("/api/resource/add?repoId=%s&path=/test.md&createdBy=test".formatted(first),
						"File Content",
						RestResponse.class).error());
		
		//remove resource first before adding it again.
        /*
        Assertions.assertNotNull(restTemplate.postForObject("/api/resource/remove?repoId=%s&path=test.md".formatted(first), "File Content", RestResponse.class));

        Assertions.assertEquals("Successfully inserted 'test.md' Resource!",
                restTemplate.postForObject("/api/resource/add?repoId=%s&path=test.md&createdBy=test".formatted(first), "File Content", RestResponse.class)
                        .message());
                        
         */
	}
	
	@Test
	void removeResources() {
		
		RepoId first = repoService.getRepositories().keySet().stream().findFirst().get();
		//should fail duo to missing required parameters
		Assertions.assertEquals("Repo 'test' does not exist",
				request.postForObject("/api/resource/remove?repoId=test&path=test", null, RestResponse.class).error());
		
		Assertions.assertEquals("Path 'test' file type is not allowed, only .md files are allowed",
				request.postForObject("/api/resource/remove?repoId=%s&path=test".formatted(first), null, RestResponse.class).error());
		
		Assertions.assertEquals("Path '..\\test.md' cannot contain '..' to escape the current directory",
				request.postForObject("/api/resource/remove?repoId=%s&path=../test.md".formatted(first), null, RestResponse.class).error());
		
		Assertions.assertEquals("Path '\\test.md' cannot start with a '/'",
				request.postForObject("/api/resource/remove?repoId=%s&path=/test.md".formatted(first), null, RestResponse.class).error());

        
        /*
        Assertions.assertEquals("Successfully inserted 'test/remove/file.md' Resource!",
                restTemplate.postForObject("/api/resource/add?repoId=%s&path=test/remove/file.md&createdBy=test".formatted(first), "File Content", RestResponse.class)
                        .message());

        Assertions.assertEquals("Successfully removed 'test/remove/file.md' Resource!",
                restTemplate.postForObject("/api/resource/remove?repoId=%s&path=test/remove/file.md".formatted(first), null, RestResponse.class).message());
                
         */
	}
	
}
