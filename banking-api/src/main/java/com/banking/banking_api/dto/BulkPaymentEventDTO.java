package com.banking.banking_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;


//this is for kafka message structure
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPaymentEventDTO {

    private String batchId;
    private String paymentReference;
    private Long fromAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
    private Long customerId;
    private String token;
}