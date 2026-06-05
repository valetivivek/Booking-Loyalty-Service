package com.vivek.bookingloyalty.loyalty;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    Optional<LoyaltyAccount> findByCustomerId(Long customerId);
}
