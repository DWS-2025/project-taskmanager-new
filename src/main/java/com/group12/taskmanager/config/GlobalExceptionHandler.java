package com.group12.taskmanager.config;

import com.group12.taskmanager.config.exceptions.ForbiddenAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenAccessException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public String handleForbidden(ForbiddenAccessException ex, Model model) {
        model.addAttribute("errorMessage", "Error 403 - " + ex.getMessage());
        return "error";
    }

}
