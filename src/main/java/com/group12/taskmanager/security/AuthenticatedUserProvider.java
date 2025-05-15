package com.group12.taskmanager.security;

import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    private final UserService userService;

    public AuthenticatedUserProvider(UserService userService) {
        this.userService = userService;
    }

    public UserResponseDTO getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        String email = auth.getName();
        return userService.findUserByEmail(email);
    }
}
