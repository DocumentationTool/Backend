package com.wonkglorg.doc.api.controller;

import com.wonkglorg.doc.api.json.JsonFileTree;
import com.wonkglorg.doc.api.json.JsonResource;
import com.wonkglorg.doc.api.json.JsonResourceEdit;
import com.wonkglorg.doc.api.service.ResourceService;
import com.wonkglorg.doc.core.exception.client.ClientException;
import com.wonkglorg.doc.core.objects.*;
import com.wonkglorg.doc.core.request.ResourceRequest;
import com.wonkglorg.doc.core.request.ResourceUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.wonkglorg.doc.api.controller.Constants.ControllerPaths.API_RESOURCE;
import static com.wonkglorg.doc.core.path.TargetPath.normalizePath;

/**
 * Rest Controller endpoint for API resource requests
 */
@RestController
@RequestMapping(API_RESOURCE)
public class ApiResourceController {
    private static final Logger log = LoggerFactory.getLogger(ApiResourceController.class);
    private final ResourceService resourceService;

    public ApiResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Returns resources by the specified request.
     *
     * @param request the {@link ResourceRequest} to get resources
     * @return {@link RestResponse}
     */
    @Operation(summary = "Gets a resource", description = """
            ## Returns resources by the specified request.
            
            ### searchTerm
            if null is given returns the result regardless of its file contents.
            ### path
            if null is given returns the result for all resources in the repository.
            ### repoId
            if null is given returns the result for all repositories.
            ### userId
            if null is given returns regardless of permissions, if a valid user is given limits the search to any file the user would have access to.
            ### whiteListTags
            requires a resource to at least have this tag to be returned.
            ### blacklistListTags
            requires a resource to not have this tag to be returned. If a resource has a whitelisted tag and a blacklisted tag it will not be returned as the blacklisted tag takes precedence.
            ### withData
            if true returns the data of the resource.
            ### returnLimit
            limits the amount of resources returned.
            """)
    @PostMapping("/get")
    public ResponseEntity<RestResponse<Map<String, List<JsonResource>>>> getResources(@RequestBody ResourceRequest request) {
        try {
            var resources = resourceService.getResources(request);
            Map<String, List<JsonResource>> jsonResources = new HashMap<>();

            for (var resource : resources) {
                jsonResources.computeIfAbsent(resource.repoId().id(), s -> new ArrayList<>()).add(JsonResource.of(resource));
            }
            return RestResponse.success(jsonResources).toResponse();

        } catch (ClientException e) {
            return RestResponse.<Map<String, List<JsonResource>>>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while retrieving resources", e);
            return RestResponse.<Map<String, List<JsonResource>>>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Constructs a file tree out of the given resource request.
     *
     * @param request the {@link ResourceRequest} to get resources
     * @return {@link RestResponse}
     */
    @Operation(summary = "Constructs a file tree", description = """
            ## Constructs a file tree out of the given resource request.
            
            ### searchTerm
            if null is given returns the result regardless of its file contents.
            ### path
            if null is given returns the result for all resources in the repository.
            ### repoId
            if null is given returns the result for all repositories.
            ### userId
            if null is given returns regardless of permissions, if a valid user is given limits the search to any file the user would have access to.
            ### whiteListTags
            requires a resource to at least have this tag to be returned.
            ### blacklistListTags
            requires a resource to not have this tag to be returned. If a resource has a whitelisted tag and a blacklisted tag it will not be returned as the blacklisted tag takes precedence.
            ### withData
            if true returns the data of the resource.
            ### returnLimit
            limits the amount of resources returned.
            """)
    @PostMapping("/get/filetree")
    public ResponseEntity<RestResponse<Map<String, JsonFileTree>>> getFiletree(@RequestBody ResourceRequest request) {
        try {
            var resources = resourceService.getResources(request);

            Map<String, JsonFileTree> fileTrees = new HashMap<>();

            for (var resource : resources) {
                String repo = resource.repoId().id();
                String normalizedPath = resource.resourcePath().toString().replace("\\", "/");
                String[] pathSegments = normalizedPath.split("/");

                //build file tree
                JsonFileTree current = fileTrees.computeIfAbsent(repo, JsonFileTree::new);
                for (int i = 0; i < pathSegments.length; i++) {
                    String pathPart = pathSegments[i];

                    if (i == pathSegments.length - 1 && pathPart.contains(".")) {
                        // is a file
                        current.addResource(resource);
                    } else {
                        //its a directory
                        current = current.add(pathPart);
                    }
                }
            }

            return RestResponse.success(fileTrees).toResponse();

        } catch (
                ClientException e) {//client exceptions are stuff only returned to the client, and isn't an actual error that needs fixing by the coder
            return RestResponse.<Map<String, JsonFileTree>>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while retrieving filetree", e);
            return RestResponse.<Map<String, JsonFileTree>>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Adds a resource to the repository.
     *
     * @param repoId    the repository id
     * @param path      the path the resource should be located at
     * @param createdBy the user who created the resource
     * @param category  the category of the resource
     * @param tagIds    the tags of the resource (optional)
     * @param content   the content of the resource
     * @return {@link RestResponse}
     */
    @Operation(summary = "Adds a resource", description = """
            ## Body
            The data the file should contain.
            """)
    @PostMapping("/add")
    public ResponseEntity<RestResponse<Void>> insertResource(@Parameter(description = "The repoId to search in.") @RequestParam("repoId") String repoId,
                                                             @Parameter(description = "The path to the resource.") @RequestParam("path") String path,
                                                             @Parameter(description = "The user who created the resource.") @RequestParam("createdBy") String createdBy,
                                                             @Parameter(description = "The category of the resource.") @RequestParam(value = "category", required = false) String category,
                                                             @RequestParam(value = "tagIds", required = false) List<String> tagIds,
                                                             @RequestBody String content) {
        try {
            Set<TagId> tags;
            if (tagIds != null && !tagIds.isEmpty()) {
                tags = tagIds.stream().map(TagId::new).collect(Collectors.toSet());
            } else {
                tags = new HashSet<>();
            }
            Resource resource = new Resource(Path.of(path), createdBy, RepoId.of(repoId), category, tags, content);
            resourceService.insertResource(resource);
            return RestResponse.<Void>success("Successfully inserted '%s' Resource!".formatted(normalizePath(path)), null).toResponse();
        } catch (ClientException e) {
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Failed while trying to insert Resource", e);
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Updates a resource in the repository.
     *
     * @param request {@link ResourceUpdateRequest}
     * @return {@link RestResponse}
     */
    @Operation(summary = "Updates a resource", description = "Updates a resource in the Repository.")
    @PostMapping("/update")
    public ResponseEntity<RestResponse<Void>> updateResource(@RequestBody ResourceUpdateRequest request) {
        try {
            resourceService.updateResource(request);
            return RestResponse.<Void>success("Successfully updated Resource '%s' for repo '%s'!".formatted(request.path(), request.repoId()), null)
                    .toResponse();
        } catch (ClientException e) {
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Failed to update resource", e);
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Removes a resource from the repository.
     *
     * @param repo the repository id
     * @param path the path of the resource
     * @return {@link RestResponse}
     */
    @Operation(summary = "Removes a resource", description = "Removes a resource from the Repository.")
    @PostMapping("/remove")
    public ResponseEntity<RestResponse<Void>> removeResource(@RequestParam("repoId") String repo, @RequestParam("path") String path) {
        try {
            boolean removed = resourceService.removeResource(RepoId.of(repo), Path.of(path));
            return RestResponse.<Void>success("Successfully removed '%s' in '%s'".formatted(path, repo), null).toResponse();
        } catch (ClientException e) {
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while removing resources", e);
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Moves a resource from one destination to another. if a second repo is not given or is same as the first moves the resource within the same repository
     *
     * @param userId   the user id
     * @param repoFrom the repository id to move from
     * @param from     the path to move from
     * @param repoTo   the repository id to move to (optional)
     * @param to       the path to move to
     * @return {@link RestResponse}
     */
    @Operation(summary = "Moves a resource", description = "Moves a resource from one destination to another.")
    @PostMapping("/move")
    public ResponseEntity<RestResponse<Void>> moveResource(@RequestParam("userId") String userId,
                                                           @RequestParam("repoFrom") String repoFrom,
                                                           @RequestParam("pathFrom") String from,
                                                           @RequestParam(value = "repoTo", required = false) String repoTo,
                                                           @RequestParam("pathTo") String to) {
        try {
            if (repoTo == null || repoTo.equals(repoFrom)) {
                resourceService.moveResource(RepoId.of(repoFrom), Path.of(from), Path.of(to));
            } else {
                resourceService.moveToOtherRepo(UserId.of(userId), RepoId.of(repoFrom), Path.of(from), RepoId.of(repoTo), Path.of(to));
            }
            return RestResponse.<Void>success("Successfully moved '%s' to '%s'".formatted(from, to), null).toResponse();
        } catch (ClientException e) {
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while moving resource", e);
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Adds a new Tag to the repository.
     *
     * @param repoId  The repoId to add the tag in.
     * @param tagId   the id of the tag to be added.
     * @param tagName the name to display for the tag id.
     * @return {@link RestResponse}
     */
    @Operation(summary = "Adds a new Tag", description = "Adds a new Tag to the repository.")
    @PostMapping("tag/add")
    public ResponseEntity<RestResponse<Void>> addTag(@Parameter(description = "The repoId to add the tag in.") @RequestParam("repoId") String repoId,
                                                     @Parameter(description = "the id of the tag to be added.") @RequestParam("tagId") String tagId,
                                                     @Parameter(description = "the name to display for the tag id.") @RequestParam("tagName") String tagName) {
        try {
            resourceService.createTag(RepoId.of(repoId), new Tag(new TagId(tagId), tagName));
            return RestResponse.<Void>success("Created tag '%s' in repo '%s'".formatted(tagId, repoId), null).toResponse();
        } catch (ClientException e) {
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while adding tag", e);
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Removes a Tag from the repository.
     *
     * @param repoId The repoId to remove the tag from.
     * @param tagId  The tagId to remove.
     * @return {@link RestResponse}
     */
    @Operation(summary = "Removes a Tag", description = "The tag to remove from the repository.")
    @PostMapping("/tag/remove")
    public ResponseEntity<RestResponse<Void>> removeTag(@Parameter(description = "The repoId to remove the tag from.") @RequestParam("repoId") String repoId,
                                                        @Parameter(description = "The tagId to remove") @RequestParam("tagId") String tagId) {
        try {
            resourceService.removeTag(RepoId.of(repoId), new TagId(tagId));
            return RestResponse.<Void>success("Removed tag '%s' from repo '%s'".formatted(tagId, repoId), null).toResponse();
        } catch (ClientException e) {
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while removing resource", e);
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Retrieves all Tags from a specific repository.
     *
     * @param repoId The repoId to remove the tag from or null to remove the tag from all repositories.
     * @return {@link RestResponse}
     */
    @Operation(summary = "Retrieves all Tags", description = "Retrieves all tags from a specific repository.")
    @PostMapping("/tag/get")
    public ResponseEntity<RestResponse<Map<String, String>>> getTags(@Parameter(description = "The repoId to remove the tag from or null to remove the tag from all repositories.") @RequestParam(value = "repoId", required = false) String repoId) {
        try {
            List<Tag> tags = resourceService.getTags(repoId == null ? RepoId.ALL_REPOS : RepoId.of(repoId));
            return RestResponse.success(tags.stream().collect(Collectors.toMap(tag -> tag.tagId().id(), Tag::tagName))).toResponse();
        } catch (ClientException e) {
            return RestResponse.<Map<String, String>>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while adding resource", e);
            return RestResponse.<Map<String, String>>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Retrieves all Tags from a specific repository.
     *
     * @param repoId The repoId to remove the tag from or null to remove the tag from all repositories.
     * @param path   The path to set as a tag.
     * @param userId The userId to remove the tag from or null to remove the tag from all users.
     * @return {@link RestResponse}
     */
    @Operation(summary = "Sets a resource as being edited", description = "Sets a resource as being edited. Needs to be released manually by the user.")
    @PostMapping("/editing/set")
    public ResponseEntity<RestResponse<Void>> setEditing(@RequestParam("repoId") String repoId,
                                                         @RequestParam("path") String path,
                                                         @RequestParam("userId") String userId) {
        try {
            resourceService.setCurrentlyEdited(RepoId.of(repoId), UserId.of(userId), Path.of(path));
            return RestResponse.<Void>success("Set '%s' as being edited by '%s'".formatted(path, repoId), null).toResponse();
        } catch (
                ClientException e) { //core exceptions are stuff only returned to the client, and isn't an actual error that needs fixing by the coder
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while setting edited ", e);
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        }
    }

    /**
     * Checks if a resource is being edited.
     *
     * @param repoId the repoId to check if the resource is being edited
     * @param path   the path to check if it is being edited
     * @return {@link RestResponse}
     */
    @Operation(summary = "Checks if a resource is being edited", description = "Returns information about a files editing status.")
    @GetMapping("/editing/get")
    public ResponseEntity<RestResponse<JsonResourceEdit>> isBeingEdited(@RequestParam("repoId") String repoId, @RequestParam("path") String path) {
        try {
            UserId editingUser = resourceService.getEditingUser(RepoId.of(repoId), Path.of(path));
            JsonResourceEdit response = new JsonResourceEdit();
            response.editingUser = editingUser != null ? editingUser.id() : null;
            response.isBeingEdited = editingUser != null;
            response.file = path;
            return RestResponse.success(response).toResponse();
        } catch (ClientException e) {
            return RestResponse.<JsonResourceEdit>error(e.getMessage()).toResponse(HttpStatusCode.valueOf(404));
        } catch (Exception e) {
            log.error("Error while checking edited state ", e);
            return RestResponse.<JsonResourceEdit>error(e.getMessage()).toResponse();
        }
    }


    /**
     * Removes a file as being edited.
     *
     * @param repoId the repoId to remove the resource from being edited
     * @param path   the path to remove from being edited
     * @return {@link RestResponse}
     */
    @Operation(summary = "Removes a file as being edited", description = "Frees up a resource from its editing lock allowing anyone to take and edit it again.")
    @PostMapping("/editing/remove")
    public ResponseEntity<RestResponse<Void>> removeEditedState(@RequestParam("repoId") String repoId, @RequestParam("path") String path) {
        try {
            resourceService.removeCurrentlyEdited(RepoId.of(repoId), Path.of(path));
            return RestResponse.<Void>success("Removed '%s' as being edited".formatted(path), null).toResponse();
        } catch (
                ClientException e) {//core exceptions are stuff only returned to the client, and isn't an actual error that needs fixing by the coder
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        } catch (Exception e) {
            log.error("Error while checking edited state ", e);
            return RestResponse.<Void>error(e.getMessage()).toResponse();
        }
    }
}
