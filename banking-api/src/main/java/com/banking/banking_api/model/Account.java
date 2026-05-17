package com.banking.banking_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//one customer could have multiple accounts(savings, current and business account)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="id")
    private Long id; //this a unique ID for the account in db


    @Column(name ="account_number", nullable = false, unique = true)
    private String accountNumber;
    //this should be unique no two account should share the same account number
    //this will be genarated when account is created


    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    //this will be used to link to a customer in customer-api


    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    //savings, business or current


    @Enumerated(EnumType.STRING)
    @Column(name ="status", nullable = false)
    private AccountStatus status;
    //specifying if the account is ACTIVE, INACTIVE or CLOSED


    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
    //this shows current balance of the account
    // we used BigDecimal bacause is money, we can't use float or double for money
    //precision is total digits and scale is 2 decimal places


    @Column(name = "currency", nullable = false)
    private String currency;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    //when was the account created


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    //last time the account was updated or modified


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AccountStatus.ACTIVE;
            //meaning the account should start as ACTIVE by default
        }
        if (currency == null){
            currency = "ZAR";
            //default currency should be South African one (Rand)
        }
        if (balance == null){
            balance = BigDecimal.ZERO;
            //new account sj=hould start with R0.00 if the account balance is null
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        //refresh timestamp every time account is updated
    }

}
