package com.group12.taskmanager.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CsrfLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
        String header = request.getHeader("X-XSRF-TOKEN");
        String param = request.getParameter("_csrf");

        System.out.println("=== CSRF DEBUG ===");
        if (token != null) {
            System.out.println("Servidor espera: " + token.getToken());
        } else {
            System.out.println("No hay token CSRF generado a\u00FAn en el servidor");
        }

        if (header != null) {
            System.out.println("Token enviado en header: " + header);
        }
        if (param != null) {
            System.out.println("Token enviado en par\u00E1metro: " + param);
        }

        filterChain.doFilter(request, response);
    }
}
