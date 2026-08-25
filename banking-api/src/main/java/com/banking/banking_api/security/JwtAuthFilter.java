package com.banking.banking_api.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    // this means runs this request once per request

    private final JwtTokenExtractor jwtTokenExtractor;
    //this will be used to pull the email out of the token

    private final JwtTokenValidator jwtTokenValidator;
    // this will be used to check if the token is genuine and not expired

    private final UserDetailsService userDetailsService;
    //used to load the customer from DB by their email

    public JwtAuthFilter (JwtTokenExtractor jwtTokenExtractor,
                          JwtTokenValidator jwtTokenValidator,
                          UserDetailsService userDetailsService) {
        //Spring injects all three automatically via constructor injection
        this.jwtTokenExtractor = jwtTokenExtractor;
        this.jwtTokenValidator = jwtTokenValidator;
        this.userDetailsService = userDetailsService;
    }

    @Override //we are replacing the parent class's empty version
    protected void doFilterInternal(HttpServletRequest request, // this the incoming request we inspect
                                    HttpServletResponse response, // then this is the responce we can modify
                                    FilterChain filterChain) // this let us pass the request to next filter when done
            throws ServletException, java.io.IOException{
        //doFilterInternal is the main method that runs for every request

        //first: must read the Authorization Header
        String authHeader = request.getHeader("Authorization");
        //this reads the Authorization Header from incoming request


        //Second step: we must check if HEADER exists and starts with "BEARER"
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            //so what happens  is that no token found then pass the request to next filter
            //the request will eventually hit SecurityConfig rules
            //so if the endpoint requires auth, then will return 401 status
            //if the endpoint is puc=blic one like, /login or /register it must fo through

            return;

        }

        // third step: Ectracting the Token from HEADER
        String token = authHeader.substring(7);// this removes "Bearer " from start of the header
        // substring(7) means, this will give me everything from position 7 onwards


        //Now extracting the email from the token
        String email = jwtTokenExtractor.extractEmail(token);
        //this extracts the user's email from the JWT token


        //Now checking if the user not already authenticated
        if (email != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {


            //Now loading customer from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            //this will go to db and loads the customer with this email
            //UserDetailsService assist us in finding the users


            //Now we validate the token
            if (jwtTokenValidator.isTokenValid(token, userDetails.getUsername())) {
                //this checks if the token matches the loaded customer then checks if the token is not expired


                //Now creating the authentication object
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, //who is the user
                        null, //credentials will be null because it already verified via JWT, so no need for password again
                        userDetails.getAuthorities()
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                        //this adds extra request info to the auth object
                        //like IP address and session ID
                );

                //Now tell the Spring this is Authenticated
                SecurityContextHolder.getContext().setAuthentication(authToken);

            }
        }

        //passing the request to the next filter or controller
        filterChain.doFilter(request, response);
    }
}