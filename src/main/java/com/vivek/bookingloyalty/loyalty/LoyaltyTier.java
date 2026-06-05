package com.vivek.bookingloyalty.loyalty;

/**
 * Loyalty tiers, derived purely from the points balance.
 * BRONZE: 0-999, SILVER: 1000-4999, GOLD: 5000+.
 */
public enum LoyaltyTier {
    BRONZE,
    SILVER,
    GOLD;

    public static LoyaltyTier forPoints(long points) {
        if (points >= 5000) {
            return GOLD;
        }
        if (points >= 1000) {
            return SILVER;
        }
        return BRONZE;
    }
}
