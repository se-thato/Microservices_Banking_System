package com.banking.banking_api.exception;


//this is thrown when business rules are violeted (e.g email already registered or account not active)
public class BusinessException extends RuntimeException{

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
