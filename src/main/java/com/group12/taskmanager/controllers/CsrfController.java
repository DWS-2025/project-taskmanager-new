package com.group12.taskmanager.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CsrfController {
    @GetMapping("/csrf-token")
    public ResponseEntity<Map<String, String>> getToken(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
        return ResponseEntity.ok(Map.of("token", token.getToken(), "header", token.getHeaderName()));
    }
}
