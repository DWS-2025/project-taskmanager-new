package com.group12.taskmanager.services;

import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskRequestDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.task.TaskImageDTO;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Task;
import com.group12.taskmanager.repositories.ProjectRepository;
import com.group12.taskmanager.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;

    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskResponseDTO> getProjectTasks(ProjectResponseDTO dto) {
        Project project = projectRepository.findById(dto.getId()).get();
        return taskRepository.findByProject(project).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO addTask(TaskRequestDTO dto) {
        Project project = projectRepository.findById(dto.getProjectId()).orElse(null);
        if (project == null) return null;

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setProject(project);

        Task saved = taskRepository.save(task);
        return toDTO(saved);
    }

    public TaskResponseDTO findTaskById(int id) {
        return toDTO(taskRepository.findById(id).orElse(null));
    }

    public boolean removeTask(TaskResponseDTO dto) {
        int id = dto.getId();
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public TaskResponseDTO updateTask(int id, TaskRequestDTO dto) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return null;

        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());

        return toDTO(taskRepository.save(task));
    }

    public boolean uploadImage(int id, TaskImageDTO dto) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null || dto.getBase64() == null) return false;

        byte[] imageBytes = Base64.getDecoder().decode(dto.getBase64());
        task.setImage(imageBytes);
        taskRepository.save(task);
        return true;
    }

    public TaskImageDTO getImage(int id) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null || task.getImage() == null) return null;

        String encoded = Base64.getEncoder().encodeToString(task.getImage());
        return new TaskImageDTO(encoded);
    }

    public List<TaskResponseDTO> searchTasks(String title, Boolean hasImage) {
        return taskRepository.findAll().stream()
                .filter(t -> title == null || t.getTitle().toLowerCase().contains(title.toLowerCase()))
                .filter(t -> hasImage == null || hasImage.equals(t.getImage() != null))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private TaskResponseDTO toDTO(Task task) {
        return new TaskResponseDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getImage() != null,
                task.getProject().getId()
        );
    }

    public List<Task> getProjectTasksRaw(Project project) {
        return taskRepository.findByProject(project);
    }

}
