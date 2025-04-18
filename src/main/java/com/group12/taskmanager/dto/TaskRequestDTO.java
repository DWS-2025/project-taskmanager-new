package com.group12.taskmanager.dto;

public class TaskRequestDTO {
    private String title;
    private String description;
    private int projectId;

    public TaskRequestDTO() {
    }

    public TaskRequestDTO(String title, String description, int projectId) {
        this.title = title;
        this.description = description;
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }
}
