package com.vivek.bookingloyalty.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Booking creation input. The total amount is computed server-side from the
 * room type and number of nights, so the client never sets the price.
 */
public record CreateBookingRequest(
        @NotBlank String roomType,
        @NotNull @Future LocalDate checkInDate,
        @NotNull @Future LocalDate checkOutDate) {
}
