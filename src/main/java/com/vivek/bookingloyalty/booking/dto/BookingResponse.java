package com.vivek.bookingloyalty.booking.dto;

import com.vivek.bookingloyalty.booking.Booking;
import com.vivek.bookingloyalty.booking.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long customerId,
        String roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BookingStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCustomer().getId(),
                booking.getRoomType(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                booking.getUpdatedAt());
    }
}
