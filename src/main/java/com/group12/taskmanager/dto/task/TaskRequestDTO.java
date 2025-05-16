package com.group12.taskmanager.dto.task;

public class TaskRequestDTO {
    private String title;
    private final String description;
    private final int projectId;
    private String image;
    private int ownerId;

    public TaskRequestDTO(String title, String description, int projectId, int ownerId) {
        this.title = title;
        this.description = description;
        this.image = null;
        this.projectId = projectId;
        this.ownerId = ownerId;
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

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }
}
