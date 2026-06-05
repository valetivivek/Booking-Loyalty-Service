package com.vivek.bookingloyalty.booking;

import com.vivek.bookingloyalty.auth.AuthenticatedUser;
import com.vivek.bookingloyalty.booking.dto.BookingResponse;
import com.vivek.bookingloyalty.booking.dto.CreateBookingRequest;
import com.vivek.bookingloyalty.booking.dto.UpdateBookingStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking endpoints. Customer-facing routes live under /api/bookings; admin
 * routes under /api/admin/bookings and are gated by @PreAuthorize.
 */
@RestController
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ----- Customer -----

    @PostMapping("/api/bookings")
    @Operation(summary = "Create a booking for the authenticated customer")
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse created = bookingService.create(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/bookings/me")
    @Operation(summary = "List the authenticated customer's bookings")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.getMyBookings(user.id());
    }

    @GetMapping("/api/bookings/{id}")
    @Operation(summary = "Get a booking by id (owner or admin only)")
    public BookingResponse getById(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long id) {
        return bookingService.getById(id, user.id(), user.role());
    }

    @PatchMapping("/api/bookings/{id}/cancel")
    @Operation(summary = "Cancel one of the authenticated customer's bookings")
    public BookingResponse cancel(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable Long id) {
        return bookingService.cancel(id, user.id(), user.role());
    }

    // ----- Admin -----

    @GetMapping("/api/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: list all bookings")
    public List<BookingResponse> allBookings() {
        return bookingService.getAll();
    }

    @PatchMapping("/api/admin/bookings/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: update a booking's status (COMPLETED awards loyalty points)")
    public BookingResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdateBookingStatusRequest request) {
        return bookingService.updateStatus(id, request.status());
    }
}
