package com.group12.taskmanager.dto.user;

public class UserRequestDTO {
    private String name;
    private String email;
    private String password;
    private final String confirmPassword;
    private final String challenge;

    public UserRequestDTO(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.confirmPassword = password;
        this.challenge = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public String getChallenge() {
        return challenge;
    }
}
