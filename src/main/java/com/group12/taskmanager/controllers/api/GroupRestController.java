package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/groups")
public class GroupRestController {

    @Autowired private GroupService groupService;
    @Autowired private UserService userService;

    @GetMapping
    public ResponseEntity<List<GroupResponseDTO>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponseDTO> getGroupById(@PathVariable int id) {
        GroupResponseDTO group = groupService.findGroupById(id);
        return (group != null) ? ResponseEntity.ok(group) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody GroupRequestDTO dto) {
        GroupResponseDTO group = groupService.createGroup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);

    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponseDTO> updateGroup(@PathVariable int id, @RequestBody GroupRequestDTO dto) {
        GroupResponseDTO group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.notFound().build();

        if (dto.getOwnerID() == 0) {
            dto.setOwnerID(groupService.findGroupById(id).getOwnerId());
        } else {
            if (userService.findUserById(dto.getOwnerID()) == null)
                return ResponseEntity.notFound().build();
        }
        GroupResponseDTO updated = groupService.updateGroup(id, dto);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable int id, @RequestParam int requesterId) {
        UserResponseDTO requester = userService.findUserById(requesterId);
        GroupResponseDTO group = groupService.findGroupById(id);

        if (requester == null || group == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if (group.getOwnerId() != requesterId && requester.getId() != 1)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean deleted = groupService.deleteGroup(group);
        return (deleted)
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMembers(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        try {
            int currentUserId = Integer.parseInt(payload.get("currentUserId").toString());
            UserResponseDTO currentUser = userService.findUserById(currentUserId);
            GroupResponseDTO group = groupService.findGroupById(id);

            if (currentUser == null || group == null || group.getOwnerId()!=currentUser.getId()) {
                if (currentUser == null || currentUser.getId() != 1) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Collections.singletonMap("message", "No autorizado"));
                }
            }

            List<Integer> userIds = ((List<?>) payload.get("userIds")).stream()
                    .map(idObj -> Integer.parseInt(idObj.toString()))
                    .toList();

            List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
            for (Integer userId : userIds) {
                UserResponseDTO user = userService.findUserById(userId);
                if (user != null && !groupUsers.contains(user)) {
                    groupService.addUserToGroup(group, user);
                }
            }

            GroupResponseDTO dto = groupService.findGroupById(id);
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Error procesando la solicitud"));
        }
    }


    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable int id, @PathVariable int userId) {
        GroupResponseDTO group = groupService.findGroupById(id);
        UserResponseDTO user = userService.findUserById(userId);
        if (group == null || user == null) return ResponseEntity.notFound().build();

        groupService.removeUserFromGroup(group, user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable int id) {
        GroupResponseDTO group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.notFound().build();

        List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
        return ResponseEntity.ok(groupUsers);
    }

    // Obtener todos los grupos de un usuario
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupResponseDTO>> getGroupsByUser(@PathVariable int userId) {
        UserResponseDTO user = userService.findUserById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<GroupResponseDTO> groups = userService.getUserGroups(user);
        return ResponseEntity.ok(groups);
    }

    // Permitir que un usuario abandone un grupo
    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable int groupId, @RequestParam int userId) {
        GroupResponseDTO group = groupService.findGroupById(groupId);
        UserResponseDTO user = userService.findUserById(userId);

        if (group == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
        if (!groupUsers.contains(user)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El usuario no pertenece al grupo.");
        }

        if (group.getOwnerId() == user.getId()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("El propietario no puede abandonar el grupo.");
        }

        groupService.removeUserFromGroup(group, user);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // Buscar usuarios por nombre que NO estén ya en el grupo
    @GetMapping("/{groupId}/search_users")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(
            @PathVariable int groupId,
            @RequestParam String q) {

        GroupResponseDTO group = groupService.findGroupById(groupId);
        if (group == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<UserResponseDTO> results = userService.searchUsersByNameExcludingGroup(q, group);
        return ResponseEntity.ok(results);
    }

}
