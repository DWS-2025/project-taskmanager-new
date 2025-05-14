package com.group12.taskmanager.config.security;

import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    private final UserService userService;

    public AuthenticatedUserProvider(UserService userService) {
        this.userService = userService;
    }

    public UserResponseDTO getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findUserByEmail(email);
    }
}
