package com.vivek.bookingloyalty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BookingLoyaltyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingLoyaltyApplication.class, args);
    }
}
