package com.group12.taskmanager.dto.task;

import java.time.LocalDateTime;

public class TaskRequestDTO {
    private String title;
    private String description;
    private int projectId;
    private String image;
    private int ownerId;
    private String filename;
    private LocalDateTime lastReportGenerated;

    public TaskRequestDTO(String title, String description, int projectId, int ownerId) {
        this.title = title;
        this.description = description;
        this.image = null;
        this.projectId = projectId;
        this.ownerId = ownerId;
        this.filename = null;
        this.lastReportGenerated = null;
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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public LocalDateTime getLastReportGenerated() {
        return lastReportGenerated;
    }

    public void setLastReportGenerated(LocalDateTime lastReportGenerated) {
        this.lastReportGenerated = lastReportGenerated;
    }
}
