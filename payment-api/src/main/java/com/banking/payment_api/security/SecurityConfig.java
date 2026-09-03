package com.banking.payment_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                // CSRF disabled — using JWT which handles this differently

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // stateless — no sessions, every request must carry a JWT token

                .authorizeHttpRequests(auth -> auth

                                //swagger doc implementation
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html"
                                ).permitAll()


                                //making payment, only logged-in user permited to make payment
                                .requestMatchers(
                                        HttpMethod.POST, "/api/payments/transder"
                                ).hasRole("CUSTOMER")


                                //customer or admin endpoint, customer can view their payment details and also the admin
                                .requestMatchers(
                                        HttpMethod.GET, "/api/payments/status/**"
                                ).hasAnyRole("CUSTOMER", "ADMIN")

                                .anyRequest().authenticated()
                        //other endpoints will require valid token
                )


                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        // token is checked and authentication is set before anything else

        return http.build();
    }

    //jwt setup
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }
}