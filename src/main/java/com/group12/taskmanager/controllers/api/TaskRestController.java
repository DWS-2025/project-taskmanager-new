package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskRequestDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.task.TaskImageDTO;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.TaskService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskRestController {

    private final TaskService taskService;
    private final ProjectService projectService;

    public TaskRestController(TaskService taskService, ProjectService projectService) {
        this.taskService = taskService;
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponseDTO>> getTasksByProject(@PathVariable int projectId) {
        Project project = projectService.findProjectByIdRaw(projectId);
        ProjectResponseDTO dto = new ProjectResponseDTO(project.getId(), project.getName(), project.getGroup().getId());
        return ResponseEntity.ok(taskService.getProjectTasks(dto));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TaskResponseDTO>> searchTasks(@RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean hasImage, @RequestParam int projectID) {
        TaskResponseDTO dto = new TaskResponseDTO(title, hasImage, projectID);
        return ResponseEntity.ok(taskService.searchTasks(dto));
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(@RequestBody TaskRequestDTO dto) {
        TaskResponseDTO created = taskService.addTask(dto);
        if (created == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> updateTask(@PathVariable int id, @RequestBody TaskRequestDTO dto) {
        TaskResponseDTO updated = taskService.updateTask(id, dto);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable int id) {
        TaskResponseDTO task = taskService.findTaskById(id);
        boolean removed = taskService.removeTask(task);
        return removed ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Void> uploadImage(@PathVariable int id, @RequestBody TaskImageDTO dto) {
        boolean success = taskService.uploadImage(id, dto);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<TaskImageDTO> getImage(@PathVariable int id) {
        TaskImageDTO image = taskService.getImage(id);
        return (image != null) ? ResponseEntity.ok(image) : ResponseEntity.notFound().build();
    }
}
