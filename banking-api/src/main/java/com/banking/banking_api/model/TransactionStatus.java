package com.banking.banking_api.model;

public enum TransactionStatus {
    PENDING,
    //transaction initiated however not yet precessed

    COMPLETED,
    //transaction was successfully processed

    FAILED
    // the transaction could not be completed
}
