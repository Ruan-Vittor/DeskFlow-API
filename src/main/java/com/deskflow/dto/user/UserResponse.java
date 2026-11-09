package com.deskflow.dto.user;

import com.deskflow.entity.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        Instant createdAt
) {
}
