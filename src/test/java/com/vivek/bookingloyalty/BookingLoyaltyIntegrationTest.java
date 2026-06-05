package com.vivek.bookingloyalty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.bookingloyalty.auth.JwtService;
import com.vivek.bookingloyalty.auth.dto.AuthResponse;
import com.vivek.bookingloyalty.booking.dto.BookingResponse;
import com.vivek.bookingloyalty.user.Role;
import com.vivek.bookingloyalty.user.User;
import com.vivek.bookingloyalty.user.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingLoyaltyIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String registerCustomer(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123","firstName":"Test","lastName":"User","phone":"555"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    private String createAdminToken(String email) {
        User admin = new User(email, passwordEncoder.encode("password123"), Role.ADMIN);
        userRepository.save(admin);
        return jwtService.generateToken(admin);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void unauthenticatedRequest_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotAccessAdminEndpoint_butAdminCan() throws Exception {
        String customerToken = registerCustomer("rbac-customer@example.com");
        String adminToken = createAdminToken("rbac-admin@example.com");

        mockMvc.perform(get("/api/admin/bookings").header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/bookings").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotAccessAnotherCustomersBooking() throws Exception {
        String tokenA = registerCustomer("owner@example.com");
        String tokenB = registerCustomer("intruder@example.com");

        long bookingId = createBooking(tokenA);

        mockMvc.perform(get("/api/bookings/" + bookingId).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bookings/" + bookingId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeBookingFlow_awardsLoyaltyPoints_andBlocksDoubleAward() throws Exception {
        String customerToken = registerCustomer("loyalty@example.com");
        String adminToken = createAdminToken("loyalty-admin@example.com");

        long bookingId = createBooking(customerToken); // DELUXE, 2 nights => 400

        // Admin completes the booking -> triggers the transactional award.
        mockMvc.perform(patch("/api/admin/bookings/" + bookingId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/loyalty/me").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").value(400))
                .andExpect(jsonPath("$.tier").value("BRONZE"));

        mockMvc.perform(get("/api/loyalty/me/transactions").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionType").value("EARNED"))
                .andExpect(jsonPath("$[0].points").value(400));

        // Completing again must be rejected (no duplicate points).
        mockMvc.perform(patch("/api/admin/bookings/" + bookingId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest());
    }

    private long createBooking(String token) throws Exception {
        String body = """
                {"roomType":"DELUXE","checkInDate":"%s","checkOutDate":"%s"}
                """.formatted(LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));
        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        BookingResponse booking =
                objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);
        assertThat(booking.totalAmount()).isEqualByComparingTo("400");
        return booking.id();
    }
}
