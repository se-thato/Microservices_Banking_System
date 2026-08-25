package com.banking.banking_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    //unique id for a transaction


    @Column(name = "transaction_reference", nullable = false, unique = true)
    private String transactionReference;
    //this a unique reference number for the transaction
    //e.g "TXN-26546387-002"
    //this is generated automatically when the transaction is created


    @Column(name = "account_id", nullable = false)
    private Long accountId; // which accound th transaction belong to, which will link back to Account table


    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    //this records how much money moved in to this transaction


    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    //CREDIT - money that came IN to the account
    //DEBIT - Money that went out of the account


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;
    //PENDING, COMPLETED or FAILED


    @Column(name = "description")
    private String description;
    // this a human readable description of t=what the transaction was for


    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    // what was the balance after the transaction, stored for historical accuracy


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    //when was this transaction happened


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        //capturing the exact moment transaction was made
        if (status == null) {
            status = TransactionStatus.PENDING;
            //so this means transaction will start as PENDING when made
            //then move to COMPLETED or FAILED after precessing
        }
    }
}
