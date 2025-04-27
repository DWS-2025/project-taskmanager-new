package com.group12.taskmanager.controllers;

import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.services.GroupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/user_groups")
    public String getUserGroups(Model model, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("user", currentUser);
        return "groups"; // groups loaded by JS from /api/groups/p/:id
    }

    @GetMapping("/user_data")
    public String showEditUserPage(HttpSession session, Model model) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("user", currentUser);
        return "edit_user";
    }

    @GetMapping("/{groupId}/members")
    public String getManageMembers(@PathVariable int groupId, Model model, HttpSession session) {
        GroupResponseDTO group = groupService.findGroupById(groupId);
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("user");

        List<UserResponseDTO> groupUsers = groupService.getGroupUsers(group);
        model.addAttribute("users", groupUsers);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentGroup", group);
        return "group_members";
    }
}
