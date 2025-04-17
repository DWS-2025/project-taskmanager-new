package com.group12.taskmanager.dto;

public class TaskImageDTO {
    private String base64;

    public TaskImageDTO() {}

    public TaskImageDTO(String base64) {
        this.base64 = base64;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }
}
