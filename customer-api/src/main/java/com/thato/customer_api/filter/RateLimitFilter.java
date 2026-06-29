package com.thato.customer_api.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
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

@Component
//this will check if the caller has exceeded their allowed request rate
public class RateLimitFilter extends OncePerRequestFilter {

    //bucket storage
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    //stores one bucket per IP address for login
    //can also handle  simulteneous requests with data being corrupted

    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    //this separate buckets for register endpoint

    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>(); //this bucket is for all endpoints


    //Bucket facktories
    private Bucket createLoginBucket() {
        //5 login attempts per minutes

        Bandwidth limit = Bandwidth.classic(
                5, //the buckets will only hold 5 token max
                Refill.greedy(5, Duration.ofMinutes(1))
                //meaning the tokens will then be refilled in the bucket after 1 minute
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createRegisterBucket() {
        //here we want 3 registration attempts per minutes per IP, preventing spam account creation

        Bandwidth limit = Bandwidth.classic(
                3,
                Refill.greedy(3, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }


    private Bucket createGeneralBucket() {
        //30 requests per minute per IP, any request for all endpoints

        Bandwidth limit = Bandwidth.classic(
                30,
                Refill.greedy(30, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }


    //Main filter section impl
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        String ip = getClientIp(request);
        //checking if who is making this request, each IP address will get it own bucket


        //Login Rate Limit
        if (path.equals("/api/customers/login")) {

            Bucket bucket = loginBuckets.computeIfAbsent(
                    ip,
                    key -> createLoginBucket()
                    //by ComputeifAbsent i mean get existing bucket or create a new one if its IP first time call
            );

            if (!bucket.tryConsume(1)) {
                //meaning if not try to take 1 token from bucket
                //true = token was successfully so allow request

                sendRateLimitResponse(
                        response,
                        "Too many login attempts. Please wait 1 minute and try again."
                );
                return;
            }
        }

        //Register rate limit
        else if (path.equals("/api/customers/register")) {

            Bucket bucket = registerBuckets.computeIfAbsent(
                    ip,
                    key -> createRegisterBucket()
            );

            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(
                        response,
                        "Too many registration attempts. Please wait 1 minute and try again"
                );
                return;
            }
        }

        //General rate limit, 30 per minutes
        else if (path.startsWith("/api/customers")) {

            Bucket bucket = generalBuckets.computeIfAbsent(
                    ip,
                    key -> createGeneralBucket()
            );

            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(
                        response,
                        "Too many request. Please wait 1 minute and try again"
                );
                return;
            }
        }

        //carry on to the next controller or filter
        filterChain.doFilter(request, response);
    }

    //rivate helpers
    private String getClientIp(HttpServletRequest request) {
        //check the real IP address of the caller

        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {

            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(
            HttpServletResponse response,
            String message
    ) throws IOException {
        //send 429 JSON response

        response.setStatus(429); //429 response = too many request

        response.setContentType("application/json"); //tells a client this is JSON not HTML

        response.getWriter().write(
                String.format("""
                    {
                        "status": 429,
                        "code": "TOO_MANY_REQUESTS",
                        "message": "%s",
                        "timestamp":"%s",
                        "details": null
                    }
                    """,
                        message,
                        LocalDateTime.now()
                )
        );
    }
}