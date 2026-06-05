package com.vivek.bookingloyalty.auth;

import com.vivek.bookingloyalty.user.Role;

/**
 * Lightweight principal placed in the Spring Security context by
 * {@link JwtAuthenticationFilter}. Built entirely from JWT claims, so no
 * database lookup is needed to authenticate a request.
 *
 * <p>Controllers can inject it with {@code @AuthenticationPrincipal AuthenticatedUser}.
 */
public record AuthenticatedUser(Long id, String email, Role role) {
}
