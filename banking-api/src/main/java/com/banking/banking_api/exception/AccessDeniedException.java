package com.banking.banking_api.exception;

//so this is thrown when the user tries to access sometiong they're not allowed to
public class AccessDeniedException extends RuntimeException{

    private final String code;

    public AccessDeniedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
