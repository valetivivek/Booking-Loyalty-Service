package com.vivek.bookingloyalty.loyalty.dto;

import com.vivek.bookingloyalty.loyalty.LoyaltyAccount;
import com.vivek.bookingloyalty.loyalty.LoyaltyTier;
import java.time.Instant;

public record LoyaltyResponse(
        Long id,
        Long customerId,
        long pointsBalance,
        LoyaltyTier tier,
        Instant createdAt,
        Instant updatedAt) {

    public static LoyaltyResponse from(LoyaltyAccount account) {
        return new LoyaltyResponse(
                account.getId(),
                account.getCustomer().getId(),
                account.getPointsBalance(),
                account.getTier(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
