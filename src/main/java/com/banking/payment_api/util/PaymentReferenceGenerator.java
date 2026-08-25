package com.banking.payment_api.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;


@Component
//this class will generate unique payment reference numbers
public class PaymentReferenceGenerator {

    private final AtomicInteger counter = new AtomicInteger(0);
    //AtomicInteger handles muitiple payments at the same time safely
    //also will prevent two payment getting the same reference

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    //this the date formate, year/month/date

    public String generate() {
        //generating the  ubique reference

        String date = LocalDateTime.now().format(DATE_FORMAT);
        //meaning get today's date format as YYYYMMDD

        int count = counter.incrementAndGet();
        //increment counter safely, even with many simultaneous requests

        return String.format("PAY-%s-%06d", date, count);
        //PAY represents the payment identifier
        //%06d = 6 digits padded counter
    }
}
