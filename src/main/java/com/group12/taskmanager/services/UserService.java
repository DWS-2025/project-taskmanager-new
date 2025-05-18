package com.group12.taskmanager.services;

import com.group12.taskmanager.config.GlobalConstants;
import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserRequestDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.models.Group;
import com.group12.taskmanager.models.User;
import com.group12.taskmanager.repositories.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired private GroupService groupService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GlobalConstants globalConstants;

    public UserService(UserRepository userRepository, GlobalConstants globalConstants, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.globalConstants = globalConstants;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO findUserById(int id) {
        validateId(id);
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        return toDTO(user);
    }
    public User findUserByIdRaw(int id) {
        validateId(id);
        return userRepository.findById(id).orElse(null);
    }

    public UserResponseDTO findUserByUsername(String userName) {
        // SQL injection validation
        validateUsername(userName);

        User user = userRepository.findByName(userName).orElse(null);
        if (user == null) return null;
        return toDTO(user);
    }
    // Search users by name, excluding those already in the group
    public List<UserResponseDTO> searchUsersByNameExcludingGroup(String q, GroupResponseDTO group) {
        if (q == null || q.trim().isEmpty()) {
            return List.of(); // avoid returning all users if query is empty
        }

        // SQL injection protection
        String sanitizedQuery = q.trim();
        String lowered = sanitizedQuery.toLowerCase();
        if (lowered.contains("select ") || lowered.contains("insert ") || lowered.contains("update ") ||
                lowered.contains("delete ") || lowered.contains("drop ") || lowered.contains("alter ") ||
                lowered.contains("--") || lowered.contains(";") || lowered.contains("'") || lowered.contains("\"")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta de búsqueda inválida");
        }

        validateId(group.getId());

        List<User> groupUsers = groupService.getGroupUsersRaw(group);
        return userRepository.findByNameStartingWithExcludingGroup(sanitizedQuery, groupUsers)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO findUserByEmail(String email) {
        validateUserEmail(email);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return null;
        return toDTO(user); // Custom repo method used
    }

    public void createUser(UserRequestDTO dto) {
        validateUsername(dto.getName());
        validateUserEmail(dto.getEmail());

        String rawPassword = dto.getPassword();
        if (!dto.getPassword().matches("[a-fA-F0-9]{64}")) {         // sha256 validation
            throw new IllegalArgumentException("El nuevo password debe estar en formato SHA256");
        }
        String bcryptHash = passwordEncoder.encode(rawPassword);

        String safeName = Jsoup.clean(dto.getName(), Safelist.none());
        String safeEmail = Jsoup.clean(dto.getEmail(), Safelist.none());

        User user = new User(safeName, safeEmail, bcryptHash);

        if (safeEmail.endsWith("@admin.com") && rawPassword.equals(globalConstants.getAdminPassword())) {
            user.setRole("ROLE_ADMIN");
        } else {
            user.setRole("ROLE_USER");
        }

        userRepository.save(user); // user is saved first
        for (Group group : user.getGroups()) {
            group.getUsers().add(user); // make sure the relationship is bidirectional
        }
    }

    public UserResponseDTO updateUser(int id, UserRequestDTO dto) {
        validateId(id);
        validateUserEmail(dto.getEmail());
        validateUsername(dto.getName());

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;

        if (dto.getName() != null) {
            String safeName = Jsoup.clean(dto.getName(), Safelist.none());
            user.setName(safeName);
        }
        if (dto.getEmail() != null) {
            String safeEmail = Jsoup.clean(dto.getEmail(), Safelist.none());
            user.setEmail(safeEmail);
        }
        if (dto.getPassword() != null) {
            if (!dto.getPassword().matches("[a-fA-F0-9]{64}")) {         // sha256 validation
                throw new IllegalArgumentException("El nuevo password debe estar en formato SHA256");
            }
            String bcryptHash = passwordEncoder.encode(dto.getPassword());
            user.setPassword(bcryptHash);
        } else {
            dto.setPassword(user.getPassword());
        }
        userRepository.save(user); // Hibernate handles insert vs. update
        return toDTO(user);
    }

    public boolean deleteUser(UserResponseDTO dto) {
        validateId(dto.getId());

        int userId = dto.getId();

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
        validateId(dto.getId());

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
        validateId(user.getId());

        List<Group> userGroups = userRepository.findGroupsByUserId(user.getId());
        return userGroups.stream()
                .map(groupService::toDTO)
                .collect(Collectors.toList());
    }

    protected UserResponseDTO toDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    private void validateId(Object id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID no puede ser nulo");
        }

        try {
            int id2 = Integer.parseInt(String.valueOf(id));
            if (id2 <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID  inválido");
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID no es un número entero válido");
        }
    }
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de usuario no puede estar vacío");
        }

        String sanitized = username.trim();
        String lowered = sanitized.toLowerCase();

        if (lowered.contains("select ") || lowered.contains("insert ") || lowered.contains("update ") ||
                lowered.contains("delete ") || lowered.contains("drop ") || lowered.contains("alter ") ||
                lowered.contains("--") || lowered.contains(";") || lowered.contains("'") || lowered.contains("\"")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de usuario sospechoso");
        }

        if (sanitized.length() < 3 || sanitized.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de usuario debe tener entre 3 y 50 caracteres");
        }

        if (!sanitized.matches("^[\\w.-]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de usuario con caracteres no permitidos");
        }
    }

    private void validateUserEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email no válido");
        }

        String sanitized = email.trim().toLowerCase();
        if (sanitized.contains("select ") || sanitized.contains("insert ") || sanitized.contains("update ") ||
                sanitized.contains("delete ") || sanitized.contains("drop ") || sanitized.contains("alter ") ||
                sanitized.contains("--") || sanitized.contains(";") || sanitized.contains("'") || sanitized.contains("\"")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email sospechoso");
        }
    }

}
