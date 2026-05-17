package com.thato.customer_api.repository;

import com.thato.customer_api.model.Customer;
import com.thato.customer_api.model.CustomerStatus;
// ☝️ needed because findByStatus uses CustomerStatus enum

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
// ☝️ needed for the @Query annotation

import org.springframework.data.repository.query.Param;
// ☝️ needed for the @Param annotation inside @Query

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// Better error handling — database errors get translated
// into meaningful Spring exceptions instead of raw SQL errors
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);
    // SELECT * FROM customers WHERE email = ?
    // Returns Optional because the customer might not exist
    // Used in: login, checking if email exists before registering

    boolean existsByEmail(String email);
    // Returns true if email is already taken, false if available
    // Used in: registration — check BEFORE saving to avoid duplicates

    boolean existsByIdNumber(String idNumber);
    // Returns true if this SA ID number is already registered
    // Used in: registration — no two customers can share an ID number

    List<Customer> findByStatus(CustomerStatus status);
    // SELECT * FROM customers WHERE status = ?
    // Returns all customers with that status
    // Used in: admin dashboard — e.g. view all SUSPENDED accounts

    Optional<Customer> findByIdNumber(String idNumber);
    // SELECT * FROM customers WHERE id_number = ?
    // Find a specific customer by their SA ID number

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.email)     LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Customer> searchCustomers(@Param("search") String search);
    // Search customers by first name, last name, or email
    // LIKE '%search%' means "contains this word anywhere"
}