package com.vivek.bookingloyalty.loyalty;

import com.vivek.bookingloyalty.booking.Booking;
import com.vivek.bookingloyalty.common.BadRequestException;
import com.vivek.bookingloyalty.common.ResourceNotFoundException;
import com.vivek.bookingloyalty.customer.CustomerProfile;
import com.vivek.bookingloyalty.customer.CustomerRepository;
import com.vivek.bookingloyalty.loyalty.dto.LoyaltyResponse;
import com.vivek.bookingloyalty.loyalty.dto.LoyaltyTransactionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loyalty reads plus the points-award step of the booking-completion workflow.
 * The award method has no @Transactional of its own: it is always called from
 * {@code BookingService.completeBooking}, joining that single transaction so
 * points and booking status commit or roll back together.
 */
@Service
public class LoyaltyService {

    /** Earn rate: 1 point per whole dollar of the booking total. */
    private static final BigDecimal POINTS_PER_CURRENCY_UNIT = BigDecimal.ONE;

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final CustomerRepository customerRepository;

    public LoyaltyService(LoyaltyAccountRepository loyaltyAccountRepository,
                          LoyaltyTransactionRepository loyaltyTransactionRepository,
                          CustomerRepository customerRepository) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public LoyaltyResponse getByUserId(Long userId) {
        return LoyaltyResponse.from(requireAccountForUser(userId));
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getTransactionsByUserId(Long userId) {
        LoyaltyAccount account = requireAccountForUser(userId);
        return loyaltyTransactionRepository
                .findByLoyaltyAccountIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(LoyaltyTransactionResponse::from)
                .toList();
    }

    /**
     * Awards earn-points for a completed booking and records a ledger entry.
     * Guards against double-awarding the same booking. Inherits the caller's
     * transaction (propagation REQUIRED).
     *
     * @return the number of points awarded
     */
    public long awardForCompletedBooking(Booking booking) {
        boolean alreadyAwarded = loyaltyTransactionRepository
                .existsByBookingIdAndTransactionType(booking.getId(), TransactionType.EARNED);
        if (alreadyAwarded) {
            throw new BadRequestException("Loyalty points already awarded for booking " + booking.getId());
        }

        LoyaltyAccount account = loyaltyAccountRepository
                .findByCustomerId(booking.getCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found for customer"));

        long points = calculatePoints(booking.getTotalAmount());
        account.addPoints(points);
        loyaltyAccountRepository.save(account);

        loyaltyTransactionRepository.save(new LoyaltyTransaction(
                account,
                booking,
                points,
                TransactionType.EARNED,
                "Points earned for completed booking " + booking.getId()));

        return points;
    }

    static long calculatePoints(BigDecimal totalAmount) {
        return totalAmount
                .multiply(POINTS_PER_CURRENCY_UNIT)
                .setScale(0, RoundingMode.FLOOR)
                .longValueExact();
    }

    private LoyaltyAccount requireAccountForUser(Long userId) {
        CustomerProfile profile = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        return loyaltyAccountRepository.findByCustomerId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));
    }
}
