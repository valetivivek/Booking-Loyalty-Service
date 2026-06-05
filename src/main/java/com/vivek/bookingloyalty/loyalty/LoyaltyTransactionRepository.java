package com.vivek.bookingloyalty.loyalty;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    List<LoyaltyTransaction> findByLoyaltyAccountIdOrderByCreatedAtDesc(Long loyaltyAccountId);

    /** Guard used to prevent awarding points twice for the same completed booking. */
    boolean existsByBookingIdAndTransactionType(Long bookingId, TransactionType transactionType);
}
