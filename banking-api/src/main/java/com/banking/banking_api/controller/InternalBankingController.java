package com.banking.banking_api.controller;

import com.banking.banking_api.dto.*;
import com.banking.banking_api.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/banking/internal") //this for internal endpoints
public class InternalBankingController {

    private final BankingService bankingService;

    public InternalBankingController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    //ths is called by Customer API when a customer register then auto create Account for the customer
    @PostMapping("/create-default-account/{customerId}")
    public ResponseEntity<AccountResponseDTO> createDefaultAccount(
            @PathVariable Long customerId) {
        //create default SAVINGS account automatically

        AccountResponseDTO account = bankingService.createDefaultAccount(customerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }


    //Payment API will have to look up accounts by accounts number
    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<AccountResponseDTO> getAccountByNumber(
            @PathVariable String accountNumber) {

        AccountResponseDTO account = bankingService.getAccountByAccountNumber(accountNumber);

        return ResponseEntity.ok(account);
    }

    //payment API will have to verify the PIN before processing payment
    @PostMapping("/verify-pin")
    public ResponseEntity<PinVerificationResponseDTO> verifyPin(
            @Valid @RequestBody PinVerificationRequestDTO dto) {

        PinVerificationResponseDTO response = bankingService.verifyAccountPin(dto);

        return ResponseEntity.ok(response);
    }


    //Payment API debits sender account
    @PostMapping("/debit")
    public ResponseEntity<TransactionResponseDTO> debitAccount (
            @Valid @RequestBody DebitCreditRequestDTO dto) {

        TransactionResponseDTO transaction = bankingService.debitAccount(dto);

        return ResponseEntity.ok(transaction);
    }


    //credit
    @PostMapping("/credit")
    public ResponseEntity<TransactionResponseDTO> creditAccount(
            @Valid @RequestBody DebitCreditRequestDTO dto) {

        TransactionResponseDTO transaction = bankingService.creditAccount(dto);

        return ResponseEntity.ok(transaction);
    }
}
