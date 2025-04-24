package com.group12.taskmanager.dto.group;

public class GroupRequestDTO {
    private String name;
    private int ownerID;

    public GroupRequestDTO() {}

    public GroupRequestDTO(String name, int ownerID) {
        this.name = name;
        this.ownerID = ownerID;
    }

    public String getName() {
        return name;
    }
    public int getOwnerID() {
        return ownerID;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setOwnerID(int ownerID) {
        this.ownerID = ownerID;
    }
}
