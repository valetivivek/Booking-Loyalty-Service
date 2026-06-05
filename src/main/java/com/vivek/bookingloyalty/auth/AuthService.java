package com.vivek.bookingloyalty.auth;

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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles registration and login. Registration is transactional because it
 * provisions three rows together (user + profile + loyalty account); if any
 * step fails, none are persisted.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       LoyaltyAccountRepository loyaltyAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.CUSTOMER);
        userRepository.save(user);

        CustomerProfile profile = new CustomerProfile(
                user, request.firstName(), request.lastName(), request.phone());
        customerRepository.save(profile);

        loyaltyAccountRepository.save(new LoyaltyAccount(profile));

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getEmail(),
                user.getRole());
    }
}
