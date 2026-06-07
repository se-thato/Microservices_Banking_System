package com.banking.banking_api.repository;

import com.banking.banking_api.model.CustomerPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerPinRepository extends JpaRepository<CustomerPin, Long> {

    Optional<CustomerPin> findByCustomerId(Long customerId);
    // Used to find the PIN for a specific customer

    boolean existsByCustomerId(Long customerId);
    // Checks if customer already has a PIN to prevent setting it twice
}