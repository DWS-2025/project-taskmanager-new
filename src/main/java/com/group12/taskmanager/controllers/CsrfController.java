package com.group12.taskmanager.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class CsrfController {
    @GetMapping("/csrf-token")
    public Map<String, String> getToken(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
        return Map.of("token", token.getToken(), "header", token.getHeaderName());
    }
}
