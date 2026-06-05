package com.vivek.bookingloyalty.loyalty.dto;

import jakarta.validation.constraints.Positive;

/** Request to redeem a number of loyalty points from the caller's account. */
public record RedeemRequest(@Positive long points) {
}
