package com.banking.payment_api.model;

public enum PaymentStatus {

    PENDING, //here payment is initiated however not yet processed
    //will be saved to db be processing starts

    PROCESSING,
    //now the payment is being processed
    //debit & credit operation happemning

    COMPLETED, //payment was successfully

    FAILED, //payment failed
}
