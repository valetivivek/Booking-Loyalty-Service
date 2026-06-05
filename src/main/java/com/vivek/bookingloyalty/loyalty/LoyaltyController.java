package com.vivek.bookingloyalty.loyalty;

import com.vivek.bookingloyalty.auth.AuthenticatedUser;
import com.vivek.bookingloyalty.loyalty.dto.LoyaltyResponse;
import com.vivek.bookingloyalty.loyalty.dto.LoyaltyTransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loyalty")
@Tag(name = "Loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated customer's loyalty account")
    public LoyaltyResponse getMyLoyalty(@AuthenticationPrincipal AuthenticatedUser user) {
        return loyaltyService.getByUserId(user.id());
    }

    @GetMapping("/me/transactions")
    @Operation(summary = "List the authenticated customer's loyalty transactions")
    public List<LoyaltyTransactionResponse> getMyTransactions(@AuthenticationPrincipal AuthenticatedUser user) {
        return loyaltyService.getTransactionsByUserId(user.id());
    }
}
