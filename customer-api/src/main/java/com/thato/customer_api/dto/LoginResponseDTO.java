package com.thato.customer_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // this will generate getters, setters, toString, equals for all fields
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    //this is the JWT token will be generated after login is successfully

    private String tokenType;
    //this is for telling the frontend to use the token, usually it almost always "Bearer"
    //Bearer mean "whoever has this token trust them"


    private CustomerResponseDTO customer;
    // the logged in user/customer's safe profile information
    //frontend will use this to show the customer's name, check their status, etc.

    private long expiresIn;
    // this is for telling us how long should the token last before it expires
    //we'll set this to 86400000 milliseconds which is 24 hours(1 day)
}
