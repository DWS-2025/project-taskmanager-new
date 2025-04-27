package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.project.ProjectRequestDTO;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/projects")
public class ProjectRestController {

    private final ProjectService projectService;
    private final GroupService groupService;
    private final UserService userService;

    public ProjectRestController(ProjectService projectService, GroupService groupService, UserService userService) {
        this.projectService = projectService;
        this.groupService = groupService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable int id) {
        ProjectResponseDTO project = projectService.findProjectById(id);
        return (project != null) ? ResponseEntity.ok(project) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@RequestBody ProjectRequestDTO dto) {
        GroupResponseDTO group = groupService.findGroupById(dto.getGroupId());
        if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        ProjectResponseDTO created = projectService.createProject(dto);
        return (created != null)
                ? ResponseEntity.status(HttpStatus.CREATED).body(created)
                : ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(@PathVariable int id, @RequestBody ProjectRequestDTO dto) {
        ProjectResponseDTO project = projectService.findProjectById(id);
        if (project == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        ProjectResponseDTO updated = projectService.updateProject(id, dto);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable int id) {
        ProjectResponseDTO project = projectService.findProjectById(id);
        if (project == null) return ResponseEntity.status(404).body(Collections.singletonMap("error", "Project not found"));

        boolean deleted = projectService.deleteProject(project);
        return deleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }

    // Pagination Controller --------------------------------------------------------------------------

    @GetMapping("/p/{userId}")
    public ResponseEntity<Page<ProjectResponseDTO>> getPaginatedProjects(@PathVariable int userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {

        UserResponseDTO user = userService.findUserById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Page<ProjectResponseDTO> projects = projectService.getProjectsPaginated(user, page, size);
        return ResponseEntity.ok(projects);
    }
}
