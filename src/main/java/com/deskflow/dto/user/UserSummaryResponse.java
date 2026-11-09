package com.deskflow.dto.user;

import com.deskflow.entity.Role;

public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        Role role
) {
}
