package com.banking.banking_api.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinVerificationResponseDTO {

    private Boolean valid; //true if PIN is correct

    private String message; // this should be human readable results, "PIN verified successfully"
}
