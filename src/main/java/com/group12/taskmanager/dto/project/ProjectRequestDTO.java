package com.group12.taskmanager.dto.project;

public class ProjectRequestDTO {
    private String name;
    private int groupId;

    public ProjectRequestDTO() {
    }

    public ProjectRequestDTO(String name, int groupId) {
        this.name = name;
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
}
