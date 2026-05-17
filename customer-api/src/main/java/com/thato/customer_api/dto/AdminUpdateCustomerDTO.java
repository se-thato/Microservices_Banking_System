package com.thato.customer_api.dto;

import com.thato.customer_api.model.CustomerStatus;
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
public class AdminUpdateCustomerDTO {
    // This DTO is for ADMINS only


    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;


    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    // Admin can correct a customer's last name


    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;


    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(\\+27|0)[6-8][0-9]{8}$",
            message = "Please provide a valid South African phone number"
    )
    private String phoneNumber;
    // Admin can update phone number on behalf of the customer
    // e.g. customer lost their phone and has a new number


    private CustomerStatus status;
    // MOST IMPORTANT admin field
    // Admin can change account status:
    // ACTIVE    - customer can log in and transact normally
    // INACTIVE  - customer exists but cannot transact
    // SUSPENDED - customer is blocked, possible fraud detected
    // to update name only without touching the status

}