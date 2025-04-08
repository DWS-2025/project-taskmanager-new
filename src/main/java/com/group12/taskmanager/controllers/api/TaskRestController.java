package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Task;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskRestController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectService projectService;

    @GetMapping("/tasks")
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<?> getTasks(@PathVariable int projectId) {
        Project project = projectService.findProjectById(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        List<Task> tasks = taskService.getProjectTasks(project);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping(value = "/projects/{projectId}/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createTask(
            @PathVariable int projectId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile image) {

        Project project = projectService.findProjectById(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        byte[] imageBytes = null;
        try {
            if (image != null && !image.isEmpty()) {
                if (image.getSize() > 5 * 1024 * 1024)
                    return ResponseEntity.status(413).body("Image too large (max 5MB)");
                imageBytes = image.getBytes();
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error reading image");
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setImage(imageBytes);
        task.setProject(project);

        taskService.addTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PutMapping(value = "/projects/{projectId}/tasks/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateTask(
            @PathVariable int projectId,
            @PathVariable int taskId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile image) {

        Task task = taskService.findTaskById(taskId);
        if (task == null || task.getProject().getId() != projectId)
            return ResponseEntity.status(404).body("Task not found");

        try {
            if (image != null && !image.isEmpty()) {
                if (image.getSize() > 5 * 1024 * 1024)
                    return ResponseEntity.status(413).body("Image too large");
                task.setImage(image.getBytes());
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Image processing failed");
        }

        task.setTitle(title);
        task.setDescription(description);
        taskService.updateTask(taskId, title, description, task.getImage());

        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable int projectId, @PathVariable int taskId) {
        Task task = taskService.findTaskById(taskId);
        if (task == null || task.getProject().getId() != projectId)
            return ResponseEntity.status(404).body("Task not found");

        taskService.removeTask(taskId);
        return ResponseEntity.ok("Task deleted successfully");
    }

    @GetMapping("/tasks/search")
    public ResponseEntity<List<Task>> searchTasks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean hasImage) {

        List<Task> tasks = taskService.getAllTasks();

        // Filtro por título (contiene texto)
        if (title != null && !title.isBlank()) {
            String lower = title.toLowerCase();
            tasks = tasks.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(lower))
                    .toList();
        }

        // Filtro por si tiene imagen o no
        if (hasImage != null) {
            tasks = tasks.stream()
                    .filter(t -> hasImage.equals(t.getImage() != null))
                    .toList();
        }

        return ResponseEntity.ok(tasks);
    }

}
