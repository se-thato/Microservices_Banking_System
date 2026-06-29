package com.thato.customer_api.service;
import com.thato.customer_api.client.BankingClient;
import com.thato.customer_api.dto.*;
import com.thato.customer_api.exception.BusinessException;
import com.thato.customer_api.exception.ResourceNotFoundException;
import com.thato.customer_api.exception.UnauthorizedException;
import com.thato.customer_api.model.Customer;
import com.thato.customer_api.model.CustomerStatus;
import com.thato.customer_api.repository.CustomerRepository;
import com.thato.customer_api.security.JwtTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;


import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
// Spring manages it and makes it available wherever needed via @Autowired
public class CustomerService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private static final long LOCKOUT_DURATION_MINUTES = 30; // acc will be locked for 30 minutes

    @Autowired
    private CustomerRepository customerRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final BankingClient bankingClient;


    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenGenerator jwtTokenGenerator,
                           BankingClient bankingClient) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.bankingClient = bankingClient;
    }

    //Register section
    public CustomerResponseDTO register(RegisterRequestDTO dto) {
        // dto = the data that came in from the frontend from RegisterRequestDTO(register form)
        // CustomerResponseDTO = what we send back after a user have registered

        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "EMAIL_ALREADY_REGISTERED",
                    "Email is already registered: " + dto.getEmail());
            // This checks BEFORE saving and if email exists, it will stop immediately
            // Without this check, the DB would throw a confusing SQL error (400)
            // This gives a clear, readable error message to the frontend
        }

        if (customerRepository.existsByIdNumber(dto.getIdNumber())) {
            throw new BusinessException(
                    "ID_NUMBER_ALREADY_REGISTERED",
                    "ID number is already registered");
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

        String internalToken = jwtTokenGenerator.generateToken(
                savedCustomer.getEmail(),
                savedCustomer.getId(),
                savedCustomer.getRole().name()
        ); //creating a token so banking api can authenticate, the auto account creation request

        bankingClient.createDefaultAccount(savedCustomer.getId(), internalToken);
        //so if Banking API is down registration still succeeds

        return convertToResponseDTO(savedCustomer);
        // Convert the saved Customer model → CustomerResponseDTO
        // This filters out sensitive fields like password and idNumber
        // before sending the response back to the frontend
    }


    //Login Section
    public LoginResponseDTO login(LoginRequestDTO dto) {
        // For now returning CustomerResponseDTO

        Customer customer = customerRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new UnauthorizedException(
                        "INVALID_CREDENTIALS",
                        "Invalid email or password"));
        // Find customer by their email, if not found throw an error


        //checking if the account is currently locked or not
        if (customer.getLockedUntil() != null
            && customer.getLockedUntil().isAfter(LocalDateTime.now())) {


            long minutesRemaining = java.time.Duration.between(
                    LocalDateTime.now(), customer.getLockedUntil()
            ).toMinutes() + 1;
            //this calculates how many minutes left until unlock

            throw new UnauthorizedException(
                    "ACCOUNT_LOCKED",
                    "Account is locked due to many failed attempts. " + "Please try again in " + minutesRemaining + " minute(s)"
            );
        }


        //now checking if the account:  was locked but time has passed?
        if (customer.getLockedUntil() != null && customer.getLockedUntil().isBefore(LocalDateTime.now())) {
            //meaning lock has expired then reset everithing
            customer.setFailedLoginAttempts(0);
            customer.setLockedUntil(null);
            //auto unlocks, meaning user gets a fresh start
        }

        //verifying password
        if (!passwordEncoder.matches(dto.getPassword(), customer.getPassword())) {

            //wrong passwoed
            int attempts = customer.getFailedLoginAttempts() + 1;
            customer.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                //if attemps are greater than 5
                customer.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));

                customerRepository.save(customer);

                throw new UnauthorizedException(
                        "ACCOUNT_LOCKED",
                        "Too many failed attempts. Account locked for " + LOCKOUT_DURATION_MINUTES + " minutes"
                );
            }

            customerRepository.save(customer);

            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            throw new UnauthorizedException(
                    "INVALID_CREDENTIALS",
                    "Invalid email or password. " + remaining + " attempt(s) remaining before lockout");
        }

        //checking if the account ACTIVE
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new UnauthorizedException(
                    "ACCOUNT_NOT_ACTIVE",
                    "Account is not active. Please contact support");
            // this means if the user status is INACTIVE return the error message
            // If status of the user is INACTIVE or SUSPENDED must not be able to login
        }

        //Reset failed attemps
        customer.setFailedLoginAttempts(0);
        customer.setLockedUntil(null);
        customerRepository.save(customer);
        //successful login will then reste everything


        String token = jwtTokenGenerator.generateToken(
                customer.getEmail(),
                customer.getId(),
                customer.getRole().name());
        // this will call jwtGenerator.generateToken()


        return LoginResponseDTO.builder()
        // Return the customer profile info and token if login successful

                .token(token)
                .tokenType("Bearer")
                .customer(convertToResponseDTO(customer))
                .expiresIn(86400000) //expires after 24 hours
                .build();
    }


    //banking api informing customer api that the customer has entered 3 wrong attemps, now lock them
    public void recordFailedPinAttempts(Long customerId) {
        //this is called by banking api when customer enters wrong pin

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "Customer not found with id: " + customerId
                ));


        //now checking if lock expired then reset if so(expires after 30 minutes)
        if (customer.getLockedUntil() != null && customer.getLockedUntil().isBefore(LocalDateTime.now())) {
            customer.setFailedLoginAttempts(0);
            customer.setLockedUntil(null);
        }

        int attempts = customer.getFailedLoginAttempts() + 1;
        customer.setFailedLoginAttempts(attempts);

        if (attempts >= 3) {
            customer.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
        }

        customerRepository.save(customer);
    }


    //the following will be called when the PIN is correct
    public void resetFailedAttempts(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "Customer not found with id: " + customerId
                ));

        customer.setFailedLoginAttempts(0);
        customer.setLockedUntil(null);

        customerRepository.save(customer);
    }


    public boolean isAccountLocked(Long customerId) {
        //so this boolean is called by BAnking API before checking PIN
        //so if acc is locked Banking API rejects without checking the PIN

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "Account not found with id: " + customerId
                ));


        return customer.getLockedUntil() != null && customer.getLockedUntil().isAfter(LocalDateTime.now());
    }



    // This section is for getting a user Profile
    public CustomerResponseDTO getCustomerById(Long id) {
        // Changed return type from Customer to CustomerResponseDTO

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "Customer not found with id: " + id));
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "Customer not found with id: " + id));


        if (!customer.getEmail().equals(dto.getEmail()) &&
                customerRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "EMAIL_ALREADY_IN_USE",
                    "Email is already in use by another account");
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "Customer not found with id: " + id));
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUSTOMER_NOT_FOUND",
                        "Customer not found with id: " + id));
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
