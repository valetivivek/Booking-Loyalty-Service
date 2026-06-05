package com.vivek.bookingloyalty.auth.dto;

import com.vivek.bookingloyalty.user.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        Long userId,
        String email,
        Role role) {
}
