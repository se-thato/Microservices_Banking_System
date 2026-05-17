package com.banking.banking_api.dto;

import com.banking.banking_api.model.AccountStatus;
import com.banking.banking_api.model.AccountType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDTO {
    //this is what will be sent back when a customer request for account info

    private Long id;
    private Long customerId;
    private String accountHolder; // the customers full name fetched from Customer API
    // which customer this belongs to
    private String accountNumber;
    private AccountType accountType;
    private AccountStatus status;
    // ACTIVE, INACTIVE, or CLOSED

    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
    // when the account was opened
}