package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskRequestDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.task.TaskImageDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.security.AccessManager;
import com.group12.taskmanager.security.CustomUserDetails;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.TaskService;
import com.group12.taskmanager.services.UserService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskRestController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;
    private final AccessManager accessManager;

    public TaskRestController(TaskService taskService, ProjectService projectService, AccessManager accessManager, UserService userService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.accessManager = accessManager;
        this.userService = userService;
    }

    private boolean verifyTaskAccess(TaskResponseDTO task, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkTaskAccess(task, currentUser);
    }
    private boolean verifyProjectAccess(ProjectResponseDTO project, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkProjectAccess(project, currentUser);
    }
    private boolean verifyTaskOwnership(TaskResponseDTO task, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkTaskOwnership(task, currentUser);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TaskResponseDTO>> searchTasks(@RequestParam(required = false) String title,
                                                             @RequestParam(required = false) Boolean hasImage, @RequestParam int projectID,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponseDTO project = projectService.findProjectById(projectID);

        if (!verifyProjectAccess(project, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        TaskResponseDTO dto = new TaskResponseDTO(title, hasImage, projectID, 0);
        return ResponseEntity.ok(taskService.searchTasks(dto));
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(@RequestBody TaskRequestDTO dto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponseDTO project = projectService.findProjectById(dto.getProjectId());

        if (!verifyProjectAccess(project, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        TaskResponseDTO created = taskService.addTask(dto, currentUser);
        if (created == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> updateTask(@PathVariable int id, @RequestBody TaskRequestDTO dto,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskResponseDTO task = taskService.findTaskById(id);

        if (!verifyTaskOwnership(task, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        TaskResponseDTO updated = taskService.updateTask(id, dto);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskResponseDTO task = taskService.findTaskById(id);

        if (!verifyTaskOwnership(task, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean removed = taskService.removeTask(task);
        return removed ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Void> uploadImage(@PathVariable int id, @RequestBody TaskImageDTO dto,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskResponseDTO task = taskService.findTaskById(id);

        if (!verifyTaskOwnership(task, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean success = taskService.uploadImage(id, dto);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<TaskImageDTO> getImage(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskResponseDTO task = taskService.findTaskById(id);

        if (!verifyTaskAccess(task, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        TaskImageDTO image = taskService.getImage(id);
        return (image != null) ? ResponseEntity.ok(image) : ResponseEntity.notFound().build();
    }
}
