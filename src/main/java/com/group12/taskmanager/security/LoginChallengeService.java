package com.group12.taskmanager.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LoginChallengeService {

    private final AtomicReference<String> challenge = new AtomicReference<>();

    public LoginChallengeService() {
        rotateChallenge(); // Inicializa al arrancar
    }

    public String getCurrentChallenge() {
        return challenge.get();
    }

    @Scheduled(fixedRate = 3 * 60 * 1000) // cada 3 minutos
    public void rotateChallenge() {
        String newChallenge = UUID.randomUUID().toString().replace("-", "");
        challenge.set(newChallenge);
        System.out.println("[LoginChallengeService] Nuevo token: " + newChallenge);
    }
}

