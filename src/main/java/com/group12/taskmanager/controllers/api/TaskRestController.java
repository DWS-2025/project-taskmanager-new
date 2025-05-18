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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Font;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
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

        if (!verifyTaskOwnership(task, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        TaskImageDTO image = taskService.getImage(id);
        return (image != null) ? ResponseEntity.ok(image) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getFile(@PathVariable int id,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        TaskResponseDTO task = taskService.findTaskById(id);

        if (!verifyTaskOwnership(task, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // ----------------------- LFI protections ---------------------------
        String filename = task.getFilename();
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\"))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        Path safeBase = Paths.get("uploadedReports/tasks/" + id).toAbsolutePath().normalize();
        Path filePath = safeBase.resolve(task.getFilename()).normalize();

        if (!filePath.startsWith(safeBase))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        // -------------------------------------------------------------------

        if (!Files.exists(filePath))
            return ResponseEntity.notFound().build();

        Resource fileResource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + task.getFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileResource);
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<?> generateAndDownloadReport(@PathVariable int id,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        TaskResponseDTO task = taskService.findTaskById(id);
        if (task == null) return ResponseEntity.notFound().build();

        // DoS protection
        if (task.getLastReportGenerated() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (task.getLastReportGenerated().plusSeconds(60).isAfter(now)) {
                return ResponseEntity.status(429)
                        .body("Espera unos segundos antes de generar otro informe.");
            }
        }

        // Access Control
        if (!verifyTaskOwnership(task, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String rawTitle = task.getTitle().replaceAll("[^a-zA-Z0-9\\s]", "").trim(); // erase dangerous chars
        String filenameBase = rawTitle.isEmpty() ? ("tarea_" + id) : rawTitle;

        Path folder = Paths.get("uploadedReports/tasks/" + id);
        Files.createDirectories(folder);

        int counter = 0;
        String finalFilename;
        Path finalPath;

        do {
            finalFilename = (counter == 0)
                    ? filenameBase + ".pdf"
                    : filenameBase + "(" + counter + ").pdf";

            // ------------------- LFI protections --------------------------
            Path folderAbs = folder.toAbsolutePath().normalize();
            finalPath = folderAbs.resolve(finalFilename).normalize();

            if (!finalPath.startsWith(folder.toAbsolutePath().normalize())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            // --------------------------------------------------------------

            counter++;
        } while (Files.exists(finalPath));

        // Generate PDF
        try (OutputStream os = Files.newOutputStream(finalPath)) {
            Document document = new Document();
            PdfWriter.getInstance(document, os);
            document.open();

            // ---------- Title style ----------
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph(task.getTitle(), titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            // ---------- Description with styles ----------
            String htmlDescription = task.getDescription(); // contains HTML from Quill
            try {
                List<Element> htmlElements = HTMLWorker.parseToList(new StringReader(htmlDescription), null);
                for (Element e : htmlElements) {
                    document.add(e);
                }
            } catch (Exception e) {
                document.add(new Paragraph("[Error al procesar la descripci\u00F3n enriquecida]"));
            }

            document.add(new Paragraph(" "));

            // ---------- Image ----------
            if (task.getHasImage()) {
                TaskImageDTO imageDTO = taskService.getImage(id);
                if (imageDTO != null && imageDTO.base64() != null) {
                    try {
                        String base64 = imageDTO.base64();
                        if (base64.contains(",")) {
                            base64 = base64.split(",")[1];
                        }
                        byte[] imageBytes = Base64.getDecoder().decode(base64);
                        Image img = Image.getInstance(imageBytes);
                        img.scaleToFit(400, 400);
                        img.setAlignment(Image.ALIGN_CENTER);
                        document.add(img);
                    } catch (Exception e) {
                        document.add(new Paragraph("[Error al insertar imagen]"));
                    }
                }
            }

            document.close();
        }

        // Save in DB
        TaskRequestDTO taskRequest = new TaskRequestDTO(task.getTitle(), task.getDescription(), task.getProjectId(), task.getOwnerId());
        taskRequest.setFilename(finalFilename);
        taskRequest.setLastReportGenerated(LocalDateTime.now());

        taskService.updateTask(id, taskRequest);


        // Returns file as a download
        Resource resource = new UrlResource(finalPath.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + finalFilename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }



}
