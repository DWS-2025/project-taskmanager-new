package com.group12.taskmanager.controllers;

import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @GetMapping("/user_groups")
    public String getUserGroups(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        List<Group> groups;

        if (currentUser.getId().equals(1)) {
            // Admin: get all groups
            groups = groupService.getAllGroups();
            for (Group group : groups) {
                group.setIsOwner(true); // Mark as owner
                group.setIsPersonal(group.getName().equals("USER_admin")); // Check if it's a personal group
            }
        } else {
            groups = currentUser.getGroups();
            for (Group group : groups) {
                // Check if current user is the owner
                group.setIsOwner(group.getOwner().getId().equals(currentUser.getId()));
                // Check if it's their personal group
                group.setIsPersonal(group.getName().equals("USER_" + currentUser.getName()));
            }
        }

        model.addAttribute("groups", groups);
        model.addAttribute("user", currentUser);
        return "groups";
    }

    @PostMapping("/save_group")
    public String createGroup(@RequestParam String name, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        groupService.createGroup(name, currentUser);
        return "redirect:/user_groups";
    }

    @PostMapping("/leave_group/{groupId}")
    public ResponseEntity<?> leaveGroup(@PathVariable int groupId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\": false, \"message\": \"Not authenticated\"}");

        Group group = groupService.findGroupById(groupId);
        // Owner can't leave their own group
        if (group == null || group.getOwner().getId().equals(currentUser.getId()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"success\": false, \"message\": \"Cannot leave the group\"}");

        groupService.removeUserFromGroup(group, currentUser);

        // Update session group list
        currentUser.getGroups().removeIf(g -> g.getId().equals(groupId));
        session.setAttribute("user", currentUser);

        return ResponseEntity.ok("{\"success\": true}");
    }

    @PostMapping("/edit_group/{groupId}")
    public ResponseEntity<?> editGroup(@PathVariable int groupId, @RequestParam String name, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\": false, \"message\": \"Not authenticated\"}");

        boolean success = groupService.updateGroupName(groupId, name);
        // Update name in session
        currentUser.getGroups().stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .ifPresent(g -> g.setName(name));

        session.setAttribute("user", currentUser);

        return success ? ResponseEntity.ok("{\"success\": true}")
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"success\": false, \"message\": \"Error updating group\"}");
    }

    @PostMapping("/delete_group/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable int groupId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\": false, \"message\": \"Not authenticated\"}");

        boolean success = groupService.deleteGroup(groupId, currentUser);
        // Update session group list
        currentUser.getGroups().removeIf(g -> g.getId().equals(groupId));
        session.setAttribute("user", currentUser);

        return success ? ResponseEntity.ok("{\"success\": true}")
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"success\": false, \"message\": \"Error deleting group\"}");
    }

    @GetMapping("/manage_members/{groupId}")
    public String getManageMembers(@PathVariable int groupId, Model model, HttpSession session) {
        Group group = groupService.findGroupById(groupId);
        User currentUser = (User) session.getAttribute("user");

        model.addAttribute("users", group.getUsers());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentGroup", group);
        return "group_members";
    }

    @DeleteMapping("/delete_member/{userId}")
    public ResponseEntity<?> deleteMember(@PathVariable int userId, @RequestParam int groupId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Not authenticated"));
        }

        Group group = groupService.findGroupById(groupId);
        User user = userService.findUserById(userId);

        // Validate permission and existence
        if (group == null || user == null || !group.getOwner().getId().equals(currentUser.getId())) {
            if (!currentUser.getId().equals(1))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "Not authorized or group/user not found"));
        }

        if (currentUser.getId() == userId) {
            // Admin self-removal validation
            if (!currentUser.getId().equals(1) || group.getOwner().getId().equals(1))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "You cannot remove yourself if you're the owner"));
        }

        group.getUsers().remove(user);
        groupService.saveGroup(group); // Save group with changes

        if (currentUser.getId().equals(userId)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "own");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }

    @GetMapping("/search_users")
    @ResponseBody
    public List<User> searchUsers(@RequestParam String q, @RequestParam int groupId) {
        Group group = groupService.findGroupById(groupId);
        // Search users by name excluding those already in group
        return userService.searchUsersByNameExcludingGroup(q, group);
    }

    @PostMapping("/add_members")
    public ResponseEntity<?> addMembersToGroup(@RequestBody Map<String, Object> payload) {
        try {
            int currentUserId = Integer.parseInt(payload.get("currentUserId").toString());
            User currentUser = userService.findUserById(currentUserId);
            int groupId = Integer.parseInt(payload.get("groupId").toString());
            Group group = groupService.findGroupById(groupId);

            // Validate permissions
            if (currentUser == null || group == null || !group.getOwner().getId().equals(currentUser.getId())) {
                if (!currentUser.getId().equals(1)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "Not authorized"));
                }
            }

            // Add new users to group
            List<Integer> userIds = ((List<?>) payload.get("userIds")).stream()
                    .map(id -> Integer.parseInt(id.toString()))
                    .toList();

            for (Integer userId : userIds) {
                User user = userService.findUserById(userId);
                if (user != null && !group.getUsers().contains(user)) {
                    group.getUsers().add(user);
                    user.getGroups().add(group);
                }
            }
            groupService.saveGroup(group);

            return ResponseEntity.ok(Collections.singletonMap("success", true));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Error processing request"));
        }
    }

    @GetMapping("/edit_user")
    public String showEditUserPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("user", currentUser);
        return "edit_user";
    }

    @PostMapping("/delete_user/{userId}")
    public String deleteUser(@PathVariable int userId, HttpSession session) {
        if (userId != 1) {
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) return "redirect:/error";

            boolean deleted = userService.deleteUser(userId, currentUser);
            if (deleted && currentUser.getId().equals(userId)) session.invalidate(); // Log out if deleting self
            return deleted ? "redirect:/" : "redirect:/error";
        }
        return "redirect:/";
    }

    @PostMapping("/update_user")
    public String updateUser(@RequestParam String name, @RequestParam String email, @RequestParam String password, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            Group userGroup = userService.findPersonalGroup(currentUser.getId());
            currentUser.setName(name);
            currentUser.setEmail(email);
            if (!password.isBlank()) {
                currentUser.setPassword(password); // In production, encrypt this
            }

            userService.updateUser(currentUser);
            groupService.updateGroupName(userGroup.getId(), "USER_" + name);

            // Reload user to reflect updates in session
            User updatedUser = userService.findUserById(currentUser.getId());
            session.setAttribute("user", updatedUser);
        }
        return "redirect:/";
    }

    @Transactional
    @PostMapping("/group/{groupId}/change_owner")
    public ResponseEntity<?> changeGroupOwner(
            @PathVariable int groupId,
            @RequestParam int newOwnerId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        Group group = groupService.findGroupById(groupId);
        if (group == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        // Only current owner or admin can change owner
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            if (!currentUser.getId().equals(1)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No permission to change owner");
            }
        }

        User newOwner = userService.findUserById(newOwnerId);
        if (newOwner == null || !group.getUsers().contains(newOwner)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("New owner must be a group member");
        }

        // Change group owner
        group.setOwner(newOwner);
        groupService.saveGroup(group);

        currentUser.getGroups().stream()
                .filter(g -> g.getId() == groupId)
                .findFirst()
                .ifPresent(g -> {
                    g.setOwner(newOwner);
                    g.setIsOwner(g.getOwner().getId() == newOwnerId);
                });

        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }

    @GetMapping("/group_members")
    @ResponseBody
    public List<User> getGroupMembers(@RequestParam int groupId) {
        Group group = groupService.findGroupById(groupId);
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }

        // Return all members except the owner
        List<User> users = group.getUsers();
        for (User user : users) {
            if (group.getOwner().getId() == user.getId()) {
                users.remove(user);
                break;
            }
        }

        return users;
    }
}
