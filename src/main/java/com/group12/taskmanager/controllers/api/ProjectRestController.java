package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.ProjectRequestDTO;
import com.group12.taskmanager.dto.ProjectResponseDTO;
import com.group12.taskmanager.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/projects")
public class ProjectRestController {

    @Autowired private ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable int id) {
        ProjectResponseDTO project = projectService.findProjectDTOById(id);
        return (project != null) ? ResponseEntity.ok(project) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@RequestBody ProjectRequestDTO dto) {
        ProjectResponseDTO created = projectService.createProject(dto);
        return (created != null)
                ? ResponseEntity.status(HttpStatus.CREATED).body(created)
                : ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(@PathVariable int id, @RequestBody ProjectRequestDTO dto) {
        ProjectResponseDTO updated = projectService.updateProject(id, dto);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable int id) {
        boolean deleted = projectService.deleteProject(id);
        return deleted
                ? ResponseEntity.ok(Collections.singletonMap("message", "Deleted successfully"))
                : ResponseEntity.notFound().build();
    }
}
