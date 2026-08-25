package com.banking.banking_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryRequestDTO {
    // this will be used when requesting older transaction history(async)

    private Long accountId;
    //which account to retrieve history for

    private String fromDate;
    //this is the start date for the history

    private String toDate;
    //then this is the end date for the history

    private String callbackEmail;
    //so this is telling us where to send the results when async processing is done
}
