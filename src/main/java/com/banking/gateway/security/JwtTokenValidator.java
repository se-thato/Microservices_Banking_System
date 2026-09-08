package com.banking.gateway.security;

import org.springframework.stereotype.Component;
import java.util.Date;


// Validates whether a JWT token is genuine and not expired

@Component
public class JwtTokenValidator {

    private final JwtTokenExtractor jwtTokenExtractor;

    public JwtTokenValidator(JwtTokenExtractor jwtTokenExtractor) {
        this.jwtTokenExtractor = jwtTokenExtractor;
    }

    public boolean isTokenValid(String token) {
        //this will return true if token is genuine and not expired

        try {
            return !isTokenExpired(token);
            // valid = not expired AND signature matches
            // signature check happens inside extractAllClaims()


        } catch (Exception e) {
            return false;
            //any exception that will return means token is invalid
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = jwtTokenExtractor.getExpirationDate(token);
        return expiration.before(new Date());
        // true if token expired
        // false if token still valid
    }
}