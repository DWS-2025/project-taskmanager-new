package com.group12.taskmanager.services;

import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserRequestDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired private GroupService groupService;
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO findUserById(int id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        return toDTO(user);
    }
    public User findUserByIdRaw(int id) {
        return userRepository.findById(id).orElse(null);
    }

    public UserResponseDTO findUserByUsername(String userName) {
        User user = userRepository.findByName(userName).orElse(null);
        if (user == null) return null;
        return toDTO(user);
    }
    // Search users by name, excluding those already in the group
    public List<UserResponseDTO> searchUsersByNameExcludingGroup(String q, GroupResponseDTO group) {
        if (q == null || q.trim().isEmpty()) {
            return List.of(); // avoid returning all users if query is empty
        }

        List<User> groupUsers = groupService.getGroupUsersRaw(group);
        return userRepository.findByNameStartingWithExcludingGroup(q.trim(), groupUsers)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO findUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return null;
        return toDTO(user); // Custom repo method used
    }

    public void createUser(UserRequestDTO dto) {
        User user = new User(dto.getName(), dto.getEmail(), dto.getPassword());
        userRepository.save(user); // user is saved first
        for (Group group : user.getGroups()) {
            group.getUsers().add(user); // make sure the relationship is bidirectional
        }
    }

    public UserResponseDTO updateUser(int id, UserRequestDTO dto) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPassword() != null) user.setPassword(dto.getPassword());
        userRepository.save(user); // Hibernate handles insert vs. update
        return toDTO(user);
    }

    public boolean deleteUser(UserResponseDTO dto, UserResponseDTO currentUserDTO) {
        int userId = dto.getId();
        User currentUser = userRepository.findById(currentUserDTO.getId()).orElse(null);
        if (currentUser == null) return false;
        // Only the own user or an admin (ID 1) can delete
        if (currentUser.getId() != userId) {
            if (currentUser.getId() != 1) {
                System.out.println("Not authorized to delete this account.");
                return false;
            }
        }

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return false;

            // Clone the group list to avoid concurrency issues
            List<Group> userGroups = new ArrayList<>(user.getGroups());

            for (Group group : userGroups) {
                if (group.getOwner().getId().equals(user.getId())) {
                    // If the user is the owner: delete the entire group
                    GroupResponseDTO groupDTO = new GroupResponseDTO();
                    groupDTO.setId(group.getId());
                    groupService.deleteGroup(groupDTO);
                } else {
                    // If not the owner: remove only the relationship
                    group.getUsers().remove(user);
                    user.getGroups().remove(group);
                    GroupRequestDTO groupDTO = new GroupRequestDTO(group.getName(), group.getOwner().getId());
                    groupService.saveGroup(group.getId(), groupDTO); // save the updated group
                }
            }

            // Finally, delete the user
            userRepository.delete(user);
            System.out.println("User and their groups were deleted successfully.");
            return true;

        } catch (Exception e) {
            System.out.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    // Find the user's personal group named "USER_<username>"
    public GroupResponseDTO findPersonalGroup(UserResponseDTO dto) {
        UserResponseDTO user = findUserById(dto.getId());
        if (user == null) return null;

        String personalGroupName = "USER_" + user.getName();

        return userRepository.findGroupsByUserId(user.getId()).stream()
                .filter(group -> group.getName().equals(personalGroupName))
                .findFirst()
                .map(groupService::toDTO)
                .orElse(null);
    }

    public List<GroupResponseDTO> getUserGroups(UserResponseDTO user) {
        List<Group> userGroups = userRepository.findGroupsByUserId(user.getId());
        return userGroups.stream()
                .map(groupService::toDTO)
                .collect(Collectors.toList());
    }

    public boolean validatePassword(UserResponseDTO user, String password) {
        User user1 = userRepository.findById(user.getId()).orElse(null);
        if (user1 == null) return false;
        if (userRepository.existsById(user.getId())) {
            return (user1.getPassword().equals(password));
        }
        return false;
    }

    protected UserResponseDTO toDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

}
