package com.group12.taskmanager.services;

import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupService groupService;

    // Save a new user and ensure bidirectional relation with groups
    @Transactional
    public void addUser(User user) {
        userRepository.save(user); // 👈 user is saved first
        for (Group group : user.getGroups()) {
            group.getUsers().add(user); // make sure the relationship is bidirectional
        }
    }

    // Find user by ID
    public User findUserById(int id) {
        return userRepository.findById(id).orElse(null);
    }

    // Find user by username
    public User findUserByUsername(String userName) {
        return userRepository.findByName(userName);
    }

    // Find the user's personal group named "USER_<username>"
    public Group findPersonalGroup(int userId) {
        User user = findUserById(userId);
        if (user == null) return null;

        String personalGroupName = "USER_" + user.getName();

        return user.getGroups().stream()
                .filter(group -> group.getName().equals(personalGroupName))
                .findFirst()
                .orElse(null);
    }

    // Updated to use custom query that fetches groups to avoid LazyInitializationException
    @Transactional
    public User findUserByEmail(String email) {
        return userRepository.findByEmailWithGroups(email); // Custom repo method used
    }

    // Search users by name, excluding those already in the group
    public List<User> searchUsersByNameExcludingGroup(String q, Group group) {
        if (q == null || q.trim().isEmpty()) {
            return List.of(); // avoid returning all users if query is empty
        }

        return userRepository.findByNameStartingWithExcludingGroup(q.trim(), group.getUsers());
    }

    // Update existing user
    @Transactional
    public void updateUser(User user) {
        userRepository.save(user); // Hibernate handles insert vs. update
    }

    // Delete a user from the system with checks for ownership and permissions
    public boolean deleteUser(int userId, User currentUser) {

        // Only the user themself or an admin (ID 1) can delete
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
                    groupService.deleteGroup(group.getId(), user);
                } else {
                    // If not the owner: remove only the relationship
                    group.getUsers().remove(user);
                    user.getGroups().remove(group);
                    groupService.saveGroup(group); // save the updated group
                }
            }

            // Finally, delete the user
            userRepository.delete(user);
            System.out.println("User and their groups were deleted successfully.");
            return true;

        } catch (Exception e) {
            System.out.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
