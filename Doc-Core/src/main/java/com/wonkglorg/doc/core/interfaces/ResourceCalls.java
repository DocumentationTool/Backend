package com.wonkglorg.doc.core.interfaces;

import com.wonkglorg.doc.core.exception.CoreException;
import com.wonkglorg.doc.core.exception.CoreSqlException;
import com.wonkglorg.doc.core.exception.client.*;
import com.wonkglorg.doc.core.objects.*;
import com.wonkglorg.doc.core.path.TargetPath;
import com.wonkglorg.doc.core.request.ResourceRequest;
import com.wonkglorg.doc.core.request.ResourceUpdateRequest;

import java.nio.file.Path;
import java.util.List;

public interface ResourceCalls {
    //------------------- Resources -------------------

    /**
     * Get a list of resources based on the request
     *
     * @param request The request object
     * @return A list of resources
     */
    List<Resource> getResources(ResourceRequest request) throws CoreException, ClientException;

    /**
     * Get a resource based on the repoId and path
     *
     * @param resource The resource object
     */
    void insertResource(Resource resource) throws ClientException, CoreException;

    /**
     * Removes a resource based on the repoId and path this operation will fail if the repository is read only or locked by a user currently editing it
     *
     * @param repoId The repoId object
     * @param path   The path object
     * @return The resource object
     */
    boolean removeResource(RepoId repoId, Path path) throws CoreException, ClientException;

    /**
     * Updates a resource based on the request
     *
     * @param request The request object
     * @return The resource object
     */
    Resource updateResource(ResourceUpdateRequest request) throws ClientException, CoreSqlException;

    /**
     * Checks if a resource exists in the database
     *
     * @param repoId the repoId of the resource
     * @param path   the path to the resource
     * @return true if the resource exists false otherwise
     */
    boolean resourceExists(RepoId repoId, Path path) throws InvalidRepoException;

    /**
     * Move a resource from one path to another
     *
     * @param repoId  The repoId object
     * @param oldPath The old path object
     * @param newPath The new path object
     * @return true if the resource was moved false otherwise
     * @throws InvalidRepoException if the repoId is invalid
     */
    boolean moveResource(RepoId repoId, Path oldPath, Path newPath)
            throws InvalidRepoException, CoreSqlException, InvalidResourceException, ReadOnlyRepoException;

    //------------------- Tags -------------------

    /**
     * Get a list of tags based on the repoId
     *
     * @param repoId The repoId object
     * @return A list of tags
     */
    List<Tag> getTags(RepoId repoId) throws InvalidRepoException;

    /**
     * Create a tag based on the repoId and tag
     *
     * @param repoId The repoId object
     * @param tag    The tag object
     */
    void createTag(RepoId repoId, Tag tag) throws ClientException, CoreSqlException;

    /**
     * Remove a tag based on the repoId and tagId
     *
     * @param repoId The repoId object
     * @param tagId  The tagId object
     */
    void removeTag(RepoId repoId, TagId tagId) throws CoreSqlException, InvalidRepoException, InvalidTagException;

    /**
     * Check if a tag exists based on the repoId and tagId
     *
     * @param repoId The repoId object
     * @param tagId  The tagId object
     * @return A boolean
     */
    boolean tagExists(RepoId repoId, TagId tagId);

    //------------------- Editing -------------------

    /**
     * Get the user editing a resource
     *
     * @param repoId The repoId object
     * @param path   The path object
     * @return The userId object or null
     */
    UserId getEditingUser(RepoId repoId, Path path) throws InvalidResourceException, InvalidRepoException;

    /**
     * Check if a user is editing a resource
     *
     * @param id     The repoId object
     * @param userId The userId object
     * @return A boolean
     */
    boolean isUserEditing(RepoId id, UserId userId) throws InvalidRepoException, InvalidUserException;

    /**
     * Sets a user as editing a file locking it for others to edit at the same time
     *
     * @param userId the user editing
     * @param path   the path to the file
     */
    void setCurrentlyEdited(RepoId repoId, UserId userId, Path path) throws ClientException;

    /**
     * Removes any lock on a file placed by this user
     *
     * @param repoId The repoId object
     * @param userId The userId object
     */
    void removeCurrentlyEdited(RepoId repoId, UserId userId) throws InvalidRepoException, InvalidUserException;

    /**
     * Removes the editing lock on a file
     *
     * @param id   The repoId object
     * @param path The path object
     */
    void removeCurrentlyEdited(RepoId id, Path path) throws InvalidResourceException, InvalidRepoException;


    //-------------------- Tags --------------------

    /**
     * Add a tag to a target, this may be a resource or a path
     * @param repoId the repoId
     * @param path the path to the target
     * @param tagId the tagId to add
     * @throws CoreException
     * @throws ClientException
     */
    void addTag(RepoId repoId, TargetPath path, TagId tagId) throws CoreException, ClientException;


    /**
     * Remove a tag from a target, this may be a resource or a path
     * @param repoId  the repoId
     * @param path the path to the target
     * @param tagId the tagId to remove
     * @throws CoreException
     * @throws ClientException
     */
    void removeTag(RepoId repoId, TargetPath path, TagId tagId) throws CoreException, ClientException;

    /**
     * Check if a tag exists on a target, this may be a resource or a path
     * @param repoId the repoId
     * @param path the path to the target
     * @param tagId the tagId to check
     * @return true if the tag exists false otherwise
     * @throws CoreException if the tag path does not exist
     */
    boolean tagPathExists(RepoId repoId, TargetPath path, TagId tagId) throws CoreException, ClientException;
}
