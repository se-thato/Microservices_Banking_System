package com.thato.customer_api.security;

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

                                // these are public endpoints — no token needed
                                .requestMatchers(
                                        HttpMethod.POST, "/api/customers/register",
                                        "/api/customers/login"
                                ).permitAll()
                                // register and login are open to everyone
                                // these are the only ways to GET a token in the first place

                                //swagger doc implementation
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html"
                                ).permitAll()

                                // Admin only endpoints
                                .requestMatchers(
                                        HttpMethod.GET, "/api/customers"
                                ).hasRole("ADMIN")
                                // GET /api/customers → get ALL customers

                                .requestMatchers(
                                        HttpMethod.GET, "/api/customers/search"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT, "/api/customers/*/admin"
                                ).hasRole("ADMIN")
                                // PUT /api/customers/{id}/admin

                                .requestMatchers(
                                        HttpMethod.DELETE, "/api/customers/**"
                                ).hasRole("ADMIN")

                                //Customer or admin endpoints
                                .requestMatchers(
                                        HttpMethod.GET, "/api/customers/*"
                                ).hasAnyRole("CUSTOMER", "ADMIN")
                                // GET /api/customers/{id}
                                // customer views own profile, admin can view any profile

                                .requestMatchers(
                                        HttpMethod.PUT, "/api/customers/*/profile"
                                ).hasAnyRole("CUSTOMER", "ADMIN")
                                //admin can also update certain customer details


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