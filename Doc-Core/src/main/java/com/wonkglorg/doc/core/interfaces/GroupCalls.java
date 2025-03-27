package com.wonkglorg.doc.core.interfaces;

import com.wonkglorg.doc.core.exception.CoreException;
import com.wonkglorg.doc.core.exception.client.ClientException;
import com.wonkglorg.doc.core.exception.client.InvalidGroupException;
import com.wonkglorg.doc.core.exception.client.InvalidRepoException;
import com.wonkglorg.doc.core.exception.client.InvalidUserException;
import com.wonkglorg.doc.core.objects.GroupId;
import com.wonkglorg.doc.core.objects.UserId;
import com.wonkglorg.doc.core.user.Group;
import com.wonkglorg.doc.core.user.UserProfile;

import java.util.List;
import java.util.Set;

/**
 * A common interface referencing all group related calls
 */
public interface GroupCalls {

    /**
     * Checks if a group exists
     *
     * @param groupId The group id.
     * @return True if the group exists, false otherwise.
     */
    boolean groupExists(GroupId groupId);

    /**
     * Checks if a group exists
     *
     * @param groupId the group to check for
     * @param userId  the user to check for
     * @return true if the group exists
     */
    boolean userInGroup(GroupId groupId, UserId userId) throws InvalidUserException, InvalidGroupException;

    /**
     * Adds a new group
     *
     * @param group the group to add
     * @return true if the group was added
     * @throws CoreException         if the group could not be added
     * @throws InvalidGroupException if the group is invalid
     */
    boolean addGroup(Group group) throws CoreException, InvalidGroupException;

    /**
     * Removes a group
     *
     * @param groupId the group to remove
     * @return true if the group was removed
     * @throws CoreException         if the group could not be removed
     * @throws InvalidGroupException if the group is invalid
     */
    boolean removeGroup(GroupId groupId) throws CoreException, InvalidGroupException;

    /**
     * Gets all groups
     *
     * @return a list of groups
     */
    List<Group> getGroups();

    /**
     * Gets a group
     *
     * @param groupId the group to get
     * @return the group
     */
    Group getGroup(GroupId groupId) throws InvalidGroupException;

    /**
     * Updates a group
     *
     * @param groupId the group to update
     * @param newName the updated group
     * @return the updated group
     * @throws CoreException         if the group could not be updated
     * @throws InvalidGroupException if the group is invalid
     */
    Group renameGroup(GroupId groupId, String newName) throws CoreException, InvalidGroupException;

    /**
     * Adds a user to a group
     *
     * @param groupId the group to add the user to
     * @param userId  the user to add to the group
     * @return true if the user was added to the group
     * @throws CoreException         if the user could not be added to the group
     * @throws InvalidRepoException  if the repo is invalid
     * @throws InvalidGroupException if the group is invalid
     */
    boolean addUserToGroup(GroupId groupId, UserId userId) throws CoreException, ClientException;

    /**
     * Removes a user from a group
     *
     * @param groupId the group to remove the user from
     * @param userId  the user to remove from the group
     * @return true if the user was removed from the group
     * @throws CoreException         if the user could not be removed from the group
     * @throws InvalidGroupException if the group is invalid
     */
    boolean removeUserFromGroup(GroupId groupId, UserId userId) throws CoreException, InvalidGroupException, InvalidUserException;

    /**
     * Gets all groups a user is in
     * @param userId the user to get the groups from
     * @return a set of groups
     * @throws InvalidUserException if the user is invalid
     */
    Set<Group> getGroupsFromUser(UserId userId) throws InvalidUserException;

    /**
     * Gets all users in a group
     * @param groupId the group to get the users from
     * @return a set of users
     * @throws InvalidGroupException if the group is invalid
     */
    Set<UserProfile> getUsersFromGroup(GroupId groupId) throws InvalidGroupException;
}
