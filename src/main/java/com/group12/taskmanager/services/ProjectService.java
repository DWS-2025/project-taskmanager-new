package com.group12.taskmanager.services;

import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.Task;
import com.group12.taskmanager.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;
    private GroupService groupService;
    @Autowired
    private TaskService taskService;

    // Retrieve all projects from the database
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Create and save a new project assigned to a group
    public Project createProject(String name, int groupId) {
        Group group = groupService.findGroupById(groupId);
        if (group == null) throw new IllegalArgumentException("Group not found");
        Project project = new Project(name, group);
        return projectRepository.save(project);
    }


    // Find a project by its ID
    public Project findProjectById(int id) {
        return projectRepository.findById(id).orElse(null);
    }

    // Delete a project and all its associated tasks
    public void deleteProject(int projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        // Remove all tasks from the project
        if (project != null) {
            List<Task> tasks = taskService.getProjectTasks(project);
            for (Task task : tasks) {
                taskService.removeTask(task.getId());
            }
            projectRepository.delete(project);
        }
    }

    // Update an existing project's data
    public void updateProject(Project project) {
        projectRepository.save(project);
    }

    // Get all projects associated with a specific group
    public List<Project> getProjectsByGroup(Group group) {
        return projectRepository.findByGroup(group);
    }
}
