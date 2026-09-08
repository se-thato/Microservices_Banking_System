package com.banking.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;



public class CustomHeaderRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();
    //this stores our added headers
    // e.g. "X-Customer-Id" to "1"

    public CustomHeaderRequestWrapper(HttpServletRequest request) {
        super(request);
        //wrap the original request
    }

    public void addHeader(String name, String value) {
        customHeaders.put(name, value);
        //adding a custom header
    }

    @Override
    public String getHeader(String name) {
        // check our custom headers first
        if (customHeaders.containsKey(name)) {
            return customHeaders.get(name);
        }
        // fall back to original request headers
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        // return both original and custom header names

        java.util.List<String> names = Collections.list(
                super.getHeaderNames()
        );
        // get original header names

        names.addAll(customHeaders.keySet());
        // add our custom header names

        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(
                    Collections.singletonList(customHeaders.get(name))
            );
        }
        return super.getHeaders(name);
    }
}