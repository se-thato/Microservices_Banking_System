package com.thato.customer_api.controller;

import com.thato.customer_api.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customers/internal")

//this internal endpoints will be called by BAnking API for pin hash lockout
public class InternalCustomerController {

    private final CustomerService customerService;

    public InternalCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{customerId}/locked")
    public ResponseEntity<Map<String, Boolean>> isLocked(
            @PathVariable Long customerId) {
        //banking api checks this before verifying PIN

        boolean locked = customerService.isAccountLocked(customerId);
        return ResponseEntity.ok(Map.of("locked", locked));
    }



    @PostMapping("/{customerId}/record-failed-pin")
    public ResponseEntity<Void> recordFailedPin(
            @PathVariable Long customerId) {
        //banking api call this when PIN is wrong

        customerService.recordFailedPinAttempts(customerId);
        return ResponseEntity.ok().build();
    }


    @PostMapping
    public ResponseEntity<Void> resetFailedAttempts (
            @PathVariable Long customerId) {
        //this called by banking API when Pin is correct

        customerService.resetFailedAttempts(customerId);
        return ResponseEntity.ok().build();
    }

}
