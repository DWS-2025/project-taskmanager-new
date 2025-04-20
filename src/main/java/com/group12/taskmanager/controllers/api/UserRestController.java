package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.user.UserRequestDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
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
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id) {
        UserResponseDTO user = userService.findUserById(id);
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

        UserRequestDTO newUser = new UserRequestDTO(name, email, password);
        userService.createUser(newUser);
        int newUserId = userService.findUserByEmail(email).getId();
        GroupRequestDTO group = new GroupRequestDTO("USER_" + newUser.getName(), newUserId);
        groupService.createGroup(group);

        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id,
                                        @RequestParam String name,
                                        @RequestParam String email,
                                        @RequestParam String password) {
        UserResponseDTO found = userService.findUserById(id);
        if (found == null) return ResponseEntity.notFound().build();

        UserRequestDTO user = new UserRequestDTO(name, email, password);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        userService.updateUser(id, user);

        return ResponseEntity.ok(found);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id,
                                        @RequestParam int requesterId) {
        UserResponseDTO requester = userService.findUserById(requesterId);
        if (requester == null) return ResponseEntity.status(401).body("No autorizado");

        UserResponseDTO deleted = userService.findUserById(id);
        boolean result = userService.deleteUser(deleted, requester);
        return result ? ResponseEntity.ok("Usuario eliminado")
                : ResponseEntity.status(403).body("Acción no permitida");
    }
}
