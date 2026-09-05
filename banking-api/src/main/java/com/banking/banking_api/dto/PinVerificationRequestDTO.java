package com.banking.banking_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//internal DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinVerificationRequestDTO {

    @NotNull(message = "Account ID is required" )
    private Long accountId; //which account are we verifying the PIN for

    @NotNull(message = "PIN is required")
    private String pin; //the pin entered by the user, we compare it against the stored hash
}
