package com.vivek.bookingloyalty.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vivek.bookingloyalty.booking.dto.BookingResponse;
import com.vivek.bookingloyalty.booking.dto.CreateBookingRequest;
import com.vivek.bookingloyalty.common.BadRequestException;
import com.vivek.bookingloyalty.common.UnauthorizedException;
import com.vivek.bookingloyalty.customer.CustomerProfile;
import com.vivek.bookingloyalty.customer.CustomerRepository;
import com.vivek.bookingloyalty.loyalty.LoyaltyService;
import com.vivek.bookingloyalty.user.Role;
import com.vivek.bookingloyalty.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private LoyaltyService loyaltyService;

    @InjectMocks private BookingService bookingService;

    private CustomerProfile customer(long userId, long customerId) {
        User user = new User("u" + userId + "@example.com", "hash", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", userId);
        CustomerProfile profile = new CustomerProfile(user, "First", "Last", null);
        ReflectionTestUtils.setField(profile, "id", customerId);
        return profile;
    }

    private Booking booking(long bookingId, CustomerProfile owner, BookingStatus status, String total) {
        Booking b = new Booking(owner, "DELUXE",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), new BigDecimal(total));
        ReflectionTestUtils.setField(b, "id", bookingId);
        b.setStatus(status);
        return b;
    }

    @Test
    void create_computesTotalFromRoomTypeAndNights() {
        CustomerProfile profile = customer(1L, 10L);
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest request = new CreateBookingRequest(
                "DELUXE", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)); // 2 nights * 200

        BookingResponse response = bookingService.create(1L, request);

        assertThat(response.totalAmount()).isEqualByComparingTo("400");
        assertThat(response.status()).isEqualTo(BookingStatus.CREATED);
        assertThat(response.roomType()).isEqualTo("DELUXE");
    }

    @Test
    void create_withCheckoutBeforeCheckin_throws() {
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(1L, 10L)));
        CreateBookingRequest request = new CreateBookingRequest(
                "DELUXE", LocalDate.now().plusDays(3), LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> bookingService.create(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cancel_setsStatusToCancelled() {
        CustomerProfile profile = customer(1L, 10L);
        Booking b = booking(5L, profile, BookingStatus.CREATED, "400");
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.cancel(5L, 1L, Role.CUSTOMER);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_completedBooking_throws() {
        CustomerProfile profile = customer(1L, 10L);
        Booking b = booking(5L, profile, BookingStatus.COMPLETED, "400");
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> bookingService.cancel(5L, 1L, Role.CUSTOMER))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void complete_awardsLoyaltyPointsAndSetsCompleted() {
        CustomerProfile profile = customer(1L, 10L);
        Booking b = booking(7L, profile, BookingStatus.CONFIRMED, "1500");
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loyaltyService.awardForCompletedBooking(b)).thenReturn(1500L);

        BookingResponse response = bookingService.complete(7L);

        assertThat(response.status()).isEqualTo(BookingStatus.COMPLETED);
        verify(loyaltyService).awardForCompletedBooking(b);
    }

    @Test
    void complete_cancelledBooking_throwsAndDoesNotAwardPoints() {
        CustomerProfile profile = customer(1L, 10L);
        Booking b = booking(7L, profile, BookingStatus.CANCELLED, "1500");
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> bookingService.complete(7L))
                .isInstanceOf(BadRequestException.class);

        verify(loyaltyService, never()).awardForCompletedBooking(any());
    }

    @Test
    void getById_otherCustomersBooking_throwsUnauthorized() {
        CustomerProfile owner = customer(2L, 20L); // owned by user 2
        Booking b = booking(9L, owner, BookingStatus.CREATED, "400");
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(b));

        // user 1 (a customer) tries to read user 2's booking
        assertThatThrownBy(() -> bookingService.getById(9L, 1L, Role.CUSTOMER))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getById_asAdmin_allowsAnyBooking() {
        CustomerProfile owner = customer(2L, 20L);
        Booking b = booking(9L, owner, BookingStatus.CREATED, "400");
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(b));

        BookingResponse response = bookingService.getById(9L, 999L, Role.ADMIN);

        assertThat(response.id()).isEqualTo(9L);
    }
}
