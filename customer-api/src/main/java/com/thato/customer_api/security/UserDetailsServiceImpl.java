package com.thato.customer_api.security;

import com.thato.customer_api.model.Customer;
import com.thato.customer_api.repository.CustomerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
//marks this class as a Spring service bean
//this tells Spring to manage this class automatically
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;
    //used to access customer data from the database

    public UserDetailsServiceImpl(CustomerRepository customerRepository) {
        //Spring automatically gives us the repository object
        this.customerRepository = customerRepository;
    }

    @Override
    //Spring Security uses this method to find a user during login
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Customer customer = customerRepository.findByEmail(email)
                //find customer using email
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Customer not found with this email: " + email
                        )
                );
        //show error if email does not exist

        return User.builder()
                //creates a Spring Security user object
                .username(customer.getEmail())
                //sets the email as the username
                .password(customer.getPassword())
                //sets the saved password from database
                .authorities(getAuthorities(customer))
                //sets the user's role permissions
                .build();
        //returns the completed user object
    }

    private List<SimpleGrantedAuthority> getAuthorities(Customer customer) {
        //gives the user a role

        return List.of(new SimpleGrantedAuthority(customer.getRole().name())
                //customer.getRole(), say must get the CustomerRole enum value
        );
    }

}