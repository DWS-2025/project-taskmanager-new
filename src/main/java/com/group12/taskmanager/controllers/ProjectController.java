package com.group12.taskmanager.controllers;

import com.group12.taskmanager.config.GlobalConstants;
import com.group12.taskmanager.config.exceptions.ForbiddenAccessException;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.security.AccessManager;
import com.group12.taskmanager.security.AuthenticatedUserProvider;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Task;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.TaskService;
import com.group12.taskmanager.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final GroupService groupService;
    private final UserService userService;
    private final GlobalConstants globalConstants;
    private final AuthenticatedUserProvider auth;
    private final AccessManager accessManager;

    public ProjectController(ProjectService projectService, TaskService taskService, GroupService groupService,
                             UserService userService, GlobalConstants globalConstants, AuthenticatedUserProvider auth, AccessManager accessManager) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.groupService = groupService;
        this.userService = userService;
        this.globalConstants = globalConstants;
        this.auth = auth;
        this.accessManager = accessManager;
    }

    @GetMapping("/projects")
    public String getProjects(Model model) {
        UserResponseDTO currentUser = auth.getCurrentUser();
        if (currentUser == null) return "redirect:/";

        model.addAttribute("user", currentUser);
        if (currentUser.getRole().equals(globalConstants.getAdminRole()))
            model.addAttribute("isAdmin", true);
        else
            model.addAttribute("isAdmin", false);

        List<GroupResponseDTO> ownedGroups;

        if (currentUser.getRole().equals(globalConstants.getAdminRole())) {
            // Admin user can see all projects and all groups
            ownedGroups = groupService.getAllGroups();
        } else {
            // Regular user: get projects from groups they belong to
            List<GroupResponseDTO> currentUserGroups = userService.getUserGroups(currentUser);
            // Groups where the user is the owner
            ownedGroups = currentUserGroups.stream()
                    .filter(g -> g.getOwnerId() == currentUser.getId())
                    .toList();
        }

        model.addAttribute("ownedGroups", ownedGroups);
        model.addAttribute("multipleGroups", ownedGroups.size() > 1);
        model.addAttribute("singleGroup", ownedGroups.size() == 1 ? ownedGroups.getFirst() : null);

        return "index"; // projects loaded by JS from /api/projects/p/:id
    }

    @GetMapping("/project/{id}")
    public String getProjectById(@PathVariable int id, Model model) {
        UserResponseDTO user = auth.getCurrentUser();
        Project project = projectService.findProjectByIdRaw(id);
        ProjectResponseDTO projectDTO = projectService.findProjectById(id);

        if (!accessManager.checkProjectAccess(projectDTO, user))
            throw new ForbiddenAccessException("No tienes permiso para acceder a este proyecto.");

        List<Task> tasks = taskService.getProjectTasksRaw(project);
        for (Task task : tasks) {
            if (task.getImage() != null) {
                String base64 = Base64.getEncoder().encodeToString(task.getImage());
                task.setImageBase64(base64);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("idproject", project.getId());
        model.addAttribute("tasks", tasks);
        return "project";
    }

    @GetMapping("/admin")
    public String goToAdminPanel() {
        UserResponseDTO currentUser = auth.getCurrentUser();
        if (accessManager.checkAdminCredentials(currentUser))
            return "admin";
        return "redirect:/";
    }
}
