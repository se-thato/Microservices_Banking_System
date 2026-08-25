package com.banking.payment_api.exception;


//this will then be thrown when the credentials are invalid or missing
// for example: wrong password, invalid token
public class UnauthorizedException extends RuntimeException{

    private final String code;

    public UnauthorizedException(String code, String message) {
        super(message);

        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
