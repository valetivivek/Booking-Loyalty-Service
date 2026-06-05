package com.vivek.bookingloyalty.customer;

import com.vivek.bookingloyalty.common.ResourceNotFoundException;
import com.vivek.bookingloyalty.customer.dto.CustomerResponse;
import com.vivek.bookingloyalty.customer.dto.UpdateCustomerRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer profile reads/writes. Lookups by user id are cached in Redis under
 * the "customer" cache (key {@code customer::{userId}}); an update evicts that
 * entry so the next read repopulates it with fresh data.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Cacheable(cacheNames = "customer", key = "#userId")
    @Transactional(readOnly = true)
    public CustomerResponse getByUserId(Long userId) {
        CustomerProfile profile = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        return CustomerResponse.from(profile);
    }

    @CacheEvict(cacheNames = "customer", key = "#userId")
    @Transactional
    public CustomerResponse update(Long userId, UpdateCustomerRequest request) {
        CustomerProfile profile = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        if (request.firstName() != null) {
            profile.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            profile.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            profile.setPhone(request.phone());
        }

        customerRepository.save(profile);
        return CustomerResponse.from(profile);
    }

    /** Admin lookup by customer-profile id. Not cached (admin/back-office path). */
    @Transactional(readOnly = true)
    public CustomerResponse getById(Long customerId) {
        CustomerProfile profile = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        return CustomerResponse.from(profile);
    }
}
