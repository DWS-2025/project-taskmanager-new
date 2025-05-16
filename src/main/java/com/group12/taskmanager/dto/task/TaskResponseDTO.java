package com.group12.taskmanager.dto.task;

public class TaskResponseDTO {
    private int id;
    private String title;
    private String description;
    private final boolean hasImage;
    private final int projectId;
    private final int ownerId;
    private String filename;

    public TaskResponseDTO(String title, boolean hasImage, int projectId, int ownerId) {
        this.title = title;
        this.hasImage = hasImage;
        this.projectId = projectId;
        this.ownerId = ownerId;
        this.filename = null;
    }
    public TaskResponseDTO(int id, String title, String description, boolean hasImage, int projectId, int ownerId, String filename) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.hasImage = hasImage;
        this.projectId = projectId;
        this.ownerId = ownerId;
        this.filename = filename;
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

    public boolean getHasImage() {
        return hasImage;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
