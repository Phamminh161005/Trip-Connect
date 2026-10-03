package com.tripconnect.backend.security;

import com.tripconnect.backend.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {

    private static final String ISSUER = "tripconnect";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    private SecretKey signingKey;
    private JwtParser parser;

    @PostConstruct
    void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET phải dài ít nhất 32 byte");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
        parser = Jwts.parser().verifyWith(signingKey).requireIssuer(ISSUER).build();
    }

    public String generateAccessToken(Long userId, String email, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(email)
                .claim("userId", userId)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    /** Trả về claims nếu token hợp lệ (đúng chữ ký, đúng issuer, chưa hết hạn). */
    public Optional<Claims> parseValidToken(String token) {
        try {
            return Optional.of(parser.parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
