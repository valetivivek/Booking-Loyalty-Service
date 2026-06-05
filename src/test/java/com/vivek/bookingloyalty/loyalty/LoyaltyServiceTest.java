package com.vivek.bookingloyalty.loyalty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vivek.bookingloyalty.booking.Booking;
import com.vivek.bookingloyalty.booking.BookingStatus;
import com.vivek.bookingloyalty.common.BadRequestException;
import com.vivek.bookingloyalty.customer.CustomerProfile;
import com.vivek.bookingloyalty.customer.CustomerRepository;
import com.vivek.bookingloyalty.user.Role;
import com.vivek.bookingloyalty.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock private LoyaltyAccountRepository loyaltyAccountRepository;
    @Mock private LoyaltyTransactionRepository loyaltyTransactionRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private LoyaltyService loyaltyService;

    private Booking bookingWithTotal(long bookingId, long customerId, String total) {
        User user = new User("u@example.com", "hash", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 1L);
        CustomerProfile profile = new CustomerProfile(user, "F", "L", null);
        ReflectionTestUtils.setField(profile, "id", customerId);
        Booking b = new Booking(profile, "SUITE",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), new BigDecimal(total));
        ReflectionTestUtils.setField(b, "id", bookingId);
        b.setStatus(BookingStatus.COMPLETED);
        return b;
    }

    @Test
    void award_addsPointsRecalculatesTier_andWritesLedgerEntry() {
        Booking booking = bookingWithTotal(5L, 20L, "1200");
        CustomerProfile profile = booking.getCustomer();
        LoyaltyAccount account = new LoyaltyAccount(profile);

        when(loyaltyTransactionRepository.existsByBookingIdAndTransactionType(5L, TransactionType.EARNED))
                .thenReturn(false);
        when(loyaltyAccountRepository.findByCustomerId(20L)).thenReturn(Optional.of(account));
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        long awarded = loyaltyService.awardForCompletedBooking(booking);

        assertThat(awarded).isEqualTo(1200L);
        assertThat(account.getPointsBalance()).isEqualTo(1200L);
        assertThat(account.getTier()).isEqualTo(LoyaltyTier.SILVER); // 1000-4999

        ArgumentCaptor<LoyaltyTransaction> captor = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(loyaltyTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getPoints()).isEqualTo(1200L);
        assertThat(captor.getValue().getTransactionType()).isEqualTo(TransactionType.EARNED);
    }

    @Test
    void award_whenAlreadyAwarded_throwsAndDoesNotTouchAccount() {
        Booking booking = bookingWithTotal(5L, 20L, "1200");
        when(loyaltyTransactionRepository.existsByBookingIdAndTransactionType(5L, TransactionType.EARNED))
                .thenReturn(true);

        assertThatThrownBy(() -> loyaltyService.awardForCompletedBooking(booking))
                .isInstanceOf(BadRequestException.class);

        verify(loyaltyAccountRepository, never()).findByCustomerId(eq(20L));
        verify(loyaltyAccountRepository, never()).save(any());
        verify(loyaltyTransactionRepository, never()).save(any());
    }

    @Test
    void calculatePoints_floorsToWholePoints() {
        assertThat(LoyaltyService.calculatePoints(new BigDecimal("199.99"))).isEqualTo(199L);
        assertThat(LoyaltyService.calculatePoints(new BigDecimal("5000.00"))).isEqualTo(5000L);
    }

    @Test
    void tier_thresholds() {
        assertThat(LoyaltyTier.forPoints(0)).isEqualTo(LoyaltyTier.BRONZE);
        assertThat(LoyaltyTier.forPoints(999)).isEqualTo(LoyaltyTier.BRONZE);
        assertThat(LoyaltyTier.forPoints(1000)).isEqualTo(LoyaltyTier.SILVER);
        assertThat(LoyaltyTier.forPoints(4999)).isEqualTo(LoyaltyTier.SILVER);
        assertThat(LoyaltyTier.forPoints(5000)).isEqualTo(LoyaltyTier.GOLD);
    }
}
