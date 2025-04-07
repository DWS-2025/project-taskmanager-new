package com.group12.taskmanager.controllers;

import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.GroupRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


import java.util.*;

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
            groups = groupService.getAllGroups();
            for (Group group : groups) {
                group.setIsOwner(true);
                group.setIsPersonal(group.getName().equals("USER_admin"));
            }
        } else {
            groups = currentUser.getGroups();
            for (Group group : groups) {
                // si el usuario es el dueño
                group.setIsOwner(group.getOwner().getId().equals(currentUser.getId()));
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\": false, \"message\": \"No autenticado\"}");

        Group group = groupService.findGroupById(groupId);
        if (group == null || group.getOwner().getId().equals(currentUser.getId()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"success\": false, \"message\": \"No se puede salir del grupo\"}");

        groupService.removeUserFromGroup(group, currentUser);

        // Actualizar lista de grupos del usuario en sesión
        currentUser.getGroups().removeIf(g -> g.getId().equals(groupId));
        session.setAttribute("user", currentUser);

        return ResponseEntity.ok("{\"success\": true}");
    }

    @PostMapping("/edit_group/{groupId}")
    public ResponseEntity<?> editGroup(@PathVariable int groupId, @RequestParam String name, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\": false, \"message\": \"No autenticado\"}");

        boolean success = groupService.updateGroupName(groupId, name);
        // Actualizar nombre del grupo en la sesión del usuario
        currentUser.getGroups().stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .ifPresent(g -> g.setName(name));

        session.setAttribute("user", currentUser);

        return success ? ResponseEntity.ok("{\"success\": true}")
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"success\": false, \"message\": \"Error al actualizar el grupo\"}");
    }

    @PostMapping("/delete_group/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable int groupId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"success\": false, \"message\": \"No autenticado\"}");

        boolean success = groupService.deleteGroup(groupId, currentUser);
        // Actualizar lista de grupos del usuario en sesión
        currentUser.getGroups().removeIf(g -> g.getId().equals(groupId));
        session.setAttribute("user", currentUser);

        return success ? ResponseEntity.ok("{\"success\": true}")
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"success\": false, \"message\": \"Error al eliminar el grupo\"}");
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
                    .body(Collections.singletonMap("message", "No autenticado"));
        }

        Group group = groupService.findGroupById(groupId);
        User user = userService.findUserById(userId);

        if (group == null || user == null || !group.getOwner().getId().equals(currentUser.getId())) {
            if (!currentUser.getId().equals(1))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "No autorizado o grupo/usuario no encontrado"));
        }

        if (currentUser.getId() == userId) {
            // validación admin
            if (!currentUser.getId().equals(1) || group.getOwner().getId().equals(1)) // si el user es admin y NO es el propietario
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "No puedes eliminarte si eres el propietario"));
        }

        group.getUsers().remove(user);
        groupService.saveGroup(group); // solo guarda el propietario (group)

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
        return userService.searchUsersByNameExcludingGroup(q, group);
    }

    @PostMapping("/add_members")
    public ResponseEntity<?> addMembersToGroup(@RequestBody Map<String, Object> payload) {
        try {
            int currentUserId = Integer.parseInt(payload.get("currentUserId").toString());
            User currentUser = userService.findUserById(currentUserId);
            int groupId = Integer.parseInt(payload.get("groupId").toString());
            Group group = groupService.findGroupById(groupId);

            if (currentUser == null || group == null || !group.getOwner().getId().equals(currentUser.getId())) {
                if (!currentUser.getId().equals(1)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "No autorizado"));

                }
            }

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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Error procesando la solicitud"));
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
            if (deleted && currentUser.getId().equals(userId)) session.invalidate();
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
                currentUser.setPassword(password); // En producción, encriptar aquí
            }

            userService.updateUser(currentUser);
            groupService.updateGroupName(userGroup.getId(), "USER_" + name);

            // Recargar desde la base de datos para reflejar los cambios
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        Group group = groupService.findGroupById(groupId);
        if (group == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Grupo no encontrado");
        }

        // Verificamos que el usuario actual sea el propietario
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            if (!currentUser.getId().equals(1)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permisos para cambiar el propietario");
            }
        }

        User newOwner = userService.findUserById(newOwnerId);
        if (newOwner == null || !group.getUsers().contains(newOwner)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El nuevo propietario debe ser un miembro del grupo");
        }

        // Cambiar propietario
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado");
        }
        // Retornamos solo los usuarios que son miembros del grupo

        List<User> users = group.getUsers();
        for (User user : users) {
            if (group.getOwner().getId() == user.getId()) {
                users.remove(user);
                break;
            }
        }

        return users;
    }

    @GetMapping("/paginated_groups")
    @ResponseBody
    public ResponseEntity<Page<Group>> getPaginatedGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Page<Group> groupPage = groupService.getGroupsPaginated(currentUser, page, size);
        return ResponseEntity.ok(groupPage);
    }
}
