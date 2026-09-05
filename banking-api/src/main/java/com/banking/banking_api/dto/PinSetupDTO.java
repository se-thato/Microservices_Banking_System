package com.banking.banking_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinSetupDTO {

    @NotBlank(message = "PIN is required")
    @Pattern(
            regexp = "^[0-9]{4,6}$",
            message = "PIN must be 4 to 6 digits only"
    )
    private String pin;


    @NotBlank (message = "Please confirm your PIN")
    @Pattern(
            regexp = "^[0-9]{4,6}$",
            message = "PIN must be 4 to 6 digits only"
    )
    private String confirmPin;

}
