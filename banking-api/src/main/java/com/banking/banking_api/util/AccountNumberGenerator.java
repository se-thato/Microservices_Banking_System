package com.banking.banking_api.util;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class AccountNumberGenerator {

    private static final String BANK_PREFIX = "2511";
    //this will the first four identify the bank

    private Random random = new Random();

    public String generate() {
        //generating 13 digits account number

        StringBuilder accountNumber = new StringBuilder(BANK_PREFIX); //we always start with bank prefixs

        for (int i = 0; i < 9; i++) {
            accountNumber.append(random.nextInt(10)); //mean we append 9 random digits (0-9)
        }

       return accountNumber.toString();
        //so we'll get "2511" + 9 random digits = 13 digits account number
    }

}
