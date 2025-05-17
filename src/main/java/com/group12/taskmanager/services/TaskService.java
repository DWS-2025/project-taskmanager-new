package com.group12.taskmanager.services;

import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskRequestDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.task.TaskImageDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Task;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.ProjectRepository;
import com.group12.taskmanager.repositories.TaskRepository;
import com.group12.taskmanager.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist; // o Whitelist en versiones antiguas


import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private static final Safelist CUSTOM_SAFELIST = new Safelist()
            .addTags("b", "i", "u", "em", "strong", "p", "ul", "ol", "li", "a")
            .addAttributes("a", "href", "title")
            .addProtocols("a", "href", "http", "https");


    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
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
        return taskRepository.findByProjectId(project.getId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    public List<Task> getProjectTasksRaw(Project project) {
        return taskRepository.findByProjectId(project.getId());
    }

    public TaskResponseDTO addTask(TaskRequestDTO dto, UserResponseDTO userDTO) {
        Project project = projectRepository.findById(dto.getProjectId()).orElse(null);
        User owner = userRepository.findByEmail(userDTO.getEmail()).orElse(null);
        if (project == null || owner == null) return null;

        // Malicious image validation
        if (dto.getImage()!= null && !dto.getImage().startsWith("data:image/png") && !dto.getImage().startsWith("data:image/jpeg")) {
            throw new IllegalArgumentException("Formato de imagen no soportado. Solo PNG o JPEG.");
        }

        byte[] imageBytes = null;
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            if (dto.getImage().length() > 7_000_000) {
                throw new IllegalArgumentException("Base64 image is too large (max ~5MB)");
            }
            try {
                imageBytes = Base64.getDecoder().decode(dto.getImage().split(",")[1]); // obtains 2nd split to decode
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid base64 image", e);
            }
        }

        Task task = new Task();
        if (dto.getTitle() != null) task.setTitle(Jsoup.clean(dto.getTitle(), Safelist.none()));
        String cleanDescription = Jsoup.clean(dto.getDescription(), CUSTOM_SAFELIST); // <-- Rich text field disinfection
        task.setDescription(cleanDescription);
        task.setProject(project);
        task.setImage(imageBytes); // Set image data
        task.setOwner(owner);
        task.setFilename(null);
        task.setLastReportGenerated(null);

        Task saved = taskRepository.save(task);
        return toDTO(saved);
    }

    public TaskResponseDTO updateTask(int id, TaskRequestDTO dto) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return null;

        if (dto.getTitle() != null) task.setTitle(Jsoup.clean(dto.getTitle(), Safelist.none()));

        if (dto.getDescription() != null) {
            String cleanDescription = Jsoup.clean(dto.getDescription(), CUSTOM_SAFELIST);  // <-- Rich text field disinfection
            task.setDescription(cleanDescription);
        }

        // Malicious image validation
        if ( dto.getImage() != null && !dto.getImage().startsWith("data:image/png") && !dto.getImage().startsWith("data:image/jpeg")) {
            throw new IllegalArgumentException("Formato de imagen no soportado. Solo PNG o JPEG.");
        }

        if (dto.getImage() != null) {
            byte[] imageBytes = null;
            if (!dto.getImage().isEmpty()) {
                if (dto.getImage().length() > 7_000_000) {
                    throw new IllegalArgumentException("Base64 image is too large (max ~5MB)");
                }
                try {
                    imageBytes = Base64.getDecoder().decode(dto.getImage().split(",")[1]); // obtains 2nd split to decode
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid base64 image", e);
                }
            }
            task.setImage(imageBytes);
        }

        if (dto.getFilename() != null) {
            String safeFilename = Jsoup.clean(dto.getFilename(), Safelist.none());
            task.setFilename(safeFilename);
        }

        if (dto.getLastReportGenerated() != null) task.setLastReportGenerated(dto.getLastReportGenerated());

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
        if (task == null || dto.base64() == null) return false;

        byte[] imageBytes = Base64.getDecoder().decode(dto.base64());
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

        String filterTitle = dto.getTitle() != null
                ? Jsoup.clean(dto.getTitle(), Safelist.none()).toLowerCase()
                : null;

        return taskRepository.findByProjectId(project.getId()).stream()
                .filter(t -> {
                    boolean matchesImage = true;
                    if (Boolean.TRUE.equals(dto.getHasImage())) {
                        matchesImage = t.getImage() != null;
                    }

                    boolean matchesTitle = true;
                    if (filterTitle != null && !filterTitle.isBlank()) {
                        matchesTitle = t.getTitle().toLowerCase().contains(filterTitle.toLowerCase());
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
                task.getProject().getId(),
                task.getOwner().getId(),
                task.getFilename(),
                task.getLastReportGenerated()
        );
    }

    // Secure URL fetch method
    /*
    public void fetchUserProvidedUrl(String userInputUrl) throws Exception {
        URL url = new URL(userInputUrl);

        if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
            throw new IllegalArgumentException("Protocolo no permitido");
        }

        if (isInternalAddress(url)) {
            throw new IllegalArgumentException("Destino interno bloqueado");
        }

        List<String> allowedDomains = List.of("miweb.com", "api.segura.com");
        String host = url.getHost().toLowerCase();
        if (allowedDomains.stream().noneMatch(host::endsWith)) {
            throw new IllegalArgumentException("Dominio no permitido");
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.connect();

    }
    */

}
