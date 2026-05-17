package com.thato.customer_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDTO {
    // this is dto is for customers only, they can update their details

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    // customer is allowed to change their email address if they wish to change it


    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(\\+27|0)[6-8][0-9]{8}$", // the number should match South African phone format
            message = "Please provide a valid South African phone number")
    private String phoneNumber;
    // if the user wishes to change their number they are allowed to do so
}
