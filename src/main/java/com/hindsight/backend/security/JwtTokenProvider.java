package com.hindsight.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration:2592000000}") long expirationMs) { // default 30 days
        String resolvedSecret = StringUtils.hasText(secret)
                ? secret
                : UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        this.key = Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String id, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", id);
        userData.put("role", role);

        return Jwts.builder()
                .claim("userData", userData)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getUserDataFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Compatibility: check nested userData claim
        Object userDataObj = claims.get("userData");
        if (userDataObj instanceof Map) {
            Map<String, Object> userDataMap = (Map<String, Object>) userDataObj;
            Map<String, String> result = new HashMap<>();
            result.put("id", String.valueOf(userDataMap.get("id")));
            result.put("role", String.valueOf(userDataMap.get("role")));
            return result;
        }

        // Fallback to top-level claims
        Map<String, String> result = new HashMap<>();
        if (claims.get("id") != null) {
            result.put("id", String.valueOf(claims.get("id")));
        }
        if (claims.get("role") != null) {
            result.put("role", String.valueOf(claims.get("role")));
        }
        return result;
    }
}
