package com.group12.taskmanager.security.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtCookieInterceptor extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // If there´s already an authorization, then nothing
        if (request.getHeader("Authorization") == null) {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt".equals(cookie.getName())) {
                        // request's Authorization's header injection
                        request = new HttpServletRequestWrapper(request) {
                            @Override
                            public String getHeader(String name) {
                                if ("Authorization".equalsIgnoreCase(name)) {
                                    return "Bearer " + cookie.getValue();
                                }
                                return super.getHeader(name);
                            }
                        };
                        break;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

