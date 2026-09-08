package com.banking.gateway.filter;

import com.banking.gateway.security.JwtTokenExtractor;
import com.banking.gateway.security.JwtTokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;


// JwtAuthFilter intercepts EVERY request before it reaches any service
// Validates JWT token and rejects invalid requests with 401
// Public paths (register, login) bypass this check


@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    // OncePerRequestFilter = runs exactly once per request
    // same as we used in other services
    // Gateway MVC uses regular servlet filters

    private final JwtTokenExtractor jwtTokenExtractor;
    private final JwtTokenValidator jwtTokenValidator;

    @Value("${gateway.public.paths}")
    private String publicPathsConfig;
    //"/api/customers/register,/api/customers/login"

    public JwtAuthFilter(JwtTokenExtractor jwtTokenExtractor,
                         JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenExtractor = jwtTokenExtractor;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        // example: "/api/customers/login"

        //first we check if is this a public path?
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            //if the path is a public endpoint skip JWT check
            //pass straight through to service
            return;
        }

        // then checking if is Authorization header present
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(
                    response,
                    "Missing or invalid Authorization header. " +
                            "Please login to get a token"
            );
            return;
            //if no token reject immediately
        }

        //Validating the token
        String token = authHeader.substring(7);
        // remove "Bearer " prefix

        try {
            if (!jwtTokenValidator.isTokenValid(token)) {
                sendUnauthorizedResponse(
                        response,
                        "Token is invalid or has expired. " +
                                "Please login again"
                );
                return;
                // invalid or expired then it should be rejected
            }
        } catch (Exception e) {
            sendUnauthorizedResponse(
                    response,
                    "Token validation failed. Please login again"
            );
            return;
        }

        //Adding customer info as request headers
        //extract claims from token
        //add as headers so downstream services can use them
        //services don't need to validate token themselves

        Long customerId = jwtTokenExtractor.extractCustomerId(token);
        String role = jwtTokenExtractor.extractRole(token);
        String email = jwtTokenExtractor.extractEmail(token);

        // wrap request to add custom headers
        CustomHeaderRequestWrapper modifiedRequest =
                new CustomHeaderRequestWrapper(request);

        if (customerId != null) {
            modifiedRequest.addHeader("X-Customer-Id",
                    customerId.toString());
            // services read customer ID from this header
        }
        modifiedRequest.addHeader("X-Customer-Role", role);
        //services read role from this header
        modifiedRequest.addHeader("X-Customer-Email", email);
        // services read email from this header



        //Pass to next filter, routes to service
        filterChain.doFilter(modifiedRequest, response);
        // passes modified request with added headers
        // Gateway routes it to the correct service
    }

    private boolean isPublicPath(String path) {
        List<String> publicPaths = Arrays.asList(
                publicPathsConfig.split(",")
        );
        return publicPaths.stream()
                .anyMatch(publicPath -> path.equals(publicPath.trim()));
    }

    private void sendUnauthorizedResponse(
            HttpServletResponse response,
            String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //401 status code

        response.setContentType("application/json");

        response.getWriter().write(
                String.format("""
                {
                    "status": 401,
                    "code": "UNAUTHORIZED",
                    "message": "%s",
                    "timestamp": "%s",
                    "details": null
                }
                """,
                        message,
                        LocalDateTime.now()
                )
        );
    }
}