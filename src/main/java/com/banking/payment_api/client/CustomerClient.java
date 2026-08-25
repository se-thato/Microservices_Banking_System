package com.banking.payment_api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CustomerClient {

    private final RestTemplate restTemplate;

    @Value("${customer.api.url}")
    private String customerApiUrl;

    public CustomerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        return headers;
    }


    public String getCustomerFullName(Long customerId, String token) {
        //this will call customer api for customer full name

        try {
            String url = customerApiUrl + "/api/customers/" + customerId;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() !=null) {
                Map<String, Object> body = response.getBody();
                String firstName = (String) body.get("firstName");
                String lastName = (String) body.get("lastName");

                if(firstName != null && lastName != null) {
                    return firstName + " " + lastName; //return a sing string
                }
            }

            return "Unknown Customer";

        } catch (Exception e) {
            System.out.println("CustomerClient error: " + e.getMessage());

            return "Unknown Customer";
        }
    }
}