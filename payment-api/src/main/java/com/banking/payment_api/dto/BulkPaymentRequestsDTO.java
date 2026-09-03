package com.banking.payment_api.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cglib.beans.BulkBean;

import java.util.List;

//so this full bulk payment, Company sends ONE request with ALL payments inside, e.g salary for 100 employees
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkPaymentRequestsDTO {

    @NotBlank(message = "PIN is required to authorise bulk payment")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4 to 6 digits only")
    private String pin;

    @NotEmpty(message = "Payment list cannot be empty")
    @Size(min = 1, max = 1000, message = "Bulk payment supports 1 to 1000 payments")
    private List<BulkPaymentItemDTO> payments;
    //this is the list of all payments to process
    //each item has: toAccountNumber, amount, description

    private String batchDescription;
    //meaning what is this bulk payment for
}
