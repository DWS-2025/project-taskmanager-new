package com.group12.taskmanager.controllers.api;

import com.group12.taskmanager.config.GlobalConstants;
import com.group12.taskmanager.dto.group.GroupRequestDTO;
import com.group12.taskmanager.dto.group.GroupResponseDTO;
import com.group12.taskmanager.dto.user.UserRequestDTO;
import com.group12.taskmanager.dto.user.UserResponseDTO;
import com.group12.taskmanager.security.AccessManager;
import com.group12.taskmanager.security.CustomUserDetails;
import com.group12.taskmanager.security.LoginChallengeService;
import com.group12.taskmanager.services.GroupService;
import com.group12.taskmanager.services.UserService;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;
    private final GroupService groupService;
    private final AccessManager accessManager;
    private final LoginChallengeService loginChallengeService;
    private final GlobalConstants globalConstants;

    public UserRestController(UserService userService, GroupService groupService, AccessManager accessManager,
                              LoginChallengeService loginChallengeService, GlobalConstants globalConstants) {
        this.userService = userService;
        this.groupService = groupService;
        this.accessManager = accessManager;
        this.loginChallengeService = loginChallengeService;
        this.globalConstants = globalConstants;
    }

    private boolean verifyUserAccess(UserResponseDTO accessedUser, CustomUserDetails userDetails) {
        UserResponseDTO currentUser = userService.findUserByEmail(userDetails.getUsername());
        return accessManager.checkUserAccess(accessedUser, currentUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO user = userService.findUserById(id);

        if (user == null)
            return ResponseEntity.notFound().build();

        if (!verifyUserAccess(user, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequestDTO dto) {

        if (!dto.getChallenge().equals(loginChallengeService.getCurrentChallenge()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (userService.findUserByUsername(dto.getName()) != null)
            return ResponseEntity.badRequest().body("El nombre de usuario ya existe");
        if (userService.findUserByEmail(dto.getEmail()) != null)
            return ResponseEntity.badRequest().body("El email ya est&#225; registrado");
        if (!dto.getPassword().equals(dto.getConfirmPassword()))
            return ResponseEntity.badRequest().body("Las contrase\u00F1as no coinciden");

        String rawPassword = dto.getPassword();
        String safeEmail = Jsoup.clean(dto.getEmail(), Safelist.none());

        if (!(safeEmail.endsWith("@TMadmin.com") || safeEmail.endsWith("@taskmanager.com")))
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Dominio no admitido");

        if (safeEmail.endsWith("@TMadmin.com") && !rawPassword.equals(globalConstants.getAdminPassword()))
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No puedes utilizar ese dominio");

        UserRequestDTO newUser = new UserRequestDTO(dto.getName(), dto.getEmail(), dto.getPassword());
        userService.createUser(newUser);

        UserResponseDTO createdUser = userService.findUserByEmail(dto.getEmail());
        int newUserId = createdUser.getId();
        GroupRequestDTO group = new GroupRequestDTO("USER_" + newUser.getName(), newUserId);
        groupService.createGroup(group);

        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id, @RequestBody UserRequestDTO dto,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO user = userService.findUserById(id);

        if (user == null)
            return ResponseEntity.notFound().build();

        if (!verifyUserAccess(user, userDetails))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        GroupResponseDTO userGroup = userService.findPersonalGroup(user);
        if (dto.getPassword().isBlank() || dto.getPassword() == null) {
            dto.setPassword(null);
        }
        if (dto.getEmail().isBlank() || dto.getEmail() == null) {
            dto.setEmail(userService.findUserByIdRaw(user.getId()).getEmail());
        }
        if (dto.getName().isBlank() || dto.getName() == null) {
            dto.setName(userService.findUserByIdRaw(user.getId()).getName());
        } else {
            GroupRequestDTO updatedGroup = new GroupRequestDTO("USER_" + dto.getName(), userGroup.getOwnerId());
            groupService.updateGroup(userGroup.getId(), updatedGroup);
        }

        UserResponseDTO success =  userService.updateUser(id, dto);
        return (success != null) ? ResponseEntity.ok(user)
                :  ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{userName}")
    public ResponseEntity<?> deleteUser(@PathVariable String userName, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDTO deleted = userService.findUserByEmail(userName);
        if (!accessManager.checkAdminCredentials(deleted)) { // validation for admin, admin can't be deleted
            if (!verifyUserAccess(deleted, userDetails))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            boolean result = userService.deleteUser(deleted);
            return result ? ResponseEntity.ok("Usuario eliminado")
                    : ResponseEntity.status(403).body("Acci\u00F3n no permitida");
        }
        return ResponseEntity.status(403).body("Acci\u00F3n no permitida");
    }
}
