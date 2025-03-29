package com.wonkglorg.doc.api.controller;

import com.wonkglorg.doc.core.objects.RepoId;
import com.wonkglorg.doc.core.objects.UserId;
import com.wonkglorg.doc.core.request.ResourceRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class ResourceControllerTest extends BaseIntegrationTest{
	private static final Logger log = LoggerFactory.getLogger(ResourceControllerTest.class);
	public String token;
	
	public ResourceControllerTest() {
		super(false);
	}
	
	@Test
	void testGetResources() {
		
		RepoId repoId = repoService.getRepositories().keySet().iterator().next();
		
		ResourceRequest validRequestWithWrongRepo = new ResourceRequest();
		validRequestWithWrongRepo.setRepoId("test");
		
		Assertions.assertEquals("Repo 'test' does not exist",
				request.postForObject("/api/resource/get", validRequestWithWrongRepo, RestResponse.class).error());
		
		ResourceRequest validRequestWithWrongUser = new ResourceRequest(null, null, repoId, UserId.of("test"), null, null, false, 0);
		Assertions.assertEquals("User 'test' does not exist",
				request.postForObject("/api/resource/get", validRequestWithWrongUser, RestResponse.class).error());
		
		ResourceRequest validRequest = new ResourceRequest(null, null, repoId, null, null, null, false, 0);
		Assertions.assertEquals(null, request.postForObject("/api/resource/get", validRequest, RestResponse.class).error());
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
				request.postForObject("/api/resource/add?repoId=%s&path=test&createdBy=test".formatted(first), "File Content", RestResponse.class)
					   .error());
		
		Assertions.assertEquals("Path '..\\test.md' cannot contain '..' to escape the current directory",
				request.postForObject("/api/resource/add?repoId=%s&path=../test.md&createdBy=test".formatted(first),
						"File Content",
						RestResponse.class).error());
		
		Assertions.assertEquals("Path '\\test.md' cannot start with a '/'",
				request.postForObject("/api/resource/add?repoId=%s&path=/test.md&createdBy=test".formatted(first), "File Content", RestResponse.class)
					   .error());
		
		RestResponse restResponse = request.postForObject("/api/resource/add?repoId=%s&path=test.md&createdBy=test".formatted(first),
				"File Content",
				RestResponse.class);
		Assertions.assertEquals("Successfully inserted 'test.md' Resource!", restResponse.message());
		
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
		
		request.postForObject("/api/resource/remove?repoId=%s&path=test/remove/file.md".formatted(first), null, RestResponse.class);
		
		RestResponse restResponse = request.postForObject("/api/resource/add?repoId=%s&path=test/remove/file.md&createdBy=test".formatted(first),
				"File Content",
				RestResponse.class);
		Assertions.assertEquals("Successfully inserted 'test\\remove\\file.md' Resource!", restResponse.message());
		
		Assertions.assertEquals("Successfully removed 'test\\remove\\file.md' in '%s'".formatted(first),
				request.postForObject("/api/resource/remove?repoId=%s&path=test/remove/file.md".formatted(first), null, RestResponse.class)
					   .message());
		
	}
	
}
