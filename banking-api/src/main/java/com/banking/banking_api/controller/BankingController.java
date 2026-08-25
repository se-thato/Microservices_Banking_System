package com.banking.banking_api.controller;

import com.banking.banking_api.dto.AccountResponseDTO;
import com.banking.banking_api.dto.TransactionResponseDTO;
import com.banking.banking_api.Service.BankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banking")
public class BankingController {

    private final BankingService bankingService;

    public BankingController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping("/accounts/{customerId}") // GET /api/banking/accounts/{customerId}
    //getting all accounts for a customer
    public ResponseEntity<List<AccountResponseDTO>> getCustomerAccounts(
            @PathVariable Long customerId,
            @RequestHeader("Authorization") String authHeader) {


        String token = authHeader.substring(7); // this removes "Bearer" to et just the token only

        List<AccountResponseDTO> accounts = bankingService.getCustomerAccounts(customerId, token);
        return ResponseEntity.ok(accounts);
    }


    //Getting ONE specific account BALANCE ( /api/banking/{customerId}/{accountId} )
    @GetMapping("/accounts/{customerId}/{accountId}")
    public ResponseEntity<AccountResponseDTO> getAccountById(
            @PathVariable Long customerId,
            @PathVariable Long accountId,
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = authHeader.substring(7);

        AccountResponseDTO account = bankingService.getAccountById(accountId, customerId, token);
        return ResponseEntity.ok(account);
    }


    //getting recent transaction of less than 5 days old - respond immediately
    @GetMapping("/transactions/recent/{customerId}/{accountId}")
    public ResponseEntity<List<TransactionResponseDTO>> getRecentTransactions(
            @PathVariable Long customerId,
            @PathVariable Long accountId
    ) {
        List<TransactionResponseDTO> transactions = bankingService.getRecentTransactions(accountId, customerId);
        return ResponseEntity.ok(transactions);
    }



    //getting  transaction older history of more than 5 days old
    @GetMapping("/transactions/history/{customerId}/{accountId}")
    public ResponseEntity<String> requestOlderTransactions(
            @PathVariable Long customerId,
            @PathVariable Long accountId
    ) {
        String message = bankingService.requestOlderTransactions(accountId, customerId);
        return ResponseEntity.accepted().body(message);
        //this will return a message stating that your request is still precessing
    }
}
