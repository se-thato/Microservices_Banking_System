package com.banking.gateway.filter;

//import com.bucket4j.Bandwidth;
//import com.bucket4j.Bucket;
//import com.bucket4j.Refill;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import com.banking.gateway.security.JwtTokenExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


// Checks how many requests this user has made recently
// Blocks them with 429 if they exceed their limit
// Each endpoint type has a different limit
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final JwtTokenExtractor jwtTokenExtractor;

    public RateLimitFilter(JwtTokenExtractor jwtTokenExtractor) {
        this.jwtTokenExtractor = jwtTokenExtractor;
    }


    //Bucket Storage Section
    private final Map<String, Bucket> loginBuckets =
            new ConcurrentHashMap<>();
    // login attempts per IP address
    // 5 per minute, this strict to prevent brute force

    private final Map<String, Bucket> registerBuckets =
            new ConcurrentHashMap<>();
    // registration attempts per IP address
    // 3 per minute, prevents spam accounts

    private final Map<String, Bucket> paymentTransferBuckets =
            new ConcurrentHashMap<>();
    // payment transfers per customerId
    // 10 per minute — your requirement

    private final Map<String, Bucket> generalBuckets =
            new ConcurrentHashMap<>();
    //all other endpoints per IP or customerId
    //60 per minute, generous for normal usage


    //Bucket Factories Section
    private Bucket createLoginBucket() {
        //5 login attempts per minute per IP
        //after 5 attempts it gonna be blocked for remainder of minute
        // prevents brute force password attacks
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        5, Refill.greedy(5, Duration.ofMinutes(1))
                ))
                .build();
    }

    private Bucket createRegisterBucket() {
        //3 registration attempts per minute per IP
        //prevents automated spam account creation
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        3, Refill.greedy(3, Duration.ofMinutes(1))
                ))
                .build();
    }

    private Bucket createPaymentTransferBucket() {
        //10 payment attempts per minute per customer
        // so each CUSTOMER gets their own 10/min limit
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        10, Refill.greedy(10, Duration.ofMinutes(1))
                ))
                .build();
    }

    private Bucket createGeneralBucket() {
        // 60 requests per minute
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        60, Refill.greedy(60, Duration.ofMinutes(1))
                ))
                .build();
    }


    //Main filter method
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIp(request);
        //IP address of who is making the request
        //used as bucket key for public endpoints


        //user is given logins for 5 attempts
        if (path.equals("/api/customers/login")
                && method.equals("POST")) {

            Bucket bucket = loginBuckets.computeIfAbsent(
                    ip, k -> createLoginBucket()
            );
            //get existing bucket for this IP
            // or create new one if first time

            if (!bucket.tryConsume(1)) {
                // bucket empty → blocked
                sendRateLimitResponse(response,
                        "Too many login attempts. " +
                                "Please wait 1 minute and try again."
                );
                return;
            }
        }

        //Register: 3 per minute per IP
        else if (path.equals("/api/customers/register")
                && method.equals("POST")) {

            Bucket bucket = registerBuckets.computeIfAbsent(
                    ip, k -> createRegisterBucket()
            );

            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response,
                        "Too many registration attempts. " +
                                "Please wait 1 minute and try again."
                );
                return;
            }
        }

        //PAYMENT TRANSFER: 10 payment transfer per minute per customer
        else if (path.equals("/api/payments/transfer")
                && method.equals("POST")) {

            String customerId = extractCustomerIdFromToken(request);
            // use customerId not IP for authenticated endpoints
            // each customer gets their own 10/min limit
            // not shared with other customers

            String bucketKey = customerId != null
                    ? "customer-" + customerId
                    : "ip-" + ip;
            //if we have customerId then we use it
            //fallback to IP if token not readable

            Bucket bucket = paymentTransferBuckets.computeIfAbsent(
                    bucketKey, k -> createPaymentTransferBucket()
            );

            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response,
                        "Too many payment attempts. " +
                                "You can make maximum 10 payments per minute. " +
                                "Please wait and try again."
                );
                return;
            }
        }

        //OTHER ENDPOINTS: 60 attempts per minute
        else {

            String customerId = extractCustomerIdFromToken(request);
            String bucketKey = customerId != null
                    ? "customer-" + customerId
                    : "ip-" + ip;
            // authenticated requests tracked by customerId
            // unauthenticated requests tracked by IP

            Bucket bucket = generalBuckets.computeIfAbsent(
                    bucketKey, k -> createGeneralBucket()
            );

            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response,
                        "Too many requests. " +
                                "Please wait 1 minute and try again."
                );
                return;
            }
        }

        //Passed rate limit then continue to JwtAuthFilter then proxy
        filterChain.doFilter(request, response);
    }


    //Private helpers section
    private String extractCustomerIdFromToken(HttpServletRequest request) {
        //reads customerId from JWT token
        //returns null if no token or invalid token

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
            // no token then we use IP address instead
        }

        try {
            String token = authHeader.substring(7);
            Long customerId = jwtTokenExtractor.extractCustomerId(token);
            return customerId != null ? customerId.toString() : null;
        } catch (Exception e) {
            return null;
            //invalid token the JWT filter will reject it
        }
    }

    private String getClientIp(HttpServletRequest request) {
        //get real IP address of the caller
        //checks X-Forwarded-For first for proxy/load balancer

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
            //first IP in the chain is the real client
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(
            HttpServletResponse response,
            String message) throws IOException {
        // sends standardised 429 response

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("""
                {
                    "status": 429,
                    "code": "TOO_MANY_REQUESTS",
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