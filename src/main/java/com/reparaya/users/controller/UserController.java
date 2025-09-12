package com.reparaya.users.controller;

import com.reparaya.users.dto.*;
import com.reparaya.users.entity.User;
import com.reparaya.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    public static final String UPDATE_USER_ERROR = "Error al actualizar el usuario. ";
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@RequestBody @Valid RegisterRequest request) {
        try {
            RegisterResponse response = userService.registerUser(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(RegisterResponse.builder().message(e.getMessage()).build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody @Valid LoginRequest request) {
        try {
            LoginResponse response = userService.authenticateUser(request);

            if (response.getToken() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse(null, null, "Error interno del servidor"));
        }
    }

    @PatchMapping("/{userId}/reset-password")
    public ResponseEntity<String> resetPassword(@PathVariable Long userId, @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(userId, request.getNewPassword()));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<String> updateUser(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserRequest request) {
        try {
            return ResponseEntity.ok(userService.updateUserPartially(userId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UPDATE_USER_ERROR + e.getMessage());
        }
    }

    @PatchMapping("/{userId}/active")
    public ResponseEntity<Void> changeActiveUser(
            @PathVariable Long userId,
            @RequestBody @Valid UserChangeActiveRequest request) {
        userService.changeUserIsActive(userId, request);
        return ResponseEntity.ok().build();
    }

}
