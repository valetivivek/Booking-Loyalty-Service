package com.vivek.bookingloyalty.auth;

import com.vivek.bookingloyalty.user.Role;
import com.vivek.bookingloyalty.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Creates and verifies signed JWTs (HS256). The token carries the user id and
 * role as claims, which is all the JWT filter needs to build a principal.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Parses and verifies the token, returning a principal built from its claims.
     * Throws a JJWT exception if the token is invalid or expired.
     */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = claims.get("uid", Long.class);
        String email = claims.getSubject();
        Role role = Role.valueOf(claims.get("role", String.class));
        return new AuthenticatedUser(userId, email, role);
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
