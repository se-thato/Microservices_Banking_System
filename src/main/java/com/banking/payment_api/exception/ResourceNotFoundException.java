package com.banking.payment_api.exception;

// this will be thrown when a request resource doesn't exist in DB
//e.g customer not found or account not found
public class ResourceNotFoundException extends RuntimeException {

    private final String code; //this gives ur readable error code (CUSTOMER_NOT_FOUND)

    public ResourceNotFoundException(String code, String message) {
        super(message); //pass the message to parent RuntimeException
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
