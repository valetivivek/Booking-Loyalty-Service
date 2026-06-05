package com.vivek.bookingloyalty.customer;

import com.vivek.bookingloyalty.auth.AuthenticatedUser;
import com.vivek.bookingloyalty.customer.dto.CustomerResponse;
import com.vivek.bookingloyalty.customer.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/api/customers/me")
    @Operation(summary = "Get the authenticated customer's profile")
    public CustomerResponse getMyProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return customerService.getByUserId(user.id());
    }

    @PutMapping("/api/customers/me")
    @Operation(summary = "Update the authenticated customer's profile")
    public CustomerResponse updateMyProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.update(user.id(), request);
    }

    @GetMapping("/api/admin/customers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: get any customer profile by id")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        return customerService.getById(id);
    }
}
