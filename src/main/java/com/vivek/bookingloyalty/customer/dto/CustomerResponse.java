package com.vivek.bookingloyalty.customer.dto;

import com.vivek.bookingloyalty.customer.CustomerProfile;
import java.time.Instant;

/**
 * Read model for a customer profile. Kept as a separate DTO so we never
 * serialize the JPA entity (avoids leaking lazy relations / password data).
 */
public record CustomerResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String phone,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerResponse from(CustomerProfile profile) {
        return new CustomerResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
