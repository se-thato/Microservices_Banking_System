package com.thato.customer_api.dto;

import com.thato.customer_api.model.CustomerStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDTO {

    private Long id;

    private String firstName;
    // Safe to send back, customers sees this on their profile page

    private String lastName;
    // Safe to send back

    private String email;
    // Safe to send back, customers sees their own email

    private String phoneNumber;
    // Safe to send back, customers sees their own phone number

    private CustomerStatus status;

    private LocalDateTime createdAt;
    // Shown as "Member since..." on the profile page


}