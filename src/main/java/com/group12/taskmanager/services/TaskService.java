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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist; // may be Whitelist in deprecated versions
import org.springframework.web.server.ResponseStatusException;


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
        validateId(id);
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return null;
        return toDTO(task);
    }

    public List<TaskResponseDTO> getProjectTasks(ProjectResponseDTO dto) {
        // SQL injection protection
        validateId(dto.getId());

        Project project = projectRepository.findById(dto.getId()).orElse(null);
        if (project == null) return null;
        return taskRepository.findByProjectId(project.getId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    public List<Task> getProjectTasksRaw(Project project) {
        // SQL injection protection
        validateId(project.getId());

        return taskRepository.findByProjectId(project.getId());
    }

    public TaskResponseDTO addTask(TaskRequestDTO dto, UserResponseDTO userDTO) {
        // SQL injection protection
        // projectId validation
        validateId(dto.getProjectId());

        // email validation
        if (userDTO == null || userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email de usuario no válido");
        }

        String sanitizedEmail = userDTO.getEmail().trim();
        String lowered = sanitizedEmail.toLowerCase();
        if (lowered.contains("select ") || lowered.contains("insert ") || lowered.contains("update ") ||
                lowered.contains("delete ") || lowered.contains("drop ") || lowered.contains("alter ") ||
                lowered.contains("--") || lowered.contains(";") || lowered.contains("'") || lowered.contains("\"")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email de usuario sospechoso");
        }

        Project project = projectRepository.findById(dto.getProjectId()).orElse(null);
        User owner = userRepository.findByEmail(sanitizedEmail).orElse(null);
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
        if (dto.getTitle() != null) task.setTitle(sanitizeAndValidateTitle(dto.getTitle()));
        task.setDescription(sanitizeAndValidateRichText(dto.getDescription()));
        task.setProject(project);
        task.setImage(imageBytes); // Set image data
        task.setOwner(owner);
        task.setFilename(null);
        task.setLastReportGenerated(null);

        Task saved = taskRepository.save(task);
        return toDTO(saved);
    }

    public TaskResponseDTO updateTask(int id, TaskRequestDTO dto) {
        validateId(id);

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) return null;

        if (dto.getTitle() != null) task.setTitle(sanitizeAndValidateTitle(dto.getTitle()));

        if (dto.getDescription() != null) task.setDescription(sanitizeAndValidateRichText(dto.getDescription()));

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
        validateId(dto.getId());

        int id = dto.getId();
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean uploadImage(int id, TaskImageDTO dto) {
        validateId(id);

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null || dto.base64() == null) return false;

        byte[] imageBytes = Base64.getDecoder().decode(dto.base64());
        task.setImage(imageBytes);
        taskRepository.save(task);
        return true;
    }

    public TaskImageDTO getImage(int id) {
        validateId(id);
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null || task.getImage() == null) return null;

        String encoded = Base64.getEncoder().encodeToString(task.getImage());
        return new TaskImageDTO(encoded);
    }

    public TaskResponseDTO deleteImage(int id) {
        validateId(id);
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null || task.getImage() == null) return null;

        task.setImage(null);
        return toDTO(taskRepository.save(task));
    }

    public List<TaskResponseDTO> searchTasks(TaskResponseDTO dto) {
        // SQL injection protection
        validateId(dto.getProjectId());
        dto.setTitle(sanitizeAndValidateTitle2(dto.getTitle()));

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

    private String sanitizeAndValidateTitle2(String rawTitle) {
        if (rawTitle == null || rawTitle.trim().isEmpty()) {
            return rawTitle;
        }
        // HTML and script validation
        String cleaned = Jsoup.clean(rawTitle.trim(), Safelist.none());
        String lowered = cleaned.toLowerCase();

        // SQL validation
        if (lowered.contains("select ") || lowered.contains("insert ") || lowered.contains("update ") ||
                lowered.contains("delete ") || lowered.contains("drop ") || lowered.contains("alter ") ||
                lowered.contains("create ") || lowered.contains("--") || lowered.contains(";")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título sospechoso");
        }

        if (cleaned.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es demasiado largo");
        }

        return cleaned;
    }
    private String sanitizeAndValidateTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título no puede estar vacío");
        }

        // HTML and script validation
        String cleaned = Jsoup.clean(rawTitle.trim(), Safelist.none());
        String lowered = cleaned.toLowerCase();

        // SQL validation
        if (lowered.contains("select ") || lowered.contains("insert ") || lowered.contains("update ") ||
                lowered.contains("delete ") || lowered.contains("drop ") || lowered.contains("alter ") ||
                lowered.contains("create ") || lowered.contains("--") || lowered.contains(";")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título sospechoso");
        }

        if (cleaned.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es demasiado largo");
        }

        return cleaned;
    }
    private String sanitizeAndValidateRichText(String rawHtml) {
        if (rawHtml == null || rawHtml.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción no puede estar vacía");
        }

        // step 1:
        String cleanedHtml = Jsoup.clean(rawHtml.trim(), CUSTOM_SAFELIST);

        // step 2: Extract visible text to validate commands
        String visibleText = Jsoup.parse(cleanedHtml).text().toLowerCase();

        // step 3:
        if (visibleText.contains("select ") || visibleText.contains("insert ") || visibleText.contains("update ") ||
                visibleText.contains("delete ") || visibleText.contains("drop ") || visibleText.contains("alter ") ||
                visibleText.contains("create ") || visibleText.contains("--") || visibleText.contains(";")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contenido sospechoso en la descripción");
        }

        // step 4: length validation
        if (cleanedHtml.length() > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es demasiado larga");
        }

        return cleanedHtml;
    }
    private void validateId(Object id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID no puede ser nulo");
        }

        try {
            int id2 = Integer.parseInt(String.valueOf(id));
            if (id2 <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID  inválido");
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID no es un número entero válido");
        }
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
