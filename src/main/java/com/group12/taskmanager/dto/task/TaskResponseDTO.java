package com.group12.taskmanager.dto.task;

import java.time.LocalDateTime;

public class TaskResponseDTO {
    private int id;
    private String title;
    private String description;
    private final boolean hasImage;
    private final int projectId;
    private final int ownerId;
    private String filename;
    private LocalDateTime lastReportGenerated;

    public TaskResponseDTO(String title, boolean hasImage, int projectId, int ownerId) {
        this.title = title;
        this.hasImage = hasImage;
        this.projectId = projectId;
        this.ownerId = ownerId;
        this.filename = null;
        this.lastReportGenerated = null;
    }
    public TaskResponseDTO(int id, String title, String description, boolean hasImage,
                           int projectId, int ownerId, String filename, LocalDateTime lastReportGenerated) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.hasImage = hasImage;
        this.projectId = projectId;
        this.ownerId = ownerId;
        this.filename = filename;
        this.lastReportGenerated = lastReportGenerated;
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

    public LocalDateTime getLastReportGenerated() {
        return lastReportGenerated;
    }

    public void setLastReportGenerated(LocalDateTime lastReportGenerated) {
        this.lastReportGenerated = lastReportGenerated;
    }
}
