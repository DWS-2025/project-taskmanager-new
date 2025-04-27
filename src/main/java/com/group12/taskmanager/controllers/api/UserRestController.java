package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserRequestDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;
    private final GroupService groupService;

    public UserRestController(UserService userService, GroupService groupService) {
        this.userService = userService;
        this.groupService = groupService;
    }

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
    public ResponseEntity<?> createUser(@RequestBody UserRequestDTO dto, @RequestParam String confirm_password) {
        if (userService.findUserByUsername(dto.getName()) != null)
            return ResponseEntity.badRequest().body("El nombre de usuario ya existe");
        if (userService.findUserByEmail(dto.getEmail()) != null)
            return ResponseEntity.badRequest().body("El email ya está registrado");
        if (!dto.getPassword().equals(confirm_password))
            return ResponseEntity.badRequest().body("Las contraseñas no coinciden");

        UserRequestDTO newUser = new UserRequestDTO(dto.getName(), dto.getEmail(), dto.getPassword());
        userService.createUser(newUser);

        UserResponseDTO createdUser = userService.findUserByEmail(dto.getEmail());
        int newUserId = createdUser.getId();
        GroupRequestDTO group = new GroupRequestDTO("USER_" + newUser.getName(), newUserId);
        groupService.createGroup(group);

        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id, @RequestBody UserRequestDTO dto) {
        UserResponseDTO user = userService.findUserById(id);
        if (user == null) return ResponseEntity.notFound().build();

        GroupResponseDTO userGroup = userService.findPersonalGroup(user);
        if (dto.getPassword().isBlank() || dto.getPassword() == null) {
            dto.setPassword(userService.findUserByIdRaw(user.getId()).getPassword());
        }
        if (dto.getEmail().isBlank() || dto.getEmail() == null) {
            dto.setEmail(userService.findUserByIdRaw(user.getId()).getEmail());
        }
        if (dto.getName().isBlank() || dto.getName() == null) {
            dto.setName(userService.findUserByIdRaw(user.getId()).getName());
        } else {
            GroupRequestDTO updatedGroup = new GroupRequestDTO("USER_" + dto.getName(), userGroup.getOwnerId());
            groupService.updateGroup(userGroup.getId(), updatedGroup);
        }

        UserResponseDTO success =  userService.updateUser(id, dto);
        return (success != null) ? ResponseEntity.ok(user)
                :  ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id, @RequestParam int requesterId) {
        if (id != 1) {
            UserResponseDTO requester = userService.findUserById(requesterId);
            if (requester == null) return ResponseEntity.status(401).body("No autorizado");

            UserResponseDTO deleted = userService.findUserById(id);
            boolean result = userService.deleteUser(deleted, requester);
            return result ? ResponseEntity.ok("Usuario eliminado")
                    : ResponseEntity.status(403).body("Acción no permitida");
        }
        return ResponseEntity.status(403).body("Acción no permitida");
    }
}
