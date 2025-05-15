package com.group12.taskmanager.security;

import com.group12.taskmanager.config.GlobalConstants;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.ProjectService;
import org.springframework.stereotype.Component;

@Component
public class AccessManager {

    private final GlobalConstants globalConstants;
    private final ProjectService projectService;
    private final GroupService groupService;

    public AccessManager(ProjectService projectService, GroupService groupService, GlobalConstants globalConstants) {
        this.projectService = projectService;
        this.groupService = groupService;
        this.globalConstants = globalConstants;
    }

    public boolean checkUserAccess(UserResponseDTO accessedUser, UserResponseDTO currentUser) {
        return accessedUser.getId() == currentUser.getId() || currentUser.getId() == globalConstants.getAdminID();
    }
    public boolean checkAdminCredentials(UserResponseDTO currentUser) {
        return currentUser.getId() == globalConstants.getAdminID();
    }

    public boolean checkGroupAccess(GroupResponseDTO group, UserResponseDTO user) {
        boolean isOwner = group.getOwnerId() == user.getId();
        boolean isMember = groupService.getGroupUsers(group).stream()
                .anyMatch(u -> u.getId() == user.getId());
        boolean isAdmin = user.getId() == globalConstants.getAdminID();

        return isOwner || isMember || isAdmin;
    }
    public boolean checkGroupOwnership(GroupResponseDTO group, UserResponseDTO user) {
        return group.getOwnerId() == user.getId();
    }

    public boolean checkProjectAccess(ProjectResponseDTO project, UserResponseDTO user) {
        GroupResponseDTO group = groupService.findGroupById(project.getGroupId());
        return checkGroupAccess(group, user);
    }
    public boolean checkProjectOwnership(ProjectResponseDTO project, UserResponseDTO user) {
        GroupResponseDTO group = groupService.findGroupById(project.getGroupId());
        return checkGroupOwnership(group, user);
    }

    public boolean checkTaskAccess(TaskResponseDTO task, UserResponseDTO user) {
        ProjectResponseDTO project = projectService.findProjectById(task.getProjectId());
        return checkProjectAccess(project, user);
    }

}
