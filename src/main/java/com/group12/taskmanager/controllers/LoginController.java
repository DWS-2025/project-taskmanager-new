package com.group12.taskmanager.controllers;

import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Transactional
    @PostConstruct
    public void initData() {
        if (userService.findUserByEmail("admin@admin.com") == null) {
            // Create users
            User u1 = new User("admin", "admin@admin.com", "eoHYeHEXe76Jn");
            User u2 = new User("test", "test@test.com", "eoHYeHEXe5g54");
            User u3 = new User("Roi", "roi@roi.com", "eoHYeHEXe5g54");
            User u4 = new User("Roberto", "rob@rob.com", "eoHYeHEXe5g54");

            // Save users to the database
            userService.addUser(u1);
            userService.addUser(u2);
            userService.addUser(u3);
            userService.addUser(u4);

            // Automatically create and assign personal groups
            groupService.createGroup("USER_" + u1.getName(), u1);
            groupService.createGroup("USER_" + u2.getName(), u2);
            groupService.createGroup("USER_" + u3.getName(), u3);
            groupService.createGroup("USER_" + u4.getName(), u4);

            // Create test group
            Group g = groupService.createGroup("PRUEBA", u1);
            g.getUsers().add(u2); // Associate u2 to the test group
            u2.getGroups().add(g); // Associate the test group to u2
            // Persist the test group and its relationships
            groupService.saveGroup(g);
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
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        User user = userService.findUserByEmail(email);
        // Check if credentials match
        if (user != null && user.getPassword().equals(password)) {
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

    @GetMapping("/register")
    public String showRegisterPage() {
        // Redirect to login (possibly no separate register view)
        return "redirect:/";
    }

    @PostMapping("/register")
    public String register(@RequestParam String new_username,
                           @RequestParam String email,
                           @RequestParam String new_password,
                           @RequestParam String confirm_password,
                           Model model) {

        // Check if username already exists
        if (userService.findUserByUsername(new_username) != null) {
            model.addAttribute("error", "El usuario ya existe");
            return "redirect:/";
        }
        // Check if email is already registered
        if (userService.findUserByEmail(email) != null) {
            model.addAttribute("error", "El email ya está registrado");
            return "redirect:/";
        }
        // Check if passwords match
        if (!new_password.equals(confirm_password)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/";
        }

        // Create new user
        User newUser = new User(new_username, email, new_password);
        userService.addUser(newUser); // Save user in 'user' table

        // Create personal group for the new user
        Group newGroup = groupService.createGroup("USER_" + newUser.getName(), newUser); // Group is automatically linked to the user

        // Save the group in the 'group' table
        groupService.saveGroup(newGroup);

        // Send success message
        model.addAttribute("success_message", "Registro exitoso, inicia sesión");
        return "redirect:/";
    }
}
