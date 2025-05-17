package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.login.LoginRequest;
import com.group12.taskmanager.dto.login.LoginResponse;
import com.group12.taskmanager.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        if (!request.getPassword().matches("[a-fA-F0-9]{64}")) {  // sha256 validation
            return ResponseEntity.badRequest().body(null);
        }

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),  // email
                        request.getPassword()   // 1 time hashed password
                )
        );

        String jwt = jwtUtil.generateToken(request.getUsername());

        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)              // <- avoid JS access to the cookie
                .secure(true)                // <- only through HTTPS
                .path("/")
                .maxAge(3600)                // ← lifetime: 1h
                .sameSite("Strict")          // <- Protection against basic CSRF
                .build();

        response.setHeader("Set-Cookie", cookie.toString());

        //return ResponseEntity.ok(new LoginResponse(jwt)); <-- Authorization Bearer method
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Deletes JWT cookie
        ResponseCookie cleared = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // Here
                .sameSite("Strict")
                .build();

        response.setHeader("Set-Cookie", cleared.toString());

        return ResponseEntity.ok().build();
    }
}
