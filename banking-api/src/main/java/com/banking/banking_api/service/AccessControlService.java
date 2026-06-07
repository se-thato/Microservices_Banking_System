package com.banking.banking_api.service;

import com.banking.banking_api.exception.UnauthorizedException;
import com.banking.banking_api.security.JwtTokenExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.banking.banking_api.exception.AccessDeniedException;

//this will decide who can see what
@Service
public class AccessControlService {

    //private final CustomerClient customerClient;
    private final JwtTokenExtractor jwtTokenExtractor;

    public AccessControlService(JwtTokenExtractor jwtTokenExtractor) {
        //this.customerClient = customerClient;
        this.jwtTokenExtractor = jwtTokenExtractor;
    }

    public void verifyCustomerAccess(Long requestedCustomerId, String token) {

        String role = jwtTokenExtractor.extractRole(token); //checks if this is admin

        // first we check is this the ADMIN?
        if ("ROLE_ADMIN".equals(role)) {
            return; // admin can access everything
        }

        // getting logged in customer's email from token
        Long loggedInCustomerId = jwtTokenExtractor.extractCustomerId(token);

        if (loggedInCustomerId == null) {
            throw new UnauthorizedException(
                    "IDENTITY_VERIFICATION_FAILED",
                    "Sorry we could not verify your identity. Please login again");
        }

        // get logged in customer's ID from CUSTOMER API
//        Long loggedInCustomerId =
//                customerClient.getCustomerIdByEmail(loggedInEmail, token);
//
//        if (loggedInCustomerId == null) {
//            throw new RuntimeException("Could not verify the customer identity");
//        }

        // check if customer IDs match
        if (!loggedInCustomerId.equals(requestedCustomerId)) {
            throw new AccessDeniedException(
                    "ACCESS_DENIED",
                    "Opps access denied: you can only view your own account information"
            );
        }
    }

    private JwtTokenExtractor getJwtTokenExtractor() {
        return jwtTokenExtractor;
    }

    // checks if currently authenticated user has ROLE_ADMIN
    private boolean isAdmin() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // no authentication found
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}