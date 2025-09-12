package com.example.learnverse.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final Key key;
    private final long accessExpMinutes;
    private final String issuer;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-exp-min}") long accessExpMinutes,
            @Value("${jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpMinutes = accessExpMinutes;
        this.issuer = issuer;
    }

    // Latest non-deprecated token generation
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessExpMinutes * 60);

        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(claims)
                .signWith(key)
                .compact();
    }

    // Latest non-deprecated token parsing
    public Jws<Claims> validateAndParse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
    }

    public long getAccessExpSeconds() {
        return accessExpMinutes * 60;
    }
}