package com.group12.taskmanager.services;

import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskRequestDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.task.TaskImageDTO;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Task;
import com.group12.taskmanager.repositories.ProjectRepository;
import com.group12.taskmanager.repositories.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO findTaskById(int id) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return null;
        return toDTO(task);
    }

    public List<TaskResponseDTO> getProjectTasks(ProjectResponseDTO dto) {
        Project project = projectRepository.findById(dto.getId()).orElse(null);
        if (project == null) return null;
        return taskRepository.findByProject(project).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    public List<Task> getProjectTasksRaw(Project project) {
        return taskRepository.findByProject(project);
    }

    public TaskResponseDTO addTask(TaskRequestDTO dto) {
        Project project = projectRepository.findById(dto.getProjectId()).orElse(null);
        if (project == null) return null;

        byte[] imageBytes = null;
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            if (dto.getImage().length() > 7_000_000) {
                throw new IllegalArgumentException("Base64 image is too large (max ~5MB)");
            }
            try {
                imageBytes = Base64.getDecoder().decode(dto.getImage().split(",")[1]);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid base64 image", e);
            }
        }

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setProject(project);
        task.setImage(imageBytes); // Set image data

        Task saved = taskRepository.save(task);
        return toDTO(saved);
    }

    public TaskResponseDTO updateTask(int id, TaskRequestDTO dto) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return null;

        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getImage() != null) {
            byte[] imageBytes = null;
            if (!dto.getImage().isEmpty()) {
                if (dto.getImage().length() > 7_000_000) {
                    throw new IllegalArgumentException("Base64 image is too large (max ~5MB)");
                }
                try {
                    imageBytes = Base64.getDecoder().decode(dto.getImage().split(",")[1]);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid base64 image", e);
                }
            }
            task.setImage(imageBytes);
        }

        return toDTO(taskRepository.save(task));
    }

    public boolean removeTask(TaskResponseDTO dto) {
        int id = dto.getId();
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
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

    public List<TaskResponseDTO> searchTasks(TaskResponseDTO dto) {
        Project project = projectRepository.findById(dto.getProjectId()).orElse(null);
        if (project == null) return List.of();

        return taskRepository.findByProject(project).stream()
                .filter(t -> {
                    boolean matchesImage = true;
                    if (Boolean.TRUE.equals(dto.getHasImage())) {
                        matchesImage = t.getImage() != null;
                    }

                    boolean matchesTitle = true;
                    if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
                        matchesTitle = t.getTitle().toLowerCase().contains(dto.getTitle().toLowerCase());
                    }

                    return matchesImage && matchesTitle;
                })
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

}
