package com.banking.banking_api.exception;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int status; //HTTP status code number
    private String code; //readable error code(CUSTOMER_NOT_FOUND)
    private String message;
    private LocalDateTime timestamp;
    private List<String> details; //used for validation errors, e.g "firt name is required", "email invalid"
}
