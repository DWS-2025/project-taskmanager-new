package com.group12.taskmanager.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Get the error status code from the request
        Object status = request.getAttribute("javax.servlet.error.status_code");

        String errorMessage = "Something totally unexpected happened.";
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            // Customize the error message based on status code
            switch (statusCode) {
                case 404:
                    errorMessage = "Error 404 - Page not found.";
                    break;
                case 500:
                    errorMessage = "Error 500 - Internal server error.";
                    break;
                case 403:
                    errorMessage = "Error 403 - Access forbidden.";
                    break;
                default:
                    errorMessage = "Error code: " + statusCode;
            }
        }

        model.addAttribute("errorMessage", errorMessage);
        return "error"; // Return the error.mustache template
    }
}
