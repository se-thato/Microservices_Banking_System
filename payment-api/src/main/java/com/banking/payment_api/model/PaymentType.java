package com.banking.payment_api.model;

public enum PaymentType {

    SINGLE,
    //this is a n=one time transfer between two accounts
    //processed immediately and synchronously, example: Thato pays Sibonelo R600

    RECURRING,
    // this is a scheduled repeating payment
    //exaple would be: monthly debit order or subscriptions(netflix)

    BULK
    //here many payments are being processed at once
    //example: salary run, paying all employees at the same time

}
