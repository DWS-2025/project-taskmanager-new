package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
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
    public List<Group> getAllGroups() {
        return groupService.getAllGroups();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroupById(@PathVariable int id) {
        Group group = groupService.findGroupById(id);
        return (group != null) ? ResponseEntity.ok(group) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestParam String name, @RequestParam int ownerId) {
        User owner = userService.findUserById(ownerId);
        if (owner == null) return ResponseEntity.badRequest().body("Propietario no encontrado");

        Group group = groupService.createGroup(name, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroupName(@PathVariable int id, @RequestParam String name) {
        boolean success = groupService.updateGroupName(id, name);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable int id, @RequestParam int requesterId) {
        User requester = userService.findUserById(requesterId);
        if (requester == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean success = groupService.deleteGroup(id, requester);
        return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMembers(@PathVariable int id, @RequestBody List<Integer> userIds) {
        Group group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.notFound().build();

        for (Integer userId : userIds) {
            User user = userService.findUserById(userId);
            if (user != null && !group.getUsers().contains(user)) {
                group.getUsers().add(user);
                user.getGroups().add(group);
            }
        }

        groupService.saveGroup(group);
        return ResponseEntity.ok("Miembros añadidos");
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable int id, @PathVariable int userId) {
        Group group = groupService.findGroupById(id);
        User user = userService.findUserById(userId);
        if (group == null || user == null) return ResponseEntity.notFound().build();

        groupService.removeUserFromGroup(group, user);
        return ResponseEntity.ok("Miembro eliminado");
    }

    @PutMapping("/{id}/change_owner")
    public ResponseEntity<?> changeOwner(@PathVariable int id, @RequestParam int newOwnerId) {
        Group group = groupService.findGroupById(id);
        User newOwner = userService.findUserById(newOwnerId);

        if (group == null || newOwner == null || !group.getUsers().contains(newOwner)) {
            return ResponseEntity.badRequest().body("Datos inválidos");
        }

        group.setOwner(newOwner);
        groupService.saveGroup(group);
        return ResponseEntity.ok("Propietario actualizado");
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable int id) {
        Group group = groupService.findGroupById(id);
        if (group == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(group.getUsers());
    }
}
