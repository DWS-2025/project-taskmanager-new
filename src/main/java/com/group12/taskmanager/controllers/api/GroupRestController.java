package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.config.GlobalConstants;
import com.group12.taskmanager.security.AccessManager;
import com.group12.taskmanager.security.CustomUserDetails;
import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.requests.*;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/groups")
public class GroupRestController {

    private final GroupService groupService;
    private final UserService userService;
    private final GlobalConstants globalConstants;
    private final AccessManager accessManager;

    public GroupRestController(GroupService groupService, UserService userService, GlobalConstants globalConstants, AccessManager accessManager) {
        this.groupService = groupService;
        this.userService = userService;
        this.globalConstants = globalConstants;
        this.accessManager = accessManager;
    }

    private boolean verifyGroupAccess(GroupResponseDTO group, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkGroupAccess(group, currentUser);
    }
    private boolean verifyGroupOwnership(GroupResponseDTO group, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkGroupOwnership(group, currentUser);
    }
    private boolean verifyUserAccess(UserResponseDTO accessedUser, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkUserAccess(accessedUser, currentUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponseDTO> getGroupById(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponseDTO group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.notFound().build();

        if (!verifyGroupAccess(group, userDetails))
            return ResponseEntity.status((HttpStatus.UNAUTHORIZED)).build();

        return  ResponseEntity.ok(group);
    }

    // Get all User's groups
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupResponseDTO>> getGroupsByUser(@PathVariable int userId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO user = userService.findUserById(userId);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if(!verifyUserAccess(user, userDetails))
            return ResponseEntity.status((HttpStatus.UNAUTHORIZED)).build();

        List<GroupResponseDTO> groups = userService.getUserGroups(user);
        return ResponseEntity.ok(groups);
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody GroupRequestDTO dto) {
        GroupResponseDTO group = groupService.createGroup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);

    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponseDTO> updateGroup(@PathVariable int id, @RequestBody GroupRequestDTO dto,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponseDTO group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.notFound().build();

        if (!verifyGroupOwnership(group, userDetails))
            return ResponseEntity.status((HttpStatus.UNAUTHORIZED)).build();

        if (userService.findUserById(dto.getOwnerID()) == null)
            dto.setOwnerID(groupService.findGroupById(id).getOwnerId());

        if (groupService.getGroupUsers(group).stream().noneMatch(u -> u.getId() == dto.getOwnerID()))
            return ResponseEntity.status((HttpStatus.NOT_FOUND)).build();

        GroupResponseDTO updated = groupService.updateGroup(id, dto);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponseDTO group = groupService.findGroupById(id);

        if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if (!verifyGroupOwnership(group, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean deleted = groupService.deleteGroup(group);
        return (deleted)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    // Group Members Controllers -----------------------------------------------------------------------

    // Controller used in change owner modal, returns all members except the current owner
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponseDTO group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.notFound().build();

        if (!verifyGroupOwnership(group, userDetails))
            return ResponseEntity.status((HttpStatus.UNAUTHORIZED)).build();

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
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@PathVariable int id, @RequestParam String q,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponseDTO group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if (!verifyGroupOwnership(group, userDetails))
            return ResponseEntity.status((HttpStatus.UNAUTHORIZED)).build();

        List<UserResponseDTO> results = userService.searchUsersByNameExcludingGroup(q, group);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> addMembersToGroup(@PathVariable int id, @RequestBody AddMembersRequestDTO request,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            GroupResponseDTO group = groupService.findGroupById(id);
            if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

            if (!verifyGroupOwnership(group, userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
    public ResponseEntity<?> removeMemberFromGroup(@PathVariable int id, @PathVariable int userId,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {

        GroupResponseDTO group = groupService.findGroupById(id);
        UserResponseDTO user = userService.findUserById(userId);
        if (group == null || user == null) return ResponseEntity.notFound().build();

        if (!verifyGroupOwnership(group, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());

        if (currentUser.getId() == userId &&                                        // if u're trying delete yourself and u're not (admin and not owner)
                !(currentUser.getRole().equals(globalConstants.getAdminRole()) && group.getOwnerId() != currentUser.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("message", "No puedes eliminarte a ti mismo, debes cambiar el propietario"));


        UserResponseDTO owner = userService.findUserById(group.getOwnerId());
        if (user.getId() == owner.getId() || !currentUser.getRole().equals(globalConstants.getAdminRole())) // if the suplicant is not admin, or the deleted is the owner
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("message", "Acción no permitida"));

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
    public ResponseEntity<?> leaveGroup(@PathVariable int groupId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponseDTO group = groupService.findGroupById(groupId);
        if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if(!verifyGroupAccess(group, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());

        if (group.getOwnerId() == currentUser.getId())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("El propietario no puede abandonar el grupo.");

        groupService.removeUserFromGroup(group, currentUser);
        return ResponseEntity.ok("{\"success\": true}");
    }

    // Pagination Controller --------------------------------------------------------------------------

    @GetMapping("/p")
    public ResponseEntity<Page<GroupResponseDTO>> getPaginatedGroups(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {

        UserResponseDTO user = userService.findUserByEmail(userDetails.getUsername());

        Page<GroupResponseDTO> groups = groupService.getGroupsPaginated(user, page, size);
        return ResponseEntity.ok(groups);
    }

}
