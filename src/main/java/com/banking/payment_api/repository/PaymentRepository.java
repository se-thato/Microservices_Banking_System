package com.banking.payment_api.repository;

import com.banking.payment_api.model.Payment;
import com.banking.payment_api.model.PaymentStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);
    //must firnd a specific payment by its reference number
    //this is used to check if payment already exists, prevent duplication


    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    //get all the payments made by a specific customer
    //in a Descending order - newest first, like a payment history statement


    List<Payment> findByFromAccountNumberOrderByCreatedAtDesc(String fromAccountNumber);
    //getting all the paymets from a specific account Number, for payment history


    List<Payment> findByStatus(PaymentStatus status);
    //getting all payments with specific payment status, e.g find all PENDING payment for retry


    boolean existsByPaymentReference(String paymentReference); //checks if payment reference already exists

    //counting total payments in a batch
    long countByTransactionReferenceStartingWith(String batchId); //counts all payments where reference starts with BatchId

    //counting payments with specific status in batch
    long countByTransactionReferenceStartingWithAndStatus(String batchId, PaymentStatus status);
}
