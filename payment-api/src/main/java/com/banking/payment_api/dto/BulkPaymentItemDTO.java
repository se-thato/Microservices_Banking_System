package com.banking.payment_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

//this dto represent ONE payment for a bulk payment, e.g one employee's salary
public class BulkPaymentItemDTO {

    @NotBlank(message = "Recipient account number is required")
    @Pattern(regexp = "^[0-9]{13}$", message = "Account number must be exactly 13 digits")
    private String toAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String description;
}
