package com.group12.taskmanager.controllers;

import com.group12.taskmanager.dto.*;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Controller
public class ProjectController {

    @Autowired private ProjectService projectService;
    @Autowired private TaskService taskService;
    @Autowired private GroupService groupService;

    @GetMapping("/projects")
    public String getProjects(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/"; // Redirect to home if user not logged in

        model.addAttribute("user", currentUser);
        List<Project> projects = new ArrayList<>();
        List<Group> ownedGroups = new ArrayList<>();

        if (currentUser.getId().equals(1)) {
            // Admin user can see all projects and all groups
            projects = projectService.getAllProjectsRaw();
            ownedGroups = groupService.getAllGroups();
        } else {
            // Regular user: get projects from groups they belong to
            for (Group group : currentUser.getGroups()) {
                projects.addAll(projectService.getGroupProjectsRaw(group));
            }
            // Groups where the user is the owner
            ownedGroups = currentUser.getGroups().stream()
                    .filter(g -> g.getOwner().getId().equals(currentUser.getId()))
                    .toList();
        }

        model.addAttribute("ownedGroups", ownedGroups);
        model.addAttribute("multipleGroups", ownedGroups.size() > 1);
        model.addAttribute("singleGroup", ownedGroups.size() == 1 ? ownedGroups.get(0) : null);
        model.addAttribute("projects", projects);

        return "index"; // Return view for project listing
    }

    @PostMapping("/save_project")
    public String saveProject(@RequestParam String name, @RequestParam int groupId) {
        Group group = groupService.findGroupById(groupId);
        if (group == null) return "redirect:/"; // Redirect if group not found

        ProjectRequestDTO dto = new ProjectRequestDTO(name, groupId); // Create project with given name and group
        projectService.createProject(dto);
        return "redirect:/projects";
    }

    @GetMapping("/new_project")
    public ResponseEntity<?> newProject() {
        // Return message for opening modal (used in frontend)
        return ResponseEntity.ok(Collections.singletonMap("mensaje", "Abriendo modal"));
    }

    @GetMapping("/project/{id}")
    public String getProjectById(@PathVariable int id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) return "redirect:/";
        Project project = projectService.findProjectById(id);
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

    @PostMapping("/project/{id}/delete_project")
    public ResponseEntity<?> deleteProject(@PathVariable int id) {
        Project project = projectService.findProjectById(id);
        if (project == null)
            return ResponseEntity.status(404).body(Collections.singletonMap("error", "Project not found"));

        projectService.deleteProject(id); // Delete project
        boolean removed = projectService.findProjectById(id) == null;
        return removed
                ? ResponseEntity.ok(Collections.singletonMap("message", "Project deleted successfully"))
                : ResponseEntity.status(500).body(Collections.singletonMap("error", "Error deleting project"));
    }

    @PutMapping("/project/{id}/edit_project")
    public ResponseEntity<?> editProject(@PathVariable int id, @RequestParam String name) {
        Project project = projectService.findProjectById(id);
        if (project == null)
            return ResponseEntity.status(404).body(Collections.singletonMap("error", "Project not found"));

        project.setName(name); // Update project name
        ProjectRequestDTO dto = new ProjectRequestDTO(project.getName(), project.getGroup().getId());
        projectService.updateProject(project.getId(), dto); // save changes
        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }
}
