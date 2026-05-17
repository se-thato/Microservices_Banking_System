package com.thato.customer_api.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "First name is Required")
    @Size(min = 3, max = 100, message = "First name should be between 3 and 100 characters")
    private String firstName; // we should have a username on register form


    @NotBlank(message = "Last name is required")
    @Size(min = 3, max = 100, message = "First name should be between 3 and 100 characters")
    private String lastName;


    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;


    @NotBlank(message = "Password is required")
    @Size(min =8, message = "Password must be at least 8 character")
    private String password;


    @NotBlank(message = "ID number is required")
    @Size(min = 13, max = 13, message = "South African should be exactly 13 digits")
    private String idNumber;


    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(\\+27|0)[6-8][0-9]{8}$", // the number should match South African phone format
            message = "Please provide a valid South African phone number")
    private String phoneNumber;

}
