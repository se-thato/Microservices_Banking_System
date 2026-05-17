package com.thato.customer_api.controller;

import com.thato.customer_api.dto.*;
import com.thato.customer_api.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
// Tells Spring: "This class handles HTTP requests and returns JSON"
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    //Register endpoint
    @PostMapping("/register")
    public ResponseEntity<CustomerResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO dto) {
        // @RequestBody — takes the JSON from the request body
        // and converts it into a RegisterRequestDTO object automatically
        // @Valid annotation activates all the validation rules on RegisterRequestDTO

        CustomerResponseDTO response = customerService.register(dto);
        // Pass the DTO to the service
        // (checking duplicates, hashing password, saving to DB)

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        // Return the response with HTTP status 201 CREATED
        // .body(response) = attach the CustomerResponseDTO as the response body
    }



    //Login endpoint section
    @PostMapping("/login")
    // Handles POST requests
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto) {
        // @RequestBody converts incoming JSON to LoginRequestDTO
        // @Valid checks email is not blank and looks like a real email

        LoginResponseDTO response = customerService.login(dto);
        // Service checks the email exists, password matches, account is ACTIVE

        return ResponseEntity.ok(response);
        // this will return bith token and profile info with HTTP 200 OK
    }


    //Get all customer section
    // GET /api/customers
    @GetMapping
    public ResponseEntity<List<CustomerResponseDTO>> getAllCustomers() {

        List<CustomerResponseDTO> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
        // Return the list with HTTP 200 OK
    }


    //Getting customer using their unique id
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> getCustomerById(
            @PathVariable Long id) {
        CustomerResponseDTO response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
        // Return the customer profile with HTTP 200 OK
    }


    //Customer updating their onw profile
    // PUT /api/customers/{id}/profile
    @PutMapping("/{id}/profile")
    public ResponseEntity<CustomerResponseDTO> updateProfile(
            @PathVariable Long id,
            // Pulls the customer ID from the URL
            @Valid @RequestBody UpdateProfileDTO dto) {
        // @Valid activates validation (@NotBlank, @Email, @Pattern)

        CustomerResponseDTO response = customerService.updateProfile(id, dto);
        // Service finds the customer, applies the changes, saves to DB
        return ResponseEntity.ok(response);
    }


    //Admin updating the customer details
    // PUT /api/customers/{id}/admin
    @PutMapping("/{id}/admin")
    public ResponseEntity<CustomerResponseDTO> adminUpdateCustomer(
            @PathVariable Long id,

            @Valid @RequestBody AdminUpdateCustomerDTO dto) {

        CustomerResponseDTO response = customerService.adminUpdateCustomer(id, dto);
        // Service applies all the admin's changes and saves to DB
        return ResponseEntity.ok(response);
    }


    // Deleting a customer
    @DeleteMapping("/{id}")
    // Handles DELETE requests to /api/customers/{id}
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        // ResponseEntity<Void> means we return NO body  just a status code

        customerService.deleteCustomer(id);
        // Tell the service to delete this customer
        // Service checks they exist first, then deletes

        return ResponseEntity.noContent().build();
        // HTTP 204 No Content = "deletion was successful, nothing to return"
    }


    // Searching customers(Admin only)
    // GET /api/customers/search?query=Thato
    //search by firstName, lastName, or email
    @GetMapping("/search")
    // Handles GET requests to /api/customers/search
    public ResponseEntity<List<CustomerResponseDTO>> searchCustomers(
            @RequestParam String query) {
        // @RequestParam pulls the value after the ? in the URL
        // e.g. /api/customers/search?query=Thato - query = "Thato"

        List<CustomerResponseDTO> results = customerService.searchCustomers(query);
        // Service calls the repository's custom @Query search method
        // Searches firstName, lastName, and email case-insensitively
        return ResponseEntity.ok(results);
    }
}