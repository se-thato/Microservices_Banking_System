package com.banking.gateway.config;

import com.banking.gateway.filter.JwtAuthFilter;
import com.banking.gateway.filter.RateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;


@RestController
@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    private final RestTemplate restTemplate = new RestTemplate();

    public GatewayConfig(JwtAuthFilter jwtAuthFilter,
                         RateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }


    @RequestMapping("/api/customers/**")
    public ResponseEntity<String> customerProxy(
            RequestEntity<String> request,
            HttpServletRequest servletRequest)
            throws URISyntaxException, IOException {

        return forwardRequest(
                request,
                servletRequest,
                "http://localhost:8081"
        );
    }


    @RequestMapping("/api/banking/**")
    public ResponseEntity<String> bankingProxy(
            RequestEntity<String> request,
            HttpServletRequest servletRequest)
            throws URISyntaxException, IOException {

        return forwardRequest(
                request,
                servletRequest,
                "http://localhost:8082"
        );
    }


    @RequestMapping("/api/payments/**")
    public ResponseEntity<String> paymentProxy(
            RequestEntity<String> request,
            HttpServletRequest servletRequest)
            throws URISyntaxException, IOException {

        return forwardRequest(
                request,
                servletRequest,
                "http://localhost:8083"
        );
    }


    private ResponseEntity<String> forwardRequest(
            RequestEntity<String> request,
            HttpServletRequest servletRequest,
            String targetBaseUrl)
            throws URISyntaxException, IOException {

        // building target URL
        String path = servletRequest.getRequestURI();
        String query = servletRequest.getQueryString();
        String targetUrl = targetBaseUrl + path +
                (query != null ? "?" + query : "");

        // copy original headers
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames =
                servletRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.addAll(
                    headerName,
                    Collections.list(
                            servletRequest.getHeaders(headerName)
                    )
            );
        }

        // build forwarded request
        RequestEntity<String> forwardedRequest = new RequestEntity<>(
                request.getBody(),
                headers,
                HttpMethod.valueOf(servletRequest.getMethod()),
                new URI(targetUrl)
        );

        // forward and return response
        return restTemplate.exchange(
                forwardedRequest,
                String.class
        );
    }

    // Jwt Authentication filter
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthFilter> registration =
                new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }


    //Rate limiter filter section
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>();
        registration.setFilter(rateLimitFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}