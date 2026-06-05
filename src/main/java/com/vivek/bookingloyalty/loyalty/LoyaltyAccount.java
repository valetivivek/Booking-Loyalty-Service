package com.vivek.bookingloyalty.loyalty;

import com.vivek.bookingloyalty.customer.CustomerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One loyalty account per customer. The tier is always kept in sync with the
 * points balance via {@link #addPoints(long)}.
 */
@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private CustomerProfile customer;

    @Column(name = "points_balance", nullable = false)
    private long pointsBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyTier tier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LoyaltyAccount() {
        // JPA
    }

    public LoyaltyAccount(CustomerProfile customer) {
        this.customer = customer;
        this.pointsBalance = 0;
        this.tier = LoyaltyTier.BRONZE;
    }

    /**
     * Adjust the balance by {@code delta} (positive earns, negative redeems)
     * and recompute the tier. Balance never goes below zero.
     */
    public void addPoints(long delta) {
        long updated = this.pointsBalance + delta;
        this.pointsBalance = Math.max(0, updated);
        this.tier = LoyaltyTier.forPoints(this.pointsBalance);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerProfile getCustomer() {
        return customer;
    }

    public long getPointsBalance() {
        return pointsBalance;
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
