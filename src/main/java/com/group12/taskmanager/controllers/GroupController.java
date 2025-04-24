package com.group12.taskmanager.controllers;

import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserRequestDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
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
import org.springframework.data.domain.Page;


import java.util.*;

@Controller
public class GroupController {

    @Autowired private GroupService groupService;
    @Autowired private UserService userService;

    @GetMapping("/user_groups")
    public String getUserGroups(Model model, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";
        List<GroupResponseDTO> groups;

        if (currentUser.getId() == 1) {
            groups = groupService.getAllGroups();
            for (GroupResponseDTO group : groups) {
                group.setIsOwner(true);
                group.setIsPersonal(group.getName().equals("USER_admin"));
            }
        } else {
            groups = userService.getUserGroups(currentUser);
            for (GroupResponseDTO group : groups) {
                // si el usuario es el dueño
                group.setIsOwner(group.getOwnerId() == currentUser.getId());
                group.setIsPersonal(group.getName().equals("USER_" + currentUser.getName()));
            }
        }


        model.addAttribute("groups", groups);
        model.addAttribute("user", currentUser);
        return "groups";
    }


    @PostMapping("/leave_group/{groupId}")
    public ResponseEntity<?> leaveGroup(@PathVariable int groupId, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\": false, \"message\": \"No autenticado\"}");

        GroupResponseDTO group = groupService.findGroupById(groupId);
        if (group == null || group.getOwnerId() == currentUser.getId())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"success\": false, \"message\": \"No se puede salir del grupo\"}");

        UserResponseDTO currentUserDTO = userService.findUserById(currentUser.getId());
        groupService.removeUserFromGroup(group, currentUserDTO);

        session.setAttribute("user", currentUser);

        return ResponseEntity.ok("{\"success\": true}");
    }

    @GetMapping("/manage_members/{groupId}")
    public String getManageMembers(@PathVariable int groupId, Model model, HttpSession session) {
        GroupResponseDTO group = groupService.findGroupById(groupId);
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");

        List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
        model.addAttribute("users", groupUsers);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentGroup", group);
        return "group_members";
    }

    @DeleteMapping("/delete_member/{userId}")
    public ResponseEntity<?> deleteMember(@PathVariable int userId, @RequestParam int groupId, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "No autenticado"));
        }

        GroupResponseDTO group = groupService.findGroupById(groupId);
        UserResponseDTO user = userService.findUserById(userId);

        if (group == null || user == null || group.getOwnerId() != currentUser.getId()) {
            if (currentUser.getId() != 1)
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "No autorizado o grupo/usuario no encontrado"));
        }

        if (currentUser.getId() == userId) {
            // validación admin
            if (currentUser.getId() != 1 || group.getOwnerId() == 1) // si el user es admin y NO es el propietario
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

    @GetMapping("/search_users")
    @ResponseBody
    public List<UserResponseDTO> searchUsers(@RequestParam String q, @RequestParam int groupId) {
        GroupResponseDTO group = groupService.findGroupById(groupId);
        return userService.searchUsersByNameExcludingGroup(q, group);
    }

    @PostMapping("/add_members")
    public ResponseEntity<?> addMembersToGroup(@RequestBody Map<String, Object> payload) {
        try {
            int currentUserId = Integer.parseInt(payload.get("currentUserId").toString());
            UserResponseDTO currentUser = userService.findUserById(currentUserId);
            int groupId = Integer.parseInt(payload.get("groupId").toString());
            GroupResponseDTO group = groupService.findGroupById(groupId);

            if (currentUser == null || group == null || group.getOwnerId() != currentUser.getId()) {
                if (currentUser.getId() != 1) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "No autorizado"));

                }
            }

            List<Integer> userIds = ((List<?>) payload.get("userIds")).stream()
                    .map(id -> Integer.parseInt(id.toString()))
                    .toList();

            for (Integer userId : userIds) {
                UserResponseDTO user = userService.findUserById(userId);
                List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
                if (user != null && !groupUsers.contains(user)) {
                    groupService.addUserToGroup(group, user);
                }
            }

            return ResponseEntity.ok(Collections.singletonMap("success", true));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Error procesando la solicitud"));
        }
    }

    @GetMapping("/edit_user")
    public String showEditUserPage(HttpSession session, Model model) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("user", currentUser);
        return "edit_user";
    }

    @PostMapping("/delete_user/{userId}")
    public String deleteUser(@PathVariable int userId, HttpSession session) {
        if (userId != 1) {
            UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
            if (currentUser == null) return "redirect:/error";

            UserResponseDTO deletedUser = userService.findUserById(userId);
            boolean deleted = userService.deleteUser(deletedUser, currentUser);
            if (deleted && currentUser.getId() == userId) session.invalidate();
            return deleted ? "redirect:/" : "redirect:/error";
        }
        return "redirect:/";
    }

    @PostMapping("/update_user")
    public String updateUser(@RequestParam String name, @RequestParam String email, @RequestParam String password, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser != null) {
            GroupResponseDTO userGroup = userService.findPersonalGroup(currentUser);
            if (password.isBlank()) {
                password = userService.findUserByIdRaw(currentUser.getId()).getPassword();
            }
            if (email.isBlank()) {
                email = userService.findUserByIdRaw(currentUser.getId()).getEmail();
            }
            if (name.isBlank()) {
                name = userService.findUserByIdRaw(currentUser.getId()).getName();
            } else {
                GroupRequestDTO updatedGroup = new GroupRequestDTO("USER_" + name, userGroup.getOwnerId());
                groupService.updateGroup(userGroup.getId(), updatedGroup);
            }
            UserRequestDTO updatedUser = new UserRequestDTO(name, email, password);
            userService.updateUser(currentUser.getId(), updatedUser);


            // Recargar desde la base de datos para reflejar los cambios
            UserResponseDTO updatedUser1 = userService.findUserById(currentUser.getId());
            session.setAttribute("user", updatedUser1);
        }
        return "redirect:/";
    }

    @GetMapping("/group_members")
    @ResponseBody
    public List<UserResponseDTO> getGroupMembers(@RequestParam int groupId) {
        GroupResponseDTO group = groupService.findGroupById(groupId);
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado");
        }
        // Retornamos solo los usuarios que son miembros del grupo

        List<UserResponseDTO> users = groupService.getGroupUsers(group);
        for (UserResponseDTO user : users) {
            if (group.getOwnerId() == user.getId()) {
                users.remove(user);
                break;
            }
        }

        return users;
    }

    @GetMapping("/paginated_groups")
    @ResponseBody
    public ResponseEntity<Page<GroupResponseDTO>> getPaginatedGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            HttpSession session) {

        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Page<GroupResponseDTO> groupPage = groupService.getGroupsPaginated(currentUser, page, size);
        return ResponseEntity.ok(groupPage);
    }
}
