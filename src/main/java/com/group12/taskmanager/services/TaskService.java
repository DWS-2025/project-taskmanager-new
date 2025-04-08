package com.group12.taskmanager.services;

import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Task;
import com.group12.taskmanager.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    };

    // Save a new task to the database
    public Task addTask(Task task) {
        return taskRepository.save(task);
    }

    // Find a task by its ID
    public Task findTaskById(int id) {
        return taskRepository.findById(id).orElse(null);
    }

    // Delete a task by ID if it exists
    public boolean removeTask(int id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Update task fields: title, description, and optional image
    public Task updateTask(int id, String title, String description, byte[] imageBytes) {
        Task task = findTaskById(id);
        if (task != null) {
            if (title != null) task.setTitle(title);
            if (description != null) task.setDescription(description);
            if (imageBytes != null) task.setImage(imageBytes); // Updates BLOB if provided
            return taskRepository.save(task);
        }
        return null;
    }

    // Get all tasks that belong to a specific project
    public List<Task> getProjectTasks(Project project) {
        return taskRepository.findByProject(project);
    }
}
