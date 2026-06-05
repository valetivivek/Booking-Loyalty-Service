package com.vivek.bookingloyalty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@SpringBootApplication
@EnableCaching
// Serialize Page responses via a stable DTO envelope ({ content, page })
// instead of the version-unstable PageImpl shape.
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class BookingLoyaltyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingLoyaltyApplication.class, args);
    }
}
