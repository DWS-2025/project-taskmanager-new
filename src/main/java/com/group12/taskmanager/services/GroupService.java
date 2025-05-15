package com.group12.taskmanager.services;

import com.group12.taskmanager.config.GlobalConstants;
import com.group12.taskmanager.dto.project.*;
import com.group12.taskmanager.dto.group.*;
import com.group12.taskmanager.dto.user.*;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.GroupRepository;
import com.group12.taskmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired private UserService userService;
    private final GroupRepository groupRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final GlobalConstants globalConstants;

    public GroupService(GroupRepository groupRepository, ProjectService projectService, UserRepository userRepository, GlobalConstants globalConstants) {
        this.groupRepository = groupRepository;
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.globalConstants = globalConstants;
    }

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
        Group group = groupRepository.findById(id).orElse(null);
        User owner = userRepository.findById(dto.getOwnerID()).orElse(null);
        if (group != null && owner != null) {
            group.setName(dto.getName());
            group.setOwner(owner);
            groupRepository.save(group);
        }else {
            System.out.println("Invalid credentials at GroupService - saveGroup()");
        }
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
        User owner = userRepository.findById(dto.getOwnerID()).orElse(null);
        if (group == null || owner == null) return null;

        if (dto.getName() != null) group.setName(dto.getName());
        if (dto.getOwnerID() != 0) group.setOwner(owner);
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

    @Transactional
    public void addUserToGroup(GroupResponseDTO group, UserResponseDTO user) {
        groupRepository.addUserToGroup(group.getId(), user.getId());
    }

    @Transactional
    public void removeUserFromGroup(GroupResponseDTO group, UserResponseDTO user) {
        groupRepository.deleteUserFromGroup(group.getId(), user.getId()); // eliminate in the DB
    }

    public Page<GroupResponseDTO> getGroupsPaginated(UserResponseDTO currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Group> groups;

        if (currentUser.getRole().equals(globalConstants.getAdminRole())) { //if its admin it can see every group
            groups =  groupRepository.findAll(pageable);
        } else {
            User userEntity = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            groups = groupRepository.findByUsersContains(userEntity, pageable); // normal user
        }

        return groups.map(group -> {
            GroupResponseDTO dto = toDTO(group);

            // flags logic
            if (currentUser.getRole().equals(globalConstants.getAdminRole())) {
                dto.setIsOwner(true);
                dto.setIsPersonal(group.getName().equals("USER_admin"));
            } else {
                dto.setIsOwner(group.getOwner().getId() == currentUser.getId());
                dto.setIsPersonal(group.getName().equals("USER_" + currentUser.getName()));
            }

            return dto;
        });
    }

    protected GroupResponseDTO toDTO(Group group) {
        return new GroupResponseDTO(group.getId(), group.getName(), group.getOwner().getId());
    }

}
