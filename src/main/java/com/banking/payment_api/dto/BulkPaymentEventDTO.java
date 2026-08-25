package com.banking.payment_api.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//this will be the message published to kafka for EACH payment is taking place
//e.g 100 employyes salary will result to 100 kafka messages
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPaymentEventDTO {

    private String batchId;
    private String paymentReference;
    //unique reference

    private Long fromAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
    private Long customerId;
    private String token;
}
