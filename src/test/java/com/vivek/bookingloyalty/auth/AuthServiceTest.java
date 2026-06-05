package com.vivek.bookingloyalty.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vivek.bookingloyalty.auth.dto.AuthResponse;
import com.vivek.bookingloyalty.auth.dto.LoginRequest;
import com.vivek.bookingloyalty.auth.dto.RegisterRequest;
import com.vivek.bookingloyalty.common.BadRequestException;
import com.vivek.bookingloyalty.customer.CustomerProfile;
import com.vivek.bookingloyalty.customer.CustomerRepository;
import com.vivek.bookingloyalty.loyalty.LoyaltyAccount;
import com.vivek.bookingloyalty.loyalty.LoyaltyAccountRepository;
import com.vivek.bookingloyalty.user.Role;
import com.vivek.bookingloyalty.user.User;
import com.vivek.bookingloyalty.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private LoyaltyAccountRepository loyaltyAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    @Test
    void register_createsUserProfileAndLoyaltyAccount_andReturnsToken() {
        RegisterRequest request = new RegisterRequest(
                "alice@example.com", "password123", "Alice", "Smith", "555-1000");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        verify(customerRepository).save(any(CustomerProfile.class));
        verify(loyaltyAccountRepository).save(any(LoyaltyAccount.class));
    }

    @Test
    void register_withExistingEmail_throwsAndPersistsNothing() {
        RegisterRequest request = new RegisterRequest(
                "taken@example.com", "password123", "Bob", "Jones", null);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(any());
        verify(customerRepository, never()).save(any());
        verify(loyaltyAccountRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        User user = new User("alice@example.com", "hashed", Role.CUSTOMER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void login_withWrongPassword_throwsBadCredentials() {
        User user = new User("alice@example.com", "hashed", Role.CUSTOMER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
