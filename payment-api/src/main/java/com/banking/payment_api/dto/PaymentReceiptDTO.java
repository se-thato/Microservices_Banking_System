package com.banking.payment_api.dto;

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
public class PaymentReceiptDTO {

    private String paymentReference; //unique reference for this paynemnt
    private String transactionReference;
    private String accountHolderName;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private BigDecimal BalanceAfter;
    private String description;
    private String status; //payment was a success or failed
    private LocalDateTime processedAt;
}
