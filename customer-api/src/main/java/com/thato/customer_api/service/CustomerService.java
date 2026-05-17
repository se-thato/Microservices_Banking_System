package com.thato.customer_api.service;
import com.thato.customer_api.dto.*;
import com.thato.customer_api.model.Customer;
import com.thato.customer_api.model.CustomerStatus;
import com.thato.customer_api.repository.CustomerRepository;
import com.thato.customer_api.security.JwtTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;


import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
// Spring manages it and makes it available wherever needed via @Autowired
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;


    private final PasswordEncoder passwordEncoder;

    private final JwtTokenGenerator jwtTokenGenerator;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenGenerator jwtTokenGenerator) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    //Register section
    public CustomerResponseDTO register(RegisterRequestDTO dto) {
        // dto = the data that came in from the frontend from RegisterRequestDTO(register form)
        // CustomerResponseDTO = what we send back after a user have registered

        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email is already registered");
            // This checks BEFORE saving and if email exists, it will stop immediately
            // Without this check, the DB would throw a confusing SQL error
            // This gives a clear, readable error message to the frontend
        }

        if (customerRepository.existsByIdNumber(dto.getIdNumber())) {
            throw new RuntimeException("ID number is already registered");
            // Checks for ID number,it must be unique per customer
            // Two people cannot share an ID number
        }

        Customer customer = Customer.builder()
                // @Builder lets us build the object field by field which is clean and readable
                // Instead of: new Customer(null, "Thato", "Bighead", email, password)
                // which is error-prone with many fields

                .firstName(dto.getFirstName())
                // Takes firstName FROM the DTO and put it INTO the Customer model

                .lastName(dto.getLastName())
                // Takes lastName from DTO

                .email(dto.getEmail())
                // Takes email from DTO

                .password(passwordEncoder.encode(dto.getPassword()))
                // this ensures to NEVER save the plain password
                // passwordEncoder.encode() hashes it using Bcrypt
                // Even if someone steals the database, they can't read passwords

                .idNumber(dto.getIdNumber())
                // Takes ID number from DTO

                .phoneNumber(dto.getPhoneNumber())
                // Takes phone number from DTO

                .build();
        // Finalise and create the Customer object
        // status, createdAt, updatedAt will be set automatically

        Customer savedCustomer = customerRepository.save(customer);
        // Save the Customer object to the database
        // Returns the saved customer WITH the generated ID filled in

        return convertToResponseDTO(savedCustomer);
        // Convert the saved Customer model → CustomerResponseDTO
        // This filters out sensitive fields like password and idNumber
        // before sending the response back to the frontend
    }


    //Login Section
    public LoginResponseDTO login(LoginRequestDTO dto) {
        // For now returning CustomerResponseDTO

        Customer customer = customerRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        // Find customer by their email, if not found throw an error

        if (!passwordEncoder.matches(dto.getPassword(), customer.getPassword())) {
            throw new RuntimeException("Invalid email or password");
            // passwordEncoder.matches() compares:
            // the plain password the user typed (dto.getPassword())
            // against the hashed password stored in DB (customer.getPassword())
            // Returns true if they match, false if not
            // We NEVER hash and compare manually — BCrypt handles this
        }

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new RuntimeException("Account is not active. Please contact support");
            // this means if the user status is INACTIVE return the error message
            // If status of the user is INACTIVE or SUSPENDED must not be able to login
        }

        String token = jwtTokenGenerator.generateToken(customer.getEmail());
        // this will call jwtGenerator.generateToken()


        return LoginResponseDTO.builder()
        // Return the customer profile info and token if login successful

                .token(token)
                .tokenType("Bearer")
                .customer(convertToResponseDTO(customer))
                .expiresIn(86400000) //expires after 24 hours
                .build();
    }



    // This section is for getting a user Profile
    public CustomerResponseDTO getCustomerById(Long id) {
        // Changed return type from Customer to CustomerResponseDTO

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        // orElseThrow means if empty, throw this error
        // e.g. "Customer not found with id: 42"

        return convertToResponseDTO(customer);
        // Convert model → DTO before returning
    }


    //Getting all customers (admin only)
    public List<CustomerResponseDTO> getAllCustomers() {
        // Admin gets a list of customers but still shouldn't see passwords

        return customerRepository.findAll()
                .stream()
                // .stream() turns the List into a stream
                // Think of it like a conveyor belt — each customer passes through

                .map(this::convertToResponseDTO)
                // .map() transforms each Customer on the belt into a CustomerResponseDTO
                // this::convertToResponseDTO is a shortcut for:
                // customer, convertToResponseDTO(customer)

                .collect(Collectors.toList());
        // .collect() gathers all the converted DTOs back into a List
    }



    //Customer Update section (contact details only)
    public CustomerResponseDTO updateProfile(Long id, UpdateProfileDTO dto) {
        // Customer can only update email and phone number

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));


        if (!customer.getEmail().equals(dto.getEmail()) &&
                customerRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email is already in use");
            // first the db should check if is any email same to this meaning is it taken by anyone
            // existsByEmail(dto.getEmail()) = already exists in DB
            // Then give the error if the email is already been taken by another user
        }

        customer.setEmail(dto.getEmail());
        // Update email, setEmail() was generated by @Data/@Lombok

        customer.setPhoneNumber(dto.getPhoneNumber());
        // Update phone number

        Customer updatedCustomer = customerRepository.save(customer);
        // Save the changes to the database
        // @PreUpdate in Customer model will automatically
        // update the updatedAt timestamp

        return convertToResponseDTO(updatedCustomer);
        // Return the updated customer info as DTO
    }



    //Admin update section
    public CustomerResponseDTO adminUpdateCustomer(Long id, AdminUpdateCustomerDTO dto) {
        // Admin can update names, contact details, and status, every user details

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        // Find the customer first

        if (!customer.getEmail().equals(dto.getEmail()) &&
                customerRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email is already in use by another customer");
        }

        customer.setFirstName(dto.getFirstName());
        // Admin can update first name — e.g. fixing a typo

        customer.setLastName(dto.getLastName());
        // Admin can update last name

        customer.setEmail(dto.getEmail());
        // Admin can update email on behalf of customer

        customer.setPhoneNumber(dto.getPhoneNumber());
        // Admin can update phone number on behalf of customer

        if (dto.getStatus() != null) {
            customer.setStatus(dto.getStatus());
            // Only update status if admin actually sent one
            // dto.getStatus() != null means admin included status in the request
            // If they didn't include it, keep the existing status
            // e.g. admin wants to fix a name only, status should stay unchanged
        }

        Customer updatedCustomer = customerRepository.save(customer);
        // Save all the admin's changes

        return convertToResponseDTO(updatedCustomer);
        // Return the updated customer info
    }



    //Deleting the customer from DB(admin only)
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        // first the admin must check customer exists before trying to delete
        // If the ID doesn't exist, deleteById silently does nothing
        // This way we throw a clear error if customer not found

        customerRepository.delete(customer);
        // Delete the found customer
        // Using delete(customer) instead of deleteById(id)
        // because we already have the customer object
    }



    //Searching the customer (admin only)
    public List<CustomerResponseDTO> searchCustomers(String search) {
        // uses the @Query search method from the repository
        // Search by firstName, lastName, and email

        return customerRepository.searchCustomers(search)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }



    //Private helper method
    private CustomerResponseDTO convertToResponseDTO(Customer customer) {
        // PRIVATE means only THIS class can use this method
        // It's a helper — other classes don't need to know it exists
        // Called internally whenever we need to convert a Customer to a DTO

        return CustomerResponseDTO.builder()
                // @Builder on CustomerResponseDTO lets us build it field by field

                .id(customer.getId())
                // Takes id FROM the Customer model → put into DTO

                .firstName(customer.getFirstName())
                // Takes firstName from model → DTO

                .lastName(customer.getLastName())
                // Takes lastName from model → DTO

                .email(customer.getEmail())
                // Takes email from model → DTO

                .phoneNumber(customer.getPhoneNumber())
                // Takes phoneNumber from model → DTO

                .status(customer.getStatus())
                // Take status from model → DTO

                .createdAt(customer.getCreatedAt())
                // Take createdAt from model → DTO

                .build();
        // Finalise and create the CustomerResponseDTO object
    }
}
