package com.group12.taskmanager.services;

import com.group12.taskmanager.dto.ProjectRequestDTO;
import com.group12.taskmanager.dto.ProjectResponseDTO;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.repositories.GroupRepository;
import com.group12.taskmanager.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired private ProjectRepository projectRepository;
    @Autowired private GroupRepository groupRepository;

    public List<ProjectResponseDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProjectResponseDTO findProjectDTOById(int id) {
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

    public boolean deleteProject(int id) {
        if (projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private ProjectResponseDTO toDTO(Project project) {
        return new ProjectResponseDTO(
                project.getId(),
                project.getName(),
                project.getGroup().getId()
        );
    }

    // Compatibility: allow access to entity for Mustache views
    public Project findProjectById(int id) {
        return projectRepository.findById(id).orElse(null);
    }

    public List<Project> getGroupProjectsRaw(Group group) {
        return projectRepository.findByGroup(group);
    }
    public List<Project> getAllProjectsRaw() {
        return projectRepository.findAll();
    }



}
