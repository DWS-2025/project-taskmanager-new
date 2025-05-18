package com.group12.taskmanager.security;

import com.group12.taskmanager.repositories.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
    private final UserRepository userRepository;
    public UserDetailsService(UserRepository userRepo) {
        this.userRepository = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // SQL injection protection
        if (email == null || email.trim().isEmpty()) {
            throw new UsernameNotFoundException("Email no válido");
        }

        String sanitizedEmail = email.trim();

        // injection validation
        String lowered = sanitizedEmail.toLowerCase();
        if (lowered.contains("select ") || lowered.contains("insert ") || lowered.contains("update ") ||
                lowered.contains("delete ") || lowered.contains("drop ") || lowered.contains("alter ") ||
                lowered.contains("--") || lowered.contains(";") || lowered.contains("'") || lowered.contains("\"")) {
            throw new UsernameNotFoundException("Email inválido o sospechoso");
        }

        return userRepository.findByEmail(sanitizedEmail)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("❌ Usuario no encontrado"));
    }
}
