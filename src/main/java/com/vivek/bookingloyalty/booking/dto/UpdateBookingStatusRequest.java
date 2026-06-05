package com.vivek.bookingloyalty.booking.dto;

import com.vivek.bookingloyalty.booking.BookingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Admin payload for changing a booking's status. Setting it to COMPLETED
 * triggers the transactional loyalty-award workflow.
 */
public record UpdateBookingStatusRequest(@NotNull BookingStatus status) {
}
