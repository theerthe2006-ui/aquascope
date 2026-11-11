package com.aquascope.controller;

import com.aquascope.dto.*;
import com.aquascope.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & User Management", description = "Endpoints for JWT authentication, registration and profile access")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Authentication successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        UserDto profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.ok(profile, "User profile retrieved successfully"));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Create a new user account (Administrator only)")
    public ResponseEntity<ApiResponse<UserDto>> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        UserDto created = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "User registered successfully"));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "List all registered users (Administrator only)")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = authService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(users, "Users retrieved successfully"));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Activate or deactivate a user account (Administrator only)")
    public ResponseEntity<ApiResponse<UserDto>> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        UserDto updated = authService.toggleUserActivation(id, active);
        return ResponseEntity.ok(ApiResponse.ok(updated, "User status updated successfully"));
    }
}
