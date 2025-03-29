package com.wonkglorg.doc.api.controller;

import com.wonkglorg.doc.core.objects.RepoId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ResourceControllerTagTest extends BaseIntegrationTest{
	
	public ResourceControllerTagTest() {
		super(true);
	}
	
	@Test
	void testInsertResource() {
		//should fail not all required parameters given
		Assertions.assertNull(request.postForObject("/api/resource/tag/add", null, RestResponse.class));
		//should fail not all required parameters given
		Assertions.assertNull(request.postForObject("/api/resource/tag/add?repoId='repo1'", null, RestResponse.class));
		//should fail not all required parameters given
		Assertions.assertNull(request.postForObject("/api/resource/tag/add?repoId='repo1'&tagId='tag1'", null, RestResponse.class));
		RepoId repoId = repoService.getRepositories().keySet().iterator().next();
		
		//can't fail otherwise removetag did not work
		addTag(repoId.id(), "tag1", "Tag Name", true);
		//can and should fail because it already exists
		addTag(repoId.id(), "tag1", "Tag Name", false);
		addTag(repoId.id(), "tag2", "Tag Name2", true);
		
		removeTag(repoId.id(), "tag1");
		
		getTag(repoId.id());
	}
	
	/**
	 * Utility method to add tags and assertions for either result if it existed and got added or already existed and nothing happened
	 *
	 * @param repoId
	 * @param tagId
	 * @param tagName
	 * @param failOnExists
	 */
	private void addTag(String repoId, String tagId, String tagName, boolean failOnExists) {
		RestResponse restResponse = request.postForObject("/api/resource/tag/add?repoId=%s&tagId=%s&tagName=%s".formatted(repoId, tagId, tagName),
				null,
				RestResponse.class);
		if(restResponse.error() != null){
			if(failOnExists){
				Assertions.fail("Resource insertion marked as no fail, failed with error: '%s'".formatted(restResponse.error()));
			}
			Assertions.assertEquals("Tag '%s' already exists in repository '%s'".formatted(tagId, repoId), restResponse.error());
		} else {
			Assertions.assertEquals("Created tag '%s' in repo '%s'".formatted(tagId, repoId), restResponse.message());
		}
	}
	
	private void getTag(String repoId) {
		RestResponse<Map<String, String>> restResponse = request.postForObject("/api/resource/tag/get?repoId=%s".formatted(repoId),
				null,
				RestResponse.class);
		if(restResponse.error() != null){
			Assertions.fail("Resource insertion marked as no fail, failed with error: '%s'".formatted(restResponse.error()));
		} else {
			Assertions.assertEquals(restResponse.content(), Map.of("tag2", "Tag Name2"));
		}
	}
	
	/**
	 * Utility method to remove tags and assertions for either result if it existed and got removed or didn't exist and nothing happened
	 *
	 * @param repoId
	 * @param tagId
	 */
	private void removeTag(String repoId, String tagId) {
		RestResponse restResponse = request.postForObject("/api/resource/tag/remove?repoId=%s&tagId=%s".formatted(repoId, tagId),
				null,
				RestResponse.class);
		if(restResponse.error() != null){
			Assertions.assertEquals("Tag '%s' does not exist".formatted(tagId), restResponse.error());
		} else {
			Assertions.assertEquals("Removed tag '%s' from repo '%s'".formatted(tagId, repoId), restResponse.message());
		}
	}
	
}
