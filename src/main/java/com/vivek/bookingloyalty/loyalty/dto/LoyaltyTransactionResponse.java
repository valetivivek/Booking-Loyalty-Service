package com.vivek.bookingloyalty.loyalty.dto;

import com.vivek.bookingloyalty.loyalty.LoyaltyTransaction;
import com.vivek.bookingloyalty.loyalty.TransactionType;
import java.time.Instant;

public record LoyaltyTransactionResponse(
        Long id,
        Long bookingId,
        long points,
        TransactionType transactionType,
        String description,
        Instant createdAt) {

    public static LoyaltyTransactionResponse from(LoyaltyTransaction tx) {
        return new LoyaltyTransactionResponse(
                tx.getId(),
                tx.getBooking() != null ? tx.getBooking().getId() : null,
                tx.getPoints(),
                tx.getTransactionType(),
                tx.getDescription(),
                tx.getCreatedAt());
    }
}
