package com.aquascope.dto;

import com.aquascope.entity.Role;
import java.time.Instant;

public record UserDto(
    Long id,
    String username,
    String email,
    String fullName,
    Role role,
    String institution,
    boolean active,
    Instant createdAt
) {}
