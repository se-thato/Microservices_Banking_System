package com.banking.banking_api.dto;

import com.banking.banking_api.model.TransactionStatus;
import com.banking.banking_api.model.TransactionType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    // what will be sent back for each transaction

    private Long id;
    private String transactionReference;
    // unique reference number how much money moved

    private Long accountId;
    //which account this transaction belong to

    private BigDecimal amount;
    //how much money was moved

    private TransactionType transactionType;
    //CREDIT or DEBIT

    private TransactionStatus status;
    //PENDING, COMPLETED, or FAILED

    private String description;
    //e.g. "Salary deposit - March 2026"

    private BigDecimal balanceAfter;
    // balance after this transaction completed

    private LocalDateTime createdAt;
    // when the transaction happened
    // used to determine sync vs async retrieval
}