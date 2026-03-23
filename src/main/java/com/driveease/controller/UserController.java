package com.driveease.controller;

import com.driveease.dto.UserResponse;
import com.driveease.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lists only active SUPPORT users. Admins are never returned.
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllSupportUsers());
    }

    /**
     * Soft-deletes a user (sets isActive = false).
     * Returns 403 if the target is an Admin.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            UserResponse response = userService.deactivateUser(id);
            return ResponseEntity.ok(Map.of(
                    "message", "User deactivated successfully",
                    "user", response));
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
