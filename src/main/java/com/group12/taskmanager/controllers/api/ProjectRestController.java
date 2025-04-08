package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.GroupService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectRestController {

    @Autowired private ProjectService projectService;
    @Autowired private GroupService groupService;

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable int id) {
        Project project = projectService.findProjectById(id);
        return (project != null) ? ResponseEntity.ok(project) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestParam String name, @RequestParam int groupId) {
        try {
            Project project = projectService.createProject(name, groupId);
            return ResponseEntity.status(HttpStatus.CREATED).body(project);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable int id, @RequestParam String name) {
        Project project = projectService.findProjectById(id);
        if (project == null) return ResponseEntity.notFound().build();
        project.setName(name);
        projectService.updateProject(project);
        return ResponseEntity.ok(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable int id) {
        Project project = projectService.findProjectById(id);
        if (project == null) return ResponseEntity.notFound().build();
        projectService.deleteProject(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Deleted successfully"));
    }
}
