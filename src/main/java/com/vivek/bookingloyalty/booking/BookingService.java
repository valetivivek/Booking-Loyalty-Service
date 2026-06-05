package com.vivek.bookingloyalty.booking;

import com.vivek.bookingloyalty.booking.dto.BookingResponse;
import com.vivek.bookingloyalty.booking.dto.CreateBookingRequest;
import com.vivek.bookingloyalty.common.BadRequestException;
import com.vivek.bookingloyalty.common.ResourceNotFoundException;
import com.vivek.bookingloyalty.common.UnauthorizedException;
import com.vivek.bookingloyalty.customer.CustomerProfile;
import com.vivek.bookingloyalty.customer.CustomerRepository;
import com.vivek.bookingloyalty.loyalty.LoyaltyService;
import com.vivek.bookingloyalty.user.Role;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking lifecycle: create, read (with ownership checks), cancel, and the
 * admin status transitions — including the COMPLETED transition that awards
 * loyalty points in the same transaction.
 */
@Service
public class BookingService {

    /** Nightly rates by room type (USD). Total = rate * nights. */
    private static final Map<String, BigDecimal> ROOM_RATES = Map.of(
            "STANDARD", new BigDecimal("100"),
            "DELUXE", new BigDecimal("200"),
            "SUITE", new BigDecimal("400"));

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyService loyaltyService;

    public BookingService(BookingRepository bookingRepository,
                          CustomerRepository customerRepository,
                          LoyaltyService loyaltyService) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.loyaltyService = loyaltyService;
    }

    @Transactional
    public BookingResponse create(Long userId, CreateBookingRequest request) {
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new BadRequestException("checkOutDate must be after checkInDate");
        }

        BigDecimal nightlyRate = ROOM_RATES.get(request.roomType().toUpperCase());
        if (nightlyRate == null) {
            throw new BadRequestException("Unknown room type: " + request.roomType()
                    + " (expected one of " + ROOM_RATES.keySet() + ")");
        }

        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal totalAmount = nightlyRate.multiply(BigDecimal.valueOf(nights));

        Booking booking = new Booking(
                customer,
                request.roomType().toUpperCase(),
                request.checkInDate(),
                request.checkOutDate(),
                totalAmount);
        bookingRepository.save(booking);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long bookingId, Long userId, Role role) {
        Booking booking = requireBooking(bookingId);
        assertCanAccess(booking, userId, role);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long userId) {
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getAll(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(BookingResponse::from);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, Long userId, Role role) {
        Booking booking = requireBooking(bookingId);
        assertCanAccess(booking, userId, role);

        if (booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel a booking that is " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return BookingResponse.from(booking);
    }

    /**
     * Admin status transition. The COMPLETED transition runs the full
     * loyalty-award workflow inside this single transaction.
     */
    @Transactional
    public BookingResponse updateStatus(Long bookingId, BookingStatus newStatus) {
        if (newStatus == BookingStatus.COMPLETED) {
            return complete(bookingId);
        }

        Booking booking = requireBooking(bookingId);
        booking.setStatus(newStatus);
        bookingRepository.save(booking);
        return BookingResponse.from(booking);
    }

    /**
     * The core transactional workflow. Loads the booking, validates it, flips it
     * to COMPLETED, and awards loyalty points + a ledger entry. Because it is a
     * single {@code @Transactional} unit, any failure rolls back BOTH the status
     * change and the points.
     */
    @Transactional
    public BookingResponse complete(Long bookingId) {
        Booking booking = requireBooking(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot complete a cancelled booking");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Booking is already completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        // Joins this transaction; throws if points were already awarded.
        loyaltyService.awardForCompletedBooking(booking);

        return BookingResponse.from(booking);
    }

    private Booking requireBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    /** Owners may access their own bookings; admins may access any. */
    private void assertCanAccess(Booking booking, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return;
        }
        if (!booking.getCustomer().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have access to this booking");
        }
    }
}
