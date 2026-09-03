package com.banking.payment_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;


@Component // this tells the Spring to manage this class as bean
public class JwtTokenExtractor {
    //this will ONLY reads/extracts data from tokens
    //pulls email and claims out of a token

    @Value("${jwt.secret}")
    private String secretString;

    private SecretKey getSigningKey() {
        // this will convert the plain secret string into proper cryptographic key

        return Keys.hmacShaKeyFor(secretString.getBytes());
        // Keys.hmacShaKeyFor() will take our secret string as bytes and turns into a SecretKey that can verify/sign JWT tokens
    }

    public Claims extractAllClaims(String token) {
        //reads the token and returns all it contents

        return Jwts.parser()
                //this will start the token reading process

                .verifyWith(getSigningKey())
                //this verifies the token SIGNATURE using the secret key
                // so if someone tampared with the token this will then throw an exception

                .build()
                //Builds the parser with the settings above

                .parseSignedClaims(token)
                //this one reads and decodes the token
                //throws an exception if Token signature is invalid(tampered), Token is expired, token format is wrong

                .getPayload();
        //this returns payload, the actual data inside the token
        //contains subject(email), issuedAt, expiration
    }

    public String extractEmail(String token) {
        // this will read a token and pulls out the email stored inside it

        return extractAllClaims(token).getSubject();
        //this return reads and verifies the token
    }


    public Date getExpirationDate(String token) {
        // Pulls the expiration date out of the token
        // Returns a Date object of when the token expires
        // Used by JwtTokenValidator to check if token is still alive

        return extractAllClaims(token).getExpiration();
        // .getExpiration() gets the Date we stored
        // with .expiration(new Date(...)) in JwtTokenGenerator
    }


    public Long extractCustomerId(String token) {
        // this reads the customerId stored inside the token

        Object customerId = extractAllClaims(token).get("customerId");
        //grtting the customerId claim from the token payload

        if (customerId instanceof Integer) {
            return ((Integer) customerId).longValue();
            //JSON will store numbers as Interger, convert to Long to match the ID type
        }

        if (customerId instanceof Long) {
            return (Long) customerId;
        }
        return null;
    }

    public String extractRole(String token) {
        //this the reads the role inside the token then return ROLE_ADMIN or ROLE_CUSTOMER

        Object role = extractAllClaims(token).get("role");
        return role != null ? role.toString() : "ROLE_CUSTOMER"; //default to ROLE_CUSTOMER if not found
    }
}