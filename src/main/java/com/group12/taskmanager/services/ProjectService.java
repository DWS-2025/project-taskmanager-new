package com.group12.taskmanager.services;

import com.group12.taskmanager.config.GlobalConstants;
import com.group12.taskmanager.config.exceptions.ForbiddenAccessException;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.project.ProjectRequestDTO;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.GroupRepository;
import com.group12.taskmanager.repositories.ProjectRepository;
import com.group12.taskmanager.repositories.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final GroupRepository groupRepository;
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final GlobalConstants globalConstants;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, GroupRepository groupRepository, TaskService taskService,
                          UserRepository userRepository, GlobalConstants globalConstants, UserService userService) {
        this.projectRepository = projectRepository;
        this.groupRepository = groupRepository;
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.globalConstants = globalConstants;
        this.userService = userService;
    }

    public List<ProjectResponseDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProjectResponseDTO findProjectById(int id) {
        Project project = projectRepository.findById(id).orElse(null);
        return (project != null) ? toDTO(project) : null;
    }
    // Compatibility: allow access to entity for Mustache views
    public Project findProjectByIdRaw(int id) {
        return projectRepository.findById(id).orElse(null);
    }

    public ProjectResponseDTO createProject(ProjectRequestDTO dto) {
        Group group = groupRepository.findById(dto.getGroupId()).orElse(null);
        if (group == null) return null;

        String safeName = Jsoup.clean(dto.getName(), Safelist.none());

        Project project = new Project();
        project.setName(safeName);
        project.setGroup(group);

        return toDTO(projectRepository.save(project));
    }

    public ProjectResponseDTO updateProject(int id, ProjectRequestDTO dto) {
        Project project = projectRepository.findById(id).orElse(null);
        Group group = groupRepository.findById(dto.getGroupId()).orElse(null);
        if (project == null || group == null) return null;

        if (dto.getName() != null) {
            String safeName = Jsoup.clean(dto.getName(), Safelist.none());
            project.setName(safeName);
        }
        if (dto.getGroupId() != 0) project.setGroup(group);

        return toDTO(projectRepository.save(project));
    }

    public boolean deleteProject(ProjectResponseDTO dto) {
        int id = dto.getId();
        if (projectRepository.existsById(id)) {
            List<TaskResponseDTO> tasks = taskService.getProjectTasks(findProjectById(id));
            for (TaskResponseDTO task : tasks) {
                taskService.removeTask(task);
            }
            projectRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<ProjectResponseDTO> getGroupProjects(GroupResponseDTO group) {
        List<Project> projects = projectRepository.findByGroup_Id(group.getId());
        List<ProjectResponseDTO> projectsDTO = new ArrayList<>();
        for (Project project : projects) {
            projectsDTO.add(toDTO(project));
        }
        return projectsDTO;
    }

    public Page<ProjectResponseDTO> getProjectsPaginated(UserResponseDTO currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Project> projects;

        if (currentUser.getRole().equals(globalConstants.getAdminRole())) { //if its admin it can see every project
            projects =  projectRepository.findAll(pageable);
        } else {
            User userEntity = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            projects = projectRepository.findByUser(userEntity, pageable);
        }
        return projects.map(p -> toDTO(p, currentUser.getId()));
    }

    private ProjectResponseDTO toDTO(Project project) {
        return new ProjectResponseDTO(
                project.getId(),
                project.getName(),
                project.getGroup().getId()
        );
    }
    private ProjectResponseDTO toDTO(Project project, int userId) {
        ProjectResponseDTO dto = toDTO(project); // llama al simple
        UserResponseDTO user = userService.findUserById(userId);
        boolean isOwner = user.getRole().equals(globalConstants.getAdminRole()) || project.getGroup().getOwner().getId() == userId;
        dto.setOwner(isOwner);
        return dto;
    }

}
