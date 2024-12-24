package ru.service.event.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.service.event.config.security.JwtProperties;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey accessKey;

    @Autowired
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.accessKey = Keys.hmacShaKeyFor(jwtProperties.getAccessKey().getBytes());
    }

    public void validateToken(String token) {
        log.info("Validating token");
        Claims claims = extractAllClaims(token);
        if (!claims.getIssuedAt().before(new Date())) {
            throw new JwtException("Token expired");
        }
    }

    public Claims extractAllClaims(String token) {
        log.info("Verifying with key and extracting claims from token");
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new JwtException(e.getMessage());
        }
        return claims;
    }
}
