package com.banking.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;


//Reads information stored inside the JWT token
@Component
public class JwtTokenExtractor {

    @Value("${jwt.secret}")
    private String secretString;
    // all services share same secret to validate same tokens

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretString.getBytes());
        // convert secret string to cryptographic key
    }

    public Claims extractAllClaims(String token) {
        // parses the token and returns all stored claims
        //then throws exception if token is invalid or tampered

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
        //email stored as JWT subject
    }

    public Date getExpirationDate(String token) {
        return extractAllClaims(token).getExpiration();
        //checking when will this token expires
    }

    public Long extractCustomerId(String token) {
        // reads customerId claim stored during login
        Object customerId = extractAllClaims(token).get("customerId");

        if (customerId instanceof Integer) {
            return ((Integer) customerId).longValue();
        }
        if (customerId instanceof Long) {
            return (Long) customerId;
        }
        return null;
    }

    public String extractRole(String token) {
        //reads role claim stored during login
        // e.g. "ROLE_CUSTOMER" or "ROLE_ADMIN"
        Object role = extractAllClaims(token).get("role");
        return role != null ? role.toString() : "ROLE_CUSTOMER";
    }
}