package com.group12.taskmanager.dto.task;

public class TaskRequestDTO {
    private String title;
    private final String description;
    private final int projectId;
    private String image;

    public TaskRequestDTO(String title, String description, int projectId) {
        this.title = title;
        this.description = description;
        this.image = null;
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

    public int getProjectId() {
        return projectId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
