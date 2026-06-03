package com.thato.customer_api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component // this tells Spring to manage this class as a bean
public class JwtTokenGenerator {

    @Value("${jwt.secret}")
    // this reads the value of jwt.secret from application.properties
    private String secretString;

    @Value("${jwt.expiration}")
    // this reads the jwt.expiration value from application.properties
    private long expirationTime;

    private SecretKey getSigningKey() {
        // converts the plain secret string into a proper cryptographic key
        return Keys.hmacShaKeyFor(secretString.getBytes());
    }

    public String generateToken(String email, Long customerId, String role) {
        // the logged in customer's email, customerId and role
        // will be stored inside the token

        Map<String, Object> claims = new HashMap<>();

        // storing customerId inside the token
        claims.put("customerId", customerId);

        // storing role inside the token
        claims.put("role", role);

        return Jwts.builder()

                // adding custom claims
                .claims(claims)

                // setting the subject (who the token belongs to)
                .subject(email)

                // setting token creation time
                .issuedAt(new Date())

                // setting token expiration time
                .expiration(new Date(System.currentTimeMillis() + expirationTime))

                // signing the token using the secret key
                .signWith(getSigningKey())

                // building the final JWT token string
                .compact();
    }
}