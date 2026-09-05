package com.banking.banking_api.model;

public enum AccountStatus {
    PENDING, //the account might be created but Pin not yet set

    ACTIVE,
    //pin is set then account can be fully operate
    //meaning the account is open and fully operational

    INACTIVE,
    //account exists but cannot transact

    CLOSED
    //now the account has been permanently closed, has historical records only
}
