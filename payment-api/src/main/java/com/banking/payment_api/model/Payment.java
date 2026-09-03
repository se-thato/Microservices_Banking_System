package com.banking.payment_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//Every payment attempt will be recorded here regardless of success or failure
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "payment_reference", nullable = false, unique = true)
    private String paymentReference;
    //unique payment reference for this payment
    //used to track and reference the payment, e.g PAY-20250511-001

    @Column(name = "from_account_number", nullable = false)
    private String fromAccountNumber;
    //this will display the user account number


    @Column(name = "to_account_numner")
    private String toAccountNumber;
    //to which account number

    @Column(name = "from_account_id")
    private Long fromAccountId;
    // this a sender's account ID from Banking API


    @Column(name = "to_account_id")
    private Long toAccountId;
    //receiver's account Id in BANKING API

    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    //who initiated this payment
    //this will read fro JWT token


    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    // how much money to be transferred


    @Column(name = "description")
    private String description;
    //what is the payment for


    @Column(name = "branch_code")
    private String branchCode;
    //SA branch code of receiver bank, e.g FNB =250655


    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;


    @Enumerated(EnumType.STRING)
    @Column(name = "staus", nullable = false)
    private PaymentStatus status;


    @Column(name = "failure_reason")
    private String failureReason;
    //why the payment failed, e.g insufficient funds


    @Column(name = "transaction_reference")
    private String transactionReference;
    //this is the reference of the transaction creayed in Banking API
    //same as paymentReference


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; //when payment was made

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; //when was the payment last changed


    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    // when was the payment completed or failed, null until status is reached


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = PaymentStatus.PENDING; //every payment starts as PENDING
        }

        if (paymentType == null) {
            paymentType = PaymentType.SINGLE; //default to single payment
        }

    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        //refresh timestamp every time status changes
    }
}
