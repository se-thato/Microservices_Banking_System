package com.banking.payment_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {


    @NotBlank(message = "Receiver account number is required")
    @Pattern(regexp = "^[0-9]{13}$", message = "Account number be exactly 13 digits")
    private String toAccountNumber;


    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;


    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "Pin must be 4 to 6 digits")
    private String pin; //to authorise the payment


    private String description;


    @Pattern(regexp = "^[0-9]{6}$", message = "Branch code must be exactly 6 digits")
    private String branchCode; //optional

}
