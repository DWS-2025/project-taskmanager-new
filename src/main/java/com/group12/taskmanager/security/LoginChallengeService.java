package com.group12.taskmanager.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LoginChallengeService {

    private final AtomicReference<String> challenge = new AtomicReference<>();

    public LoginChallengeService() {
        rotateChallenge(); // initializes at boot
    }

    public String getCurrentChallenge() {
        return challenge.get();
    }

    @Scheduled(fixedRate = 3 * 60 * 1000) // every 3 min
    public void rotateChallenge() {
        String newChallenge = UUID.randomUUID().toString().replace("-", "");
        challenge.set(newChallenge);
        System.out.println("[LoginChallengeService] Nuevo token: " + newChallenge);
    }
}

