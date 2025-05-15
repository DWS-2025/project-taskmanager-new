package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.security.AccessManager;
import com.group12.taskmanager.security.CustomUserDetails;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.TaskService;
import com.group12.taskmanager.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    private final UserService userService;
    private final GroupService groupService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final AccessManager accessManager;

    public AdminRestController(UserService userService, GroupService groupService, TaskService taskService,
                               ProjectService projectService, AccessManager accessManager) {
        this.userService = userService;
        this.groupService = groupService;
        this.taskService = taskService;
        this.projectService = projectService;
        this.accessManager = accessManager;
    }

    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        if(accessManager.checkAdminCredentials(currentUser))
            return userService.getAllUsers();

        return null;
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupResponseDTO>> getAllGroups(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        if(accessManager.checkAdminCredentials(currentUser))
            return ResponseEntity.ok(groupService.getAllGroups());

        return null;
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        if(accessManager.checkAdminCredentials(currentUser))
            return ResponseEntity.ok(projectService.getAllProjects());

        return null;
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getAllTasks(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        if(accessManager.checkAdminCredentials(currentUser))
            return ResponseEntity.ok(taskService.getAllTasks());

        return null;
    }
}
