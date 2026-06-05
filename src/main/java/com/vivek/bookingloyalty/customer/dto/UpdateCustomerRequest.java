package com.vivek.bookingloyalty.customer.dto;

import jakarta.validation.constraints.Size;

/**
 * Editable profile fields. All optional; only non-null fields are applied.
 */
public record UpdateCustomerRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) String phone) {
}
