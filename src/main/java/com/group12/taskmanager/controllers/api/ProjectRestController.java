package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.security.AccessManager;
import com.group12.taskmanager.security.CustomUserDetails;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.project.ProjectRequestDTO;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.ProjectService;
import com.group12.taskmanager.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/projects")
public class ProjectRestController {

    private final ProjectService projectService;
    private final GroupService groupService;
    private final UserService userService;
    private final AccessManager accessManager;

    public ProjectRestController(ProjectService projectService, GroupService groupService,
                                 UserService userService, AccessManager accessManager) {
        this.projectService = projectService;
        this.groupService = groupService;
        this.userService = userService;
        this.accessManager = accessManager;
    }

    private boolean verifyProjectAccess(ProjectResponseDTO project, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkProjectAccess(project, currentUser);
    }
    private boolean verifyProjectOwnership(ProjectResponseDTO project, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkProjectOwnership(project, currentUser);
    }
    private boolean verifyGroupAccess(GroupResponseDTO group, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkGroupAccess(group, currentUser);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        if(accessManager.checkAdminCredentials(currentUser))
            return ResponseEntity.ok(projectService.getAllProjects());

        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponseDTO project = projectService.findProjectById(id);
        if (project == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        if(!verifyProjectAccess(project, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(project);
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@RequestBody ProjectRequestDTO dto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponseDTO group = groupService.findGroupById(dto.getGroupId());
        if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        if (!verifyGroupAccess(group, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ProjectResponseDTO created = projectService.createProject(dto);
        return (created != null)
                ? ResponseEntity.status(HttpStatus.CREATED).body(created)
                : ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(@PathVariable int id,
                                                            @RequestBody ProjectRequestDTO dto,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponseDTO project = projectService.findProjectById(id);
        if (project == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        if (!verifyProjectOwnership(project, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ProjectResponseDTO updated = projectService.updateProject(id, dto);
        return ResponseEntity.ok(updated);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponseDTO project = projectService.findProjectById(id);
        if (project == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "Project not found"));

        if (!verifyProjectOwnership(project, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean deleted = projectService.deleteProject(project);
        return deleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }

    // Pagination Controller --------------------------------------------------------------------------

    @GetMapping("/p")
    public ResponseEntity<Page<ProjectResponseDTO>> getPaginatedProjects(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {

        UserResponseDTO user = userService.findUserByEmail(userDetails.getUsername());

        Page<ProjectResponseDTO> projects = projectService.getProjectsPaginated(user, page, size);
        return ResponseEntity.ok(projects);
    }
}
