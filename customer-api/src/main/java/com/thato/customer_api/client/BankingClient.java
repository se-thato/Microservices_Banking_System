package com.thato.customer_api.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// This allows the Customer API to call Banking API
// Used during registration to auto-create a default account
@Component
public class BankingClient {

    private final RestTemplate restTemplate;
    private static final String BANKING_API_URL = "http://localhost:8082";

    public BankingClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void createDefaultAccount(Long customerId, String token) {

        // Calls Banking API to automatically create a default SAVINGS account
        // This is executed immediately after customer registration

        try {
            String url = BANKING_API_URL
                    + "/api/banking/internal/create-default-account/"
                    + customerId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("Default account created successfully for customer: " + customerId);

        } catch (Exception e) {
            System.out.println(
                    "Warning: Could not create default account for customer "
                            + customerId + ": " + e.getMessage()
            );
            // Prevent registration from failing if Banking API is unavailable
        }
    }
}