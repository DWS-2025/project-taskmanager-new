package com.group12.taskmanager.security;

import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.ProjectService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AccessManager {

    ProjectService projectService;
    GroupService groupService;

    public void checkGroupAccess(GroupResponseDTO group, UserResponseDTO user) {
        boolean isOwner = group.getOwnerId() == user.getId();
        boolean isMember = groupService.getGroupUsers(group).stream()
                .anyMatch(u -> u.getId() == user.getId());

        if (!isOwner && !isMember) {
            throw new AccessDeniedException("No tienes acceso a este grupo.");
        }
    }
    public void checkGroupOwnership(GroupResponseDTO group, UserResponseDTO user) {
        if (group.getOwnerId() != user.getId()) {
            throw new AccessDeniedException("Solo el propietario puede realizar esta acción.");
        }
    }

    public void checkProjectAccess(ProjectResponseDTO project, UserResponseDTO user) {
        GroupResponseDTO group = groupService.findGroupById(project.getGroupId());
        checkGroupAccess(group, user);
    }
    public void checkProjectOwnership(ProjectResponseDTO project, UserResponseDTO user) {
        GroupResponseDTO group = groupService.findGroupById(project.getGroupId());
        checkGroupOwnership(group, user);
    }

    public void checkTaskAccess(TaskResponseDTO task, UserResponseDTO user) {
        ProjectResponseDTO project = projectService.findProjectById(task.getProjectId());
        checkProjectAccess(project, user);
    }

}
