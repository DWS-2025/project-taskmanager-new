package com.group12.taskmanager.controllers;

import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserRequestDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final UserService userService;
    private final GroupService groupService;

    public LoginController(UserService userService, GroupService groupService) {
        this.userService = userService;
        this.groupService = groupService;
    }

    @Transactional
    @PostConstruct
    public void initData() {
        if (userService.findUserByEmail("admin@admin.com") == null) {
            // Create users
            UserRequestDTO u1 = new UserRequestDTO("admin", "admin@admin.com", "eoHYeHEXe76Jn");
            UserRequestDTO u2 = new UserRequestDTO("test", "test@test.com", "eoHYeHEXe5g54");
            UserRequestDTO u3 = new UserRequestDTO("Roi", "roi@roi.com", "eoHYeHEXe5g54");
            UserRequestDTO u4 = new UserRequestDTO("Roberto", "rob@rob.com", "eoHYeHEXe5g54");

            // Save users to the database
            userService.createUser(u1);
            userService.createUser(u2);
            userService.createUser(u3);
            userService.createUser(u4);

            UserResponseDTO u1Rs = userService.findUserByEmail(u1.getEmail());
            UserResponseDTO u2Rs = userService.findUserByEmail(u2.getEmail());
            UserResponseDTO u3Rs = userService.findUserByEmail(u3.getEmail());
            UserResponseDTO u4Rs = userService.findUserByEmail(u4.getEmail());

            // Automatically create and assign personal groups
            groupService.createGroup( new GroupRequestDTO("USER_" + u1.getName(), u1Rs.getId()));
            groupService.createGroup(new GroupRequestDTO( "USER_" + u2.getName(), u2Rs.getId()));
            groupService.createGroup(new GroupRequestDTO( "USER_" + u3.getName(), u3Rs.getId()));
            groupService.createGroup(new GroupRequestDTO( "USER_" + u4.getName(), u4Rs.getId()));

            // Create test group
            GroupResponseDTO g = groupService.createGroup(new GroupRequestDTO("PRUEBA", u1Rs.getId()));
            groupService.addUserToGroup(g, u2Rs); // Associate u2 to the test group
        }
    }

    @GetMapping("/")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("user") != null) {
            // Redirect logged-in user to projects
            return "redirect:/projects";
        }
        return "login"; // Show login page
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpSession session, Model model) {

        UserResponseDTO user = userService.findUserByEmail(email);
        // Check if credentials match
        if (user != null && userService.validatePassword(user, password)) {
            session.setAttribute("user", user); // Set user in session
            return "redirect:/projects";
        }
        // If login fails, show error
        model.addAttribute("error", "Usuario o contraseña incorrectos");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Clear session
        return "redirect:/"; // Redirect to login page
    }

}
