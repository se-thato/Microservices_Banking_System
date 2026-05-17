package com.thato.customer_api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;


@Component // this tells the Spring to manage this class as bean
public class JwtTokenGenerator {

    @Value("${jwt.secret}")
    // this reads the value of jwt.secret from the application.properties file
    // and to make sure that our secret key is never exposed

    private String secretString;
    //this is the raw secret string which is stored in the properties file

    @Value("${jwt.expiration}")
    // this will read the jwt.expiration from the app.properties

    private long expirationTime; // how long will the token last


    private SecretKey getSigningKey() {
        // this will convert the plain secret string into proper cryptographic key

        return Keys.hmacShaKeyFor(secretString.getBytes());
        // Keys.hmacShaKeyFor() will take our secret string as bytes and turns into a SecretKey that can verify/sign JWT tokens
    }

    public String generateToken(String email) {
        // the logged in customer's email will goes inside the token
        //allowing it to return the token as a String

        return Jwts.builder()
                //jwts.buildders will start building the token

                .subject(email)
                //Setting the "subject" of the token - WHO this token for
                //the subject will be stored in the token's payload
                //so to validate the requests later we extract this email to which customer is making the request

                .issuedAt(new Date())
                // this will record the exact moment this token was generated

                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                //when will this token expires
                //System.currentTimeMillis tells us current time in milliseconds

                .signWith(getSigningKey())
                //this will sign the token with our secret key
                //So this creates a SIGNATURE

                .compact();
        // this builds the token into compact String return the token
        //with the 3 parts of a token(header, payload and signature) joined by dots
    }
}