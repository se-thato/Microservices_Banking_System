package com.thato.customer_api.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity // this annotation will tell spring that "this a class database table" so spring creates a table called customer
@Table(name="customers",
        //this assist in naming the table in the DB
        uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        //this forces the db to not allow two same email or telling it no two rows should share the email
        //So the database will reject duplicate emails
        @UniqueConstraint(columnNames = "id_number")
        //this will reject duplicate ID numbers no users should share the same ID number, so it makes it unique
        }
)

@Data //this will create getters and setters automatically
@Builder
@NoArgsConstructor //empty constructor
@AllArgsConstructor //creates constructor with all fields
public class Customer {

    @Id //this marks the primary key - auto incremented by the database
    @GeneratedValue(strategy = GenerationType.IDENTITY) //this informs db to generate the ID
    @Column(name ="id")
    private Long id;



    @NotBlank(message = "First name is required")
    //The @NotBlank ensures that firstName field should not be empty or null
    //if validation fails the message will be sent
    @Size(min = 3, max = 100, message = "First name should be between 3 and 100 characters")
    //this stops users from entering a single letter for their names
    @Column(name = "first_name", nullable = false)
    //colum in DB will be called first_name
    //database will also reject empty values
    private String firstName;


    @NotBlank(message = "Last name is required")
    @Size(min = 3, max = 100, message = "First name should be between 3 and 100 characters")
    @Column(name = "last_name", nullable = false)
    private String lastName;


    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    //this annotation will check if the email is valid and is it in a correct form(EMAIL form)
    @Column(name = "email", nullable = false, unique = true)
    private String email;


    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false)
    private String password;


    @NotBlank(message = "ID number is required")
    @Size(min = 13, max = 13, message = "South African should be exactly 13 digits")
    @Column(name = "id_number", nullable = false, unique = true)
    private String idNumber;


    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+27|0)[6-8][0-9]{8}$", message = "Please provide a valid South African phone number")
    //the pattern will match if the phone number matches South African format
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;


    @Enumerated(EnumType.STRING)
    //Enum - is a fixed list of allowed values
    // So this should allow or assist us in knowing the customer Status
    @Column(name = "status", nullable = false)
    private CustomerStatus status;
    //this will only be ACTIVE, INACTIVE or SUSPENDED


    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable = false)
    private CustomerRole role;


    @Column(name = "created_at", nullable = false, updatable = false)
    //so once a customer registers it never updated or changed
    private LocalDateTime createdAt;


    @Column(name = "updated_at")
    // here you can update, so every time when a profile is changed it we want a new timestamp
    private LocalDateTime updatedAt;



    @PrePersist
    //This method happens automatically just before a new customer is inserted in the DB for the first time
    protected void onCreate(){
        createdAt = LocalDateTime.now(); //this capture the exact moment of registration
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = CustomerStatus.ACTIVE;
            //if the is no status was provided, default to ACTIVE
            // So it means every new customer status will start with ACTIVE
        }
        if (role == null) {
            role = CustomerRole.ROLE_CUSTOMER;
            //so every customer defaults to ROLE_CUSTOMER
            //admin must manually assigned
        }
    }


    @PreUpdate
    // this happens before any update, happen automatically before an existing customer record
    //get updated in DB
    protected void onUpdate() {
        //This means refresh the timestamp right now
        //allowing to see when the last change was made
    }
}
