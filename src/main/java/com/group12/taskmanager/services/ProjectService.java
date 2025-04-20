package com.group12.taskmanager.services;

import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.project.ProjectRequestDTO;
import com.group12.taskmanager.dto.project.ProjectResponseDTO;
import com.group12.taskmanager.dto.task.TaskResponseDTO;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.repositories.GroupRepository;
import com.group12.taskmanager.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired private ProjectRepository projectRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired
    private TaskService taskService;

    public List<ProjectResponseDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProjectResponseDTO findProjectById(int id) {
        Project project = projectRepository.findById(id).orElse(null);
        return (project != null) ? toDTO(project) : null;
    }

    public List<ProjectResponseDTO> getProjectsByGroupId(int groupId) {
        return projectRepository.findByGroup_Id(groupId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    public ProjectResponseDTO createProject(ProjectRequestDTO dto) {
        Group group = groupRepository.findById(dto.getGroupId()).orElse(null);
        if (group == null) return null;

        Project project = new Project();
        project.setName(dto.getName());
        project.setGroup(group);

        return toDTO(projectRepository.save(project));
    }

    public ProjectResponseDTO updateProject(int id, ProjectRequestDTO dto) {
        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) return null;

        if (dto.getName() != null) project.setName(dto.getName());
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

    private ProjectResponseDTO toDTO(Project project) {
        return new ProjectResponseDTO(
                project.getId(),
                project.getName(),
                project.getGroup().getId()
        );
    }

    // Compatibility: allow access to entity for Mustache views
    public Project findProjectByIdRaw(int id) {
        return projectRepository.findById(id).orElse(null);
    }

    public List<Project> getGroupProjectsRaw(Group group) {
        return projectRepository.findByGroup(group);
    }
    public List<Project> getAllProjectsRaw() {
        return projectRepository.findAll();
    }



}
