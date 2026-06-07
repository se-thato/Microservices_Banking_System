package com.banking.banking_api.dto;

import com.banking.banking_api.model.Transaction;
import com.banking.banking_api.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalTransactionRequestDTO {

    @NotNull(message = "Account ID is required")
    private Long accountId;


    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount; //transaction amount

    @NotNull(message = "Transaction type is required") //DEBIT or CREDIT
    private TransactionType transactionType;

    @NotNull(message = "Description is required")
    private String description;

    @NotNull(message = "Transaction reference is required")
    private String transactionReference;
    //unique reference linking debit and credit together

    private BigDecimal balanceAfter;


}
