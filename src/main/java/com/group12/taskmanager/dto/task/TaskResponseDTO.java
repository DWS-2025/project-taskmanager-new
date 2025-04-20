package com.group12.taskmanager.dto.task;

public class TaskResponseDTO {
    private int id;
    private String title;
    private String description;
    private boolean hasImage;
    private int projectId;

    public TaskResponseDTO() {
    }

    public TaskResponseDTO(int id, String title, String description, boolean hasImage, int projectId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.hasImage = hasImage;
        this.projectId = projectId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }
}
