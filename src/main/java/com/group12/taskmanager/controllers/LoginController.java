package com.group12.taskmanager.controllers;

import com.group12.taskmanager.security.LoginChallengeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final LoginChallengeService challengeService;

    public LoginController(LoginChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginChallenge", challengeService.getCurrentChallenge());
        return "login";
    }

}
