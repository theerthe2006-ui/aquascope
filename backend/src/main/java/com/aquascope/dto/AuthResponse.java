package com.aquascope.dto;

import com.aquascope.entity.Role;

public record AuthResponse(
    String token,
    String type,
    Long id,
    String username,
    String email,
    String fullName,
    Role role,
    String institution
) {}
