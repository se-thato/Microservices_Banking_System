package com.banking.payment_api.controller;

import com.banking.payment_api.dto.*;
import com.banking.payment_api.exception.UnauthorizedException;
import com.banking.payment_api.kafka.BulkPaymentProducer;
import com.banking.payment_api.model.PaymentStatus;
import com.banking.payment_api.repository.PaymentRepository;
import com.banking.payment_api.security.JwtTokenExtractor;
import com.banking.payment_api.service.BulkPaymentService;
import com.banking.payment_api.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtTokenExtractor jwtTokenExtractor;
    //extract customer id from token
    private final BulkPaymentService bulkPaymentService;
    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentService paymentService,
                             JwtTokenExtractor jwtTokenExtractor,
                             BulkPaymentService bulkPaymentService,
                             PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.jwtTokenExtractor = jwtTokenExtractor;
        this.bulkPaymentService = bulkPaymentService;
        this.paymentRepository = paymentRepository;
    }

    //customer initiating the payment
    @PostMapping("/transfer")
    public ResponseEntity<PaymentReceiptDTO> transfer(
            
            @Valid @RequestBody PaymentRequestDTO dto,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);

        Long customerId = jwtTokenExtractor.extractCustomerId(token);

        if (customerId == null) {
            throw new UnauthorizedException(
                    "IDENTITY_VERIFICATION_FAILED",
                    "Could not verify your identity. Please try to login again"
            );
        }

        PaymentReceiptDTO receipt = paymentService.processPayment(dto, customerId, token);

        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
        //new record is created
    }


    //getting payment status and details
    @GetMapping("status/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPayment(
            @PathVariable Long paymentId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);

        Long customerId = jwtTokenExtractor.extractCustomerId(token);

        if (customerId == null) {
            throw new UnauthorizedException(
                    "IDENTITY_VERIFICATION_FAILED",
                    "Could not verify your identity. Please try to login again"
            );
        }

        PaymentResponseDTO payment = paymentService.getPaymentById(paymentId,customerId,token);

        return ResponseEntity.ok(payment);
    }

    //making bulk payments
    //example: companies starting a salary run
    @PostMapping("/bulk")
    public ResponseEntity<BulkPaymentResponseDTO> bulkTransfer(
            @Valid @RequestBody BulkPaymentRequestsDTO dto,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long customerId = jwtTokenExtractor.extractCustomerId(token);

        if (customerId == null) {
            throw new UnauthorizedException(
                    "IDENTITY_VERIFICATION_FAILED",
                    "Could not verify your identity. Please login again"
            );
        }

        BulkPaymentResponseDTO response = bulkPaymentService.processBulkPayment(dto, customerId, token);
        //this publishes all payments to kafka and returns immediately, which all will happen in the background

        return ResponseEntity.accepted().body(response);
        //if i get 202 ACCEPTED meaning the request is received, processing in the background
    }


    //GET/api/payments/batch/{batchId}
    //checking progress of bulk payment batch
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<?> getBatchStatus(
            @PathVariable String batchId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long customerId = jwtTokenExtractor.extractCustomerId(token);

        //caunting payments by status for batch
        long total = paymentRepository
                .countByTransactionReferenceStartingWith(batchId);
        long completed = paymentRepository
                .countByTransactionReferenceStartingWithAndStatus(batchId, PaymentStatus.COMPLETED);
        long failed = paymentRepository
                .countByTransactionReferenceStartingWithAndStatus(batchId, PaymentStatus.FAILED);

        long pending = total - completed - failed;

        return ResponseEntity.ok(Map.of(
                "batchId", batchId,
                "totalPayment", total,
                "completed", completed,
                "pending", pending,
                "status", pending == 0 ? "COMPLETED" : "PROCESSING"
        ));
    }

}
