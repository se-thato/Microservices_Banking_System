package com.banking.payment_api.dto;

import com.banking.payment_api.model.PaymentStatus;
import com.banking.payment_api.model.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {

    private Long id; //payment unique record Id
    private String accountHolderName;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount; //amount payed
    private String description;

    private String paymentReference;
    //customers can use this to track their payment
    private PaymentType paymentType;
    private PaymentStatus status;
    private String failureReason; //null if successful

    private String transactionReference; //links to transactions records in Banking API
    private LocalDateTime createdAt;
    private LocalDateTime completedAt; // null if still processing
}
