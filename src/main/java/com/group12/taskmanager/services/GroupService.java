package com.group12.taskmanager.services;

import com.group12.taskmanager.dto.project.*;
import com.group12.taskmanager.dto.group.*;
import com.group12.taskmanager.dto.user.*;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.GroupRepository;
import com.group12.taskmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired private GroupRepository groupRepository;
    @Autowired private ProjectService projectService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;

    public List<GroupResponseDTO> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public GroupResponseDTO findGroupById(int groupId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        return (group != null) ? toDTO(group) : null;
    }

    public void saveGroup(int id, GroupRequestDTO dto) {
        Group group = groupRepository.findById(id).get();
        group.setName(dto.getName());
        User owner = userRepository.findById(dto.getOwnerID()).get();
        group.setOwner(owner);
        groupRepository.save(group);
    }

    public GroupResponseDTO createGroup(GroupRequestDTO dto) {
        User owner = userRepository.findById(dto.getOwnerID()).orElse(null);
        if (owner == null) return null;

        Group group = new Group(dto.getName(), owner);
        group.getUsers().add(owner);

        return toDTO(groupRepository.save(group));
    }

    public GroupResponseDTO updateGroup(int id, GroupRequestDTO dto) {
        Group group = groupRepository.findById(id).orElse(null);
        if (group == null) return null;

        if (dto.getName() != null) group.setName(dto.getName());
        if (dto.getOwnerID() != 0) group.setOwner(userRepository.findById(dto.getOwnerID()).get());
        return toDTO(groupRepository.save(group));
    }
    public GroupResponseDTO changeGroupOwner(int id, GroupRequestDTO dto) {
        Group group = groupRepository.findById(id).orElse(null);
        if (group == null) return null;

        group.setOwner(userService.findUserByIdRaw(dto.getOwnerID()));
        return toDTO(groupRepository.save(group));
    }

    public boolean deleteGroup(GroupResponseDTO dto) {
        int id = dto.getId();
        if (groupRepository.existsById(id)) {
            List<ProjectResponseDTO> projects = projectService.getGroupProjects(findGroupById(id));
            for (ProjectResponseDTO p : projects) {
                projectService.deleteProject(p);
            }
            groupRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<UserResponseDTO> getGroupUsers(GroupResponseDTO dto) {
        Group group = groupRepository.findByIdWithUsers(dto.getId());
        List<UserResponseDTO> users = new ArrayList<>();
        for (User u : group.getUsers()) {
            users.add(userService.toDTO(u));
        }
        return users;
    }
    public List<User> getGroupUsersRaw(GroupResponseDTO dto) {
        Group group = groupRepository.findByIdWithUsers(dto.getId());
        return group.getUsers();
    }

    public void addUserToGroup(GroupResponseDTO group, UserResponseDTO user) {
        groupRepository.addUserToGroup(group.getId(), user.getId());
    }

    public void removeUserFromGroup(GroupResponseDTO group, UserResponseDTO user) {
        groupRepository.deleteUserFromGroup(group.getId(), user.getId()); // eliminate in the BBDD
    }

    protected GroupResponseDTO toDTO(Group group) {
        return new GroupResponseDTO(group.getId(), group.getName(), group.getOwner().getId());
    }

}
