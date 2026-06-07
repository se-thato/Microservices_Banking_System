package com.banking.banking_api.controller;

import com.banking.banking_api.dto.AccountRequestDTO;
import com.banking.banking_api.dto.AccountResponseDTO;
import com.banking.banking_api.dto.PinSetupDTO;
import com.banking.banking_api.dto.TransactionResponseDTO;
import com.banking.banking_api.exception.UnauthorizedException;
import com.banking.banking_api.security.JwtTokenExtractor;
import com.banking.banking_api.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banking")
public class BankingController {

    private final BankingService bankingService;
    private final JwtTokenExtractor jwtTokenExtractor;

    public BankingController(BankingService bankingService,
                             JwtTokenExtractor jwtTokenExtractor) {
        this.bankingService = bankingService;
        this.jwtTokenExtractor = jwtTokenExtractor;
    }

    // Creating an Account
    @PostMapping("/accounts")
    public ResponseEntity<AccountResponseDTO> createAccount(
            @RequestBody @Valid AccountRequestDTO dto,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);

        Long customerId = jwtTokenExtractor.extractCustomerId(token);

        if (customerId == null) {
            throw new UnauthorizedException(
                    "IDENTIFICATION_VERIFICATION_FAILED",
                    "Sorry could not verify your identity. Please try again"
            );
        }

        AccountResponseDTO account =
                bankingService.createAccount(customerId, dto, token);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    // SET UP PIN
    @PostMapping("/accounts/{accountId}/pin")
    public ResponseEntity<Map<String, String>> setupPin(
            @RequestBody @Valid PinSetupDTO dto,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long customerId = jwtTokenExtractor.extractCustomerId(token);

        if (customerId == null) {
            throw new UnauthorizedException(
                    "IDENTIFICATION_VERIFICATION_FAILED",
                    "Sorry could not verify your identity. Please try again"
            );
        }

        bankingService.setupPin(customerId, dto, token);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "PIN set successfully. All your accounts are now active."
                )
        );
    }

    // Getting all accounts for a customer
    @GetMapping("/accounts/{customerId}")
    public ResponseEntity<List<AccountResponseDTO>> getCustomerAccounts(
            @PathVariable Long customerId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);

        List<AccountResponseDTO> accounts =
                bankingService.getCustomerAccounts(customerId, token);

        return ResponseEntity.ok(accounts);
    }

    // Getting one specific account balance
    @GetMapping("/accounts/{customerId}/{accountId}")
    public ResponseEntity<AccountResponseDTO> getAccountById(
            @PathVariable Long customerId,
            @PathVariable Long accountId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);

        AccountResponseDTO account =
                bankingService.getAccountById(accountId, customerId, token);

        return ResponseEntity.ok(account);
    }

    // Getting recent transactions (less than 5 days old)
    @GetMapping("/transactions/recent/{customerId}/{accountId}")
    public ResponseEntity<?> getRecentTransactions(
            @PathVariable Long customerId,
            @PathVariable Long accountId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);

            List<TransactionResponseDTO> transactions =
                    bankingService.getRecentTransactions(
                            accountId,
                            customerId,
                            token
                    );

            return ResponseEntity.ok(transactions);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }

    // Getting transaction history older than 5 days
    @GetMapping("/transactions/history/{customerId}/{accountId}")
    public ResponseEntity<String> requestOlderTransactions(
            @PathVariable Long customerId,
            @PathVariable Long accountId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);

            String message =
                    bankingService.requestOlderTransactions(
                            accountId,
                            customerId,
                            token
                    );

            return ResponseEntity.accepted().body(message);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }
}