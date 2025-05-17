package com.group12.taskmanager.dto.login;

public class LoginRequest {
    private String username;
    private String password;
    private String challenge;

    // Getters y setters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getChallenge() { return challenge; }
}
