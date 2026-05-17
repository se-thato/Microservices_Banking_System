package com.thato.customer_api.model;

public enum CustomerStatus {
    ACTIVE,
    // this allows customer to log in and make payments, view anything
    //Default for new customers

    INACTIVE,
    // account exists but limited, perhaps EMAIL not yet verified or account not used for long time

    SUSPENDED
    //Blocked account, can not log in
    //Only Admin will reactivate the account
}
