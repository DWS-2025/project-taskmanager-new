package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.requests.*;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/groups")
public class GroupRestController {

    private final GroupService groupService;
    private final UserService userService;

    public GroupRestController(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<GroupResponseDTO>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponseDTO> getGroupById(@PathVariable int id) {
        GroupResponseDTO group = groupService.findGroupById(id);
        return (group != null) ? ResponseEntity.ok(group) : ResponseEntity.notFound().build();
    }

    // Get all User's groups
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupResponseDTO>> getGroupsByUser(@PathVariable int userId) {
        UserResponseDTO user = userService.findUserById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<GroupResponseDTO> groups = userService.getUserGroups(user);
        return ResponseEntity.ok(groups);
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

    // Group Members Controllers -----------------------------------------------------------------------

    // Controller used in change owner modal, returns all members except the current owner
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable int id) {
        GroupResponseDTO group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.notFound().build();

        List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
        // Removes group owner
        for (UserResponseDTO user : groupUsers) {
            if (group.getOwnerId() == user.getId()) {
                groupUsers.remove(user);
                break;
            }
        }
        return ResponseEntity.ok(groupUsers);
    }

    // Search users by name, and they can't be in the group yet
    @GetMapping("/{id}/search_users")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@PathVariable int id, @RequestParam String q) {
        GroupResponseDTO group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<UserResponseDTO> results = userService.searchUsersByNameExcludingGroup(q, group);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> addMembersToGroup(@PathVariable int id, @RequestBody AddMembersRequestDTO request) {
        try {
            int currentUserId = request.getCurrentUserId();
            UserResponseDTO currentUser = userService.findUserById(currentUserId);
            if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            GroupResponseDTO group = groupService.findGroupById(id);
            if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

            if (group.getOwnerId() != currentUser.getId() && currentUser.getId() != 1) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("message", "No autorizado"));
            }

            List<Integer> userIds = request.getUserIds().stream()
                    .map(idObj -> Integer.parseInt(idObj.toString()))
                    .toList();

            List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
            for (Integer userId : userIds) {
                UserResponseDTO user = userService.findUserById(userId);
                if (user != null && !groupUsers.contains(user)) {
                    groupService.addUserToGroup(group, user);
                }
            }

            return ResponseEntity.ok(Collections.singletonMap("success", true));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Error procesando la solicitud"));
        }
    }

    @DeleteMapping("/{id}/{userId}")
    public ResponseEntity<?> removeMemberFromGroup(@PathVariable int id, @PathVariable int userId, @RequestBody CurrentUserRequestDTO request) {
        UserResponseDTO currentUser = userService.findUserById(request.getCurrentUserId());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "No autenticado"));
        }

        GroupResponseDTO group = groupService.findGroupById(id);
        UserResponseDTO user = userService.findUserById(userId);
        if (group == null || user == null) return ResponseEntity.notFound().build();

        if (group.getOwnerId() != currentUser.getId() && currentUser.getId() != 1)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "No autorizado"));

        if (currentUser.getId() == userId) {
            // ADMIN validation
            if (currentUser.getId() != 1 || group.getOwnerId() == 1) // if the user is admin and IS NOT the owner
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "No puedes eliminarte si eres el propietario"));
        }

        groupService.removeUserFromGroup(group, user);

        if (currentUser.getId() == userId) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "own");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }

    // Allow user leave a group
    @DeleteMapping("/l/{groupId}")
    public ResponseEntity<?> leaveGroup(@PathVariable int groupId, @RequestBody CurrentUserRequestDTO request) {
        GroupResponseDTO group = groupService.findGroupById(groupId);
        UserResponseDTO currentUser = userService.findUserById(request.getCurrentUserId());

        if (group == null || currentUser == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if(group.getOwnerId() == currentUser.getId())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"success\": false, \"message\": \"No se puede salir del grupo\"}");

        List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
        boolean found = false;
        for (UserResponseDTO user : groupUsers) {
            if (user.getId() == currentUser.getId()) {
                found = true;
                break;
            }
        }
        if (!found) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "El usuario no pertenece al grupo."));

        if (group.getOwnerId() == currentUser.getId()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("El propietario no puede abandonar el grupo.");
        }

        groupService.removeUserFromGroup(group, currentUser);
        return ResponseEntity.ok("{\"success\": true}");
    }

    // Pagination Controller --------------------------------------------------------------------------

    @GetMapping("/p/{userId}")
    public ResponseEntity<Page<GroupResponseDTO>> getPaginatedGroups(@PathVariable int userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {

        UserResponseDTO user = userService.findUserById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Page<GroupResponseDTO> groups = groupService.getGroupsPaginated(user, page, size);
        return ResponseEntity.ok(groups);
    }

}
