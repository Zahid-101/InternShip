package com.driveease.service;

import com.driveease.dto.UserResponse;
import com.driveease.model.Role;
import com.driveease.model.User;
import com.driveease.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns only active SUPPORT users. Admins are never visible in this list.
     */
    public List<UserResponse> getAllSupportUsers() {
        return userRepository.findByRoleAndIsActiveTrue(Role.SUPPORT).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Soft-deletes (deactivates) a user by setting isActive = false.
     * Only SUPPORT users can be deactivated. If the target is an ADMIN,
     * a SecurityException is thrown (403 Forbidden).
     */
    public UserResponse deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new SecurityException("Cannot deactivate an Admin user");
        }

        user.setActive(false);
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .contactNumber(user.getContactNumber())
                .isActive(user.isActive())
                .build();
    }
}
