package com.banking.payment_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPaymentResponseDTO {

    private String batchId;
    //unique ID for this bacth of payments, customer uses this to track progress

    private int totalPayments;
    //how many payments in this batch

    private String status;

    private String message;
    //human readable explaination


    private LocalDateTime submittedAt;
}
