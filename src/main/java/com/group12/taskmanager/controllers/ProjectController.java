package com.group12.taskmanager.controllers;

import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.project.ProjectRequestDTO;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Task;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.TaskService;
import com.group12.taskmanager.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class ProjectController {

    @Autowired private ProjectService projectService;
    @Autowired private TaskService taskService;
    @Autowired private GroupService groupService;
    @Autowired private UserService userService;

    @GetMapping("/projects")
    public String getProjects(Model model, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser == null) return "redirect:/"; // Redirect to home if user not logged in

        model.addAttribute("user", currentUser);

        List<GroupResponseDTO> ownedGroups = new ArrayList<>();

        if (currentUser.getId() == 1) {
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
        model.addAttribute("singleGroup", ownedGroups.size() == 1 ? ownedGroups.get(0) : null);

        return "index"; // projects loaded by JS from /api/projects/p/:id
    }

    @GetMapping("/project/{id}")
    public String getProjectById(@PathVariable int id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) return "redirect:/";
        Project project = projectService.findProjectByIdRaw(id);
        List<Task> tasks = taskService.getProjectTasksRaw(project);

        for (Task task : tasks) {
            if (task.getImage() != null) {
                String base64 = Base64.getEncoder().encodeToString(task.getImage());
                task.setImageBase64(base64);
            }
        }

        if (project != null) {
            model.addAttribute("idproject", project.getId());
            model.addAttribute("tasks", tasks);
        } else {
            model.addAttribute("idproject", null);
            model.addAttribute("tasks", new ArrayList<>());
        }
        return "project";
    }
}
