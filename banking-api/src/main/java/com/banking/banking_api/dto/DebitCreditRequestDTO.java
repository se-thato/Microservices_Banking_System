package com.banking.banking_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

//internal dto
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitCreditRequestDTO {

    @NotNull(message = "Account ID is required")
    private Long accountId; //which account to debit or credit

    @NotNull(message = "Amount is Required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount; //how much to debit or credit

    private String description; //what is this transaction for

    private String transactionReference; //shared reference between debit and credit
    //link the two sides of the same payment
}
