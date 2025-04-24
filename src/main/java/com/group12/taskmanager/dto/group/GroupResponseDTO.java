package com.group12.taskmanager.dto.group;

public class GroupResponseDTO {
    private int id;
    private String name;
    private int ownerId;
    private boolean isOwner;
    private boolean isPersonal;

    public GroupResponseDTO() {}

    public GroupResponseDTO(int id, String name, int ownerId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.isOwner = false;
        this.isPersonal = false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(boolean owner) {
        isOwner = owner;
    }

    public boolean getIsPersonal() {
        return isPersonal;
    }

    public void setIsPersonal(boolean personal) {
        isPersonal = personal;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }
}
