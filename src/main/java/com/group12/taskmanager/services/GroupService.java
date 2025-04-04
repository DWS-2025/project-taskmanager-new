package com.group12.taskmanager.services;

import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    // Retrieve all groups ordered by ID descending
    public List<Group> getAllGroups() {
        return groupRepository.findAllByOrderByIdDesc();
    }

    // Save or update a group in the database
    public void saveGroup(Group group) {
        groupRepository.save(group);
    }

    // Find a group by its ID, including its users
    public Group findGroupById(int groupId) {
        return groupRepository.findByIdWithUsers(groupId);
    }

    // Create a new group and associate it with the owner
    public Group createGroup(String name, User owner) {
        Group newGroup = new Group(name, owner);
        newGroup.getUsers().add(owner); // Add owner to the group
        owner.getGroups().add(newGroup); // Add group to owner's list
        saveGroup(newGroup);
        return groupRepository.save(newGroup);
    }

    // Update the name of an existing group
    public boolean updateGroupName(int groupId, String newName) {
        Optional<Group> optionalGroup = groupRepository.findById(groupId);
        if (optionalGroup.isPresent()) {
            Group group = optionalGroup.get();
            group.setName(newName);
            groupRepository.save(group);
            return true;
        }
        return false;
    }

    // Delete a group if the user is the owner or admin (ID 1)
    @Transactional
    public boolean deleteGroup(int groupId, User currentUser) {
        Group group = findGroupById(groupId);

        if (group != null) {
            if (group.getOwner().getId().equals(currentUser.getId()) || currentUser.getId().equals(1)) {
                // Delete all projects associated with the group
                for (Project project : projectService.getProjectsByGroup(group)) {
                    projectService.deleteProject(project.getId());
                }
                // If admin is deleting a personal group, also delete the user
                if (currentUser.getId().equals(1) && group.getName().startsWith("USER_")) {
                    userService.deleteUser(group.getOwner().getId(), currentUser);
                } else {
                    groupRepository.delete(group);
                }
                return true;
            }
        }

        return false;
    }

    // Remove a user from a group (both in memory and database)
    @Transactional
    public void removeUserFromGroup(Group group, User user) {
        group.getUsers().remove(user); // Remove from memory
        groupRepository.deleteUserFromGroup(group.getId(), user.getId()); // Remove from DB
    }
}
