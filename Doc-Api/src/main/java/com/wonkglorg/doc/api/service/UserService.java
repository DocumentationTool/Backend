package com.wonkglorg.doc.api.service;

import static com.wonkglorg.doc.core.FileRepository.getUserDB;
import com.wonkglorg.doc.core.exception.CoreException;
import com.wonkglorg.doc.core.exception.CoreSqlException;
import com.wonkglorg.doc.core.exception.client.ClientException;
import com.wonkglorg.doc.core.exception.client.InvalidGroupException;
import com.wonkglorg.doc.core.exception.client.InvalidRepoException;
import com.wonkglorg.doc.core.exception.client.InvalidUserException;
import com.wonkglorg.doc.core.interfaces.GroupCalls;
import com.wonkglorg.doc.core.interfaces.UserCalls;
import com.wonkglorg.doc.core.objects.GroupId;
import com.wonkglorg.doc.core.objects.UserId;
import com.wonkglorg.doc.core.permissions.Role;
import com.wonkglorg.doc.core.user.Group;
import com.wonkglorg.doc.core.user.UserProfile;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * The user service handling all user / group related operations, this is the main entrypoint of the user system, inputs are verified before passing it onto {@link com.wonkglorg.doc.core.db.UserDatabase}
 */
@Component
@Service
public class UserService implements UserCalls, GroupCalls{
	
	private final RepoService repoService;
	
	public UserService(@Lazy RepoService repoService) {
		this.repoService = repoService;
	}
	
	//---- User ----
	
	@Override
	public boolean addUser(UserProfile user) throws ClientException, CoreSqlException {
		if(userExists(user.getId())){
			throw new ClientException("User with id '%s' already exists".formatted(user.getId()));
		}
		
		for(GroupId groupId : user.getGroups()){
			if(!groupExists(groupId)){
				throw new ClientException("Group with id '%s' does not exist".formatted(groupId));
			}
		}
		return getUserDB().addUser(user);
		
	}
	
	@Override
	public boolean removeUser(UserId userId) throws InvalidUserException, CoreSqlException {
		validateUser(userId);
		
		//removes all related permissions from any repo referencing this user
		for(var repo : repoService.getRepositories().values()){
			repo.getDatabase().permissionFunctions().cleanUpUser(userId);
		}
		
		return getUserDB().removeUser(userId);
	}
	
	@Override
	public List<UserProfile> getUsers() {
		return getUserDB().getUsers();
	}
	
	@Override
	public UserProfile getUser(UserId userId) throws InvalidUserException {
		return getUserDB().getUser(userId);
	}
	
	@Override
	public boolean userExists(UserId userId) {
		try{
			UserProfile users = getUserDB().getUser(userId);
			return users != null;
		} catch(InvalidUserException e){
			return false;
		}
	}
	
	/**
	 * Validates if a user exists
	 *
	 * @param userId the user id
	 */
	public void validateUser(UserId userId) throws InvalidUserException {
		if(!userExists(userId)){
			throw new InvalidUserException("User '%s' does not exist".formatted(userId));
		}
	}
	
	public void validateGroup(GroupId groupId) throws InvalidGroupException {
		if(!groupExists(groupId)){
			throw new InvalidGroupException("Group '%s' does not exist".formatted(groupId));
		}
	}
	
	@Override
	public boolean groupExists(GroupId groupId) {
		return getUserDB().groupExists(groupId);
	}
	
	@Override
	public boolean userInGroup(GroupId groupId, UserId userId) throws InvalidUserException, InvalidGroupException {
		validateUser(userId);
		validateGroup(groupId);
		return getUserDB().userInGroup(groupId, userId);
	}
	
	@Override
	public boolean addGroup(Group group) throws CoreException, InvalidGroupException {
		if(groupExists(group.getId())){
			throw new InvalidGroupException("Group with id '%s' already exists!".formatted(group.getId()));
		}
		return getUserDB().addGroup(group);
	}
	
	@Override
	public boolean removeGroup(GroupId groupId) throws InvalidGroupException {
		validateGroup(groupId);
		
		for(var repo : repoService.getRepositories().values()){
			repo.getDatabase().permissionFunctions().cleanUpGroup(groupId);
		}
		
		return getUserDB().removeGroup(groupId);
	}
	
	@Override
	public List<Group> getGroups() {
		return getUserDB().getGroups();
	}
	
	@Override
	public Group getGroup(GroupId groupId) throws InvalidGroupException {
		validateGroup(groupId);
		return getUserDB().getGroup(groupId);
	}
	
	@Override
	public Group renameGroup(GroupId groupId, String newName) throws CoreException, InvalidGroupException {
		validateGroup(groupId);
		return getUserDB().renameGroup(groupId, newName);
	}
	
	@Override
	public boolean addUserToGroup(GroupId groupId, UserId userId) throws CoreException, ClientException {
		
		if(!groupExists(groupId)){
			throw new InvalidGroupException("Group with id '%s' does not exist".formatted(groupId));
		}
		validateUser(userId);
		
		if(userInGroup(groupId, userId)){
			throw new ClientException("User with id '%s' is already in group '%s'".formatted(userId, groupId));
		}
		
		return getUserDB().addUserToGroup(groupId, userId);
	}
	
	@Override
	public boolean removeUserFromGroup(GroupId groupId, UserId userId) throws CoreException, InvalidGroupException, InvalidUserException {
		validateGroup(groupId);
		
		validateUser(userId);
		
		if(!userInGroup(groupId, userId)){
			throw new InvalidUserException("User with id '%s' is not in group '%s'".formatted(userId, groupId));
		}
		
		return getUserDB().removeUserFromGroup(groupId, userId);
	}
	
	@Override
	public Set<UserProfile> getUsersFromGroup(GroupId groupId) throws InvalidGroupException {
		validateGroup(groupId);
		return getUserDB().getUsersFromGroup(groupId);
	}
	
	@Override
	public Set<Group> getGroupsFromUser(UserId userId) throws InvalidUserException {
		validateUser(userId);
		return getUserDB().getGroupsFromUser(userId);
	}
	
	@Override
	public void addRole(UserId userId, Role role) throws InvalidRepoException, InvalidUserException {
		validateUser(userId);
		getUserDB().addRole(userId, role);
	}
	
	@Override
	public void removeRole(UserId userId, Role role) throws InvalidUserException, InvalidRepoException {
		validateUser(userId);
		getUserDB().removeRole(userId, role);
	}
}
