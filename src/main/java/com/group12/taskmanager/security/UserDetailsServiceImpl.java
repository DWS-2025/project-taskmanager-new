package com.group12.taskmanager.security;

import com.group12.taskmanager.repositories.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    public UserDetailsServiceImpl(UserRepository userRepo) {
        this.userRepository = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        System.out.println("🟡 Buscando usuario: " + email);
        CustomUserDetails user = userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("❌ Usuario no encontrado"));

        System.out.println("✅ Usuario encontrado: " + user.getUsername());
        System.out.println("🔑 Password (hasheada): " + user.getPassword());

        return user;
    }
}
