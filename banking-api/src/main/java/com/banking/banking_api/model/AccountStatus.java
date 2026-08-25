package com.banking.banking_api.model;

public enum AccountStatus {
    ACTIVE,
    //meaning the account is open and fully operational

    INACTIVE,
    //account exists but cannot transact

    CLOSED
    //now the account has been permanently closed, has historical records only
}
