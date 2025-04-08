package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @Autowired private UserService userService;
    @Autowired private GroupService groupService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id) {
        User user = userService.findUserById(id);
        return (user != null) ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestParam String name,
                                        @RequestParam String email,
                                        @RequestParam String password) {
        if (userService.findUserByUsername(name) != null)
            return ResponseEntity.badRequest().body("El nombre de usuario ya existe");
        if (userService.findUserByEmail(email) != null)
            return ResponseEntity.badRequest().body("El email ya está registrado");

        User newUser = new User(name, email, password);
        userService.addUser(newUser);
        Group group = groupService.createGroup("USER_" + newUser.getName(), newUser);
        groupService.saveGroup(group);

        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id,
                                        @RequestParam String name,
                                        @RequestParam String email,
                                        @RequestParam String password) {
        User user = userService.findUserById(id);
        if (user == null) return ResponseEntity.notFound().build();

        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        userService.updateUser(user);

        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id,
                                        @RequestParam int requesterId) {
        User requester = userService.findUserById(requesterId);
        if (requester == null) return ResponseEntity.status(401).body("No autorizado");

        boolean result = userService.deleteUser(id, requester);
        return result ? ResponseEntity.ok("Usuario eliminado")
                : ResponseEntity.status(403).body("Acción no permitida");
    }
}
