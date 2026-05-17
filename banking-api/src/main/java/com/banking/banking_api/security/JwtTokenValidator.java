package com.banking.banking_api.security;

import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;
import java.util.Date;


@Component
public class JwtTokenValidator {

    private final com.banking.banking_api.security.JwtTokenExtractor extractor;
    // We need the extractor to READ the token before we can validate it

    public JwtTokenValidator(com.banking.banking_api.security.JwtTokenExtractor extractor) {
        // Constructor injection — Spring sees JwtTokenExtractor is needed
        // finds the @Component bean we created and passes it in
        this.extractor = extractor;
    }

    public boolean isTokenValid(String token, String email) {
        //this checks if a token is genuine and belongs to the right customer
        //return True if valid and False if not

        try {
            // if the token is tampered or broken, extractor methods throw JwtException
            // without try-catch, that exception would crash the whole app
            // with try-catch, we safely return false instead

            String extractedEmail = extractor.extractEmail(token);
            // extractEmail() lives in JwtTokenExtractor, not in this class
            // we access it through the extractor object: extractor.extractEmail()
            //this will pull the email out of the Token

            return extractedEmail.equals(email) && !isTokenExpired(token);
            //so here will be doing two checks:
            //1.extractedEmail.equals(email) - this asks itself does this email in the token match the looked up,
            //preventing someone from using another person's token
            //2.!isTokenExpired(token) - asks itself if the token still valid not expired
            //both must true for the token to be valid

        } catch (JwtException e) {
            // token was tampered, malformed, or broken
            // we don't need to know WHY — just reject it
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        //private helper checks if the token's expiry time has passed
        //return tru if expired

        return extractor.getExpirationDate(token)
                // extractAllClaims() lives in JwtTokenExtractor not here
                // we use extractor.getExpirationDate() which does the same thing
                // but through the extractor object we have access to
                //gets the expiration date stored inside the token

                .before(new Date());
        //this means is the expiry time BEFORE right now?
        //if yes mean token has EXPIRED return true
    }
}