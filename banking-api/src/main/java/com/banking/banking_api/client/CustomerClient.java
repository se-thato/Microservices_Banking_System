package com.banking.banking_api.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CustomerClient {

    private final RestTemplate restTemplate; // making HTTP calls to other services

    private static final String CUSTOMER_API_URL = "http://localhost:8081";

    public CustomerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getCustomerFullName(Long customerId, String token) {

        // this will call the Customer API to get customer full name
        // it returns firstName and lastName as a single string

        try {

            // building the URL to call
            String url = CUSTOMER_API_URL + "/api/customers/" + customerId;
            // Example:
            // http://localhost:8081/api/customers/1

            // setting up Authorization header with JWT token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            // wrapping headers inside HttpEntity
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // making GET request to Customer API
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // checking if request was successful
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {

                Map<String, Object> body = response.getBody();

                // extracting fields from JSON response
                String firstName =(String) body.get("firstName");
                String lastName =(String) body.get("lastName"); //get the lastName value

                if (firstName != null && lastName != null) {
                    // returning full name
                    return firstName + " " + lastName;
                }

            }

            // safe fallback
            return "Unknown Customer";

        } catch (Exception e) {
//            System.out.println("=== CustomerClient ERROR: " + e.getMessage());
//            e.printStackTrace();

            // if Customer API is down/unreachable
            // don't crash Banking API
            return "Unknown Customer";
        }
    }


    public Long getCustomerIdByEmail(String email, String token) {
        //so we'll find the Customer ID by their email

        try {
            //we'll try serching the customer by their email using the search endpoin from customer api
            String url = CUSTOMER_API_URL + "/api/customers/search?query=" + email;
            //so this will return all the customers matching the email

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<java.util.List> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, java.util.List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {

                Map<String, Object> customer = (Map<String, Object>) response.getBody().get(0);

                //extracting and retuin the ID
                Object id = customer.get("id");
                if (id instanceof Integer) {
                    return ((Integer) id).longValue();
                }
            }

            return null;
            //customer not found

        } catch (Exception e) {
            return null;
        }
    }

    //is account locked ??
    public boolean isAccountLocked(Long customerId, String token) {
        try {
            String url = CUSTOMER_API_URL + "/api/customers/internal/" + customerId + " /locked";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class
            );

            Map body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("locked"));

        } catch (Exception e) {
            return false;
        } // is the Customer AP unreachable, don't block, fail open
    }


    public void recordFailedPinAttempts(Long customerId, String token) {
        try {
        String url = CUSTOMER_API_URL + "/api/customers/internal/" + customerId + "/record-failed-pin";


        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

        } catch (Exception e) {
            System.out.println("Could not record failed PIN attempt: " + e.getMessage());
        }
    }


    public void resetFailedAttempts(Long customerId, String token) {
        try {
            String url = CUSTOMER_API_URL + "/api/custmers/internal/" + customerId + "/reset-failed-attempts";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

        } catch (Exception e) {
            System.out.println("Could not reset failed attempts: " + e.getMessage());
        }
    }

}