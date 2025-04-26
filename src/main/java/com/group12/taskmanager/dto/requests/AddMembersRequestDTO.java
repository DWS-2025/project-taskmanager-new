package com.group12.taskmanager.dto.requests;

import java.util.List;

public class AddMembersRequestDTO {
    private int currentUserId;
    private List<Integer> userIds;

    // Getters y setters
    public int getCurrentUserId() { return currentUserId; }
    public void setCurrentUserId(int currentUserId) { this.currentUserId = currentUserId; }
    public List<Integer> getUserIds() { return userIds; }
    public void setUserIds(List<Integer> userIds) { this.userIds = userIds; }
}
