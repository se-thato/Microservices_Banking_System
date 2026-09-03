package com.banking.payment_api.client;

import com.banking.payment_api.exception.BusinessException;
import com.banking.payment_api.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BankingClient {
    //this class is Payment API's connection to Banking API
    //All calls to banking api will go through this class

    private final RestTemplate restTemplate;

    @Value("${banking.api.url}")
    private String bankingApiUrl;
    //this will read banking.api.url from app properties

    public BankingClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createHeaders(String token) {
        //this like private helper, creates Authorization header
        //every Banking API calls needs the JWT token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        return headers;
    }




    public Map getAccountByNumber(String accountNumber, String token) {

        try {
            String url = bankingApiUrl + "/api/banking/internal/accounts/" + accountNumber;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return response.getBody(); //return account details as a Map

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(
                    "ACCOUNT_NOT_FOUND",
                    "Account not found with number: " + accountNumber);

        } catch (Exception e) {
            throw new BusinessException(
                    "BANKING_API_ERROR",
                    "Could not retrieve account details. Please try again"
            );
        }
    }

    public boolean verifyPin(Long accoundId, String pin, String token) {
        //verifies the customer PIN before before making the payment

        try {
            String url = bankingApiUrl + "/api/banking/internal/verify-pin";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accountId", accoundId);
            requestBody.put("pin", pin); // building the requst body for the PIN verification

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createHeaders(token));

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("valid"));
            //

        } catch (Exception e) {
            throw new BusinessException(
                    "PIN_VERIFICATION_ERROR",
                    "Could not verify PIN. Please try again"
            );
        }
    }

    public Map debitAccount(Long accountId,
                            BigDecimal amount,
                            String description,
                            String reference,
                            String token) {
        //debits the senders accounts
        //reduce their balance by the payment amount

        try {
            String url = bankingApiUrl + "/api/banking/internal/debit";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accountId", accountId);
            requestBody.put("amount", amount);
            requestBody.put("description", description);
            requestBody.put("transactionReference", reference);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createHeaders(token));

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            return response.getBody(); //this return the debit transaction details

        } catch (HttpClientErrorException e) {
            throw new BusinessException(
                    //Banking API returned an error
                    "DEBIT_FAILED",
                    "Could not debit account: " + e.getMessage()
            );
        }
    }

    //CREDIT ACCONTS
    public Map creditAccount(Long accountId,
                             BigDecimal amount,
                             String description,
                             String reference,
                             String token) {

        try {
            String url = bankingApiUrl + "/api/banking/internal/credit";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accountId", accountId);
            requestBody.put("amount", amount);
            requestBody.put("description", description);
            requestBody.put("transactionReference", reference);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, createHeaders(token));

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            return response.getBody(); //returns the credit transaction details

        } catch (Exception e) {
            throw new BusinessException(
                    "CREDIT_FAILED",
                    "Could not credit account. Please contact support team"
            );
        }
    }

    public Map getCustomerDefaultAccount(Long customerId, String token) {
        //getting customers Active account to use fromAccount, auto called when payment starts

        try{
            String url = bankingApiUrl + "/api/banking/accounts" + customerId;
            //get all accounts belong to the logged in customer from banking api

            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            ResponseEntity<List> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, List.class);


            if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {

                //then find the first ACTIVE account
                for (Object item : response.getBody()) {
                    Map<String, Object> account = (Map<String, Object>) item;
                    String status = (String) account.get("status");

                    if ("ACTIVE".equals(status)) {
                        return account;
                    }
                }
            }

            return null; //meaning no active acc was found

        } catch (Exception e) {
            System.out.println("BankingClient error getting account: " + e.getMessage());

            return null;
        }
    }

}