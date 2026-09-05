package com.banking.payment_api.service;

import com.banking.payment_api.client.BankingClient;
import com.banking.payment_api.dto.*;
import com.banking.payment_api.exception.BusinessException;
import com.banking.payment_api.kafka.BulkPaymentProducer;
import com.banking.payment_api.model.Payment;
import com.banking.payment_api.model.PaymentStatus;
import com.banking.payment_api.model.PaymentType;
import com.banking.payment_api.repository.PaymentRepository;
import com.banking.payment_api.security.JwtTokenExtractor;
import com.banking.payment_api.util.PaymentReferenceGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


// BulkPaymentService will handles salary that runs and batch payments
// Key difference from single payment:
// Single payment → HTTP call → wait for result → return receipt
// Bulk payment will validate then publish to Kafka and then return batchId immediately
// background consumers process each payment


@Service
public class BulkPaymentService {

    private final BankingClient bankingClient;
    private final PaymentRepository paymentRepository;
    private final BulkPaymentProducer bulkPaymentProducer;
    private final PaymentReferenceGenerator referenceGenerator;
    private final JwtTokenExtractor jwtTokenExtractor;
    private final PaymentValidationService validationService;

    public BulkPaymentService(
            BankingClient bankingClient,
            PaymentRepository paymentRepository,
            BulkPaymentProducer bulkPaymentProducer,
            PaymentReferenceGenerator referenceGenerator,
            JwtTokenExtractor jwtTokenExtractor,
            PaymentValidationService validationService) {
        this.bankingClient = bankingClient;
        this.paymentRepository = paymentRepository;
        this.bulkPaymentProducer = bulkPaymentProducer;
        this.referenceGenerator = referenceGenerator;
        this.jwtTokenExtractor = jwtTokenExtractor;
        this.validationService = validationService;
    }


    public BulkPaymentResponseDTO processBulkPayment(
            BulkPaymentRequestsDTO dto, Long customerId, String token) {

        //We first Get sender's account
        Map<String, Object> senderAccount =
                bankingClient.getCustomerDefaultAccount(customerId, token);
        // auto-fetch sender account — same as single payment

        if (senderAccount == null) {
            throw new BusinessException(
                    "NO_ACTIVE_ACCOUNT",
                    "No active account found. " +
                            "Please create and activate an account first"
            );
        }

        Long senderAccountId = getLongValue(senderAccount.get("id"));
        String fromAccountNumber =
                (String) senderAccount.get("accountNumber");

        // Verify PIN ONCE for the entire batch
        boolean pinValid = bankingClient.verifyPin(
                senderAccountId, dto.getPin(), token
        );
        // ONE PIN verification covers ALL payments in the batch
        // company authorises the entire salary run with one PIN

        if (!pinValid) {
            throw new BusinessException(
                    "INVALID_PIN",
                    "Invalid PIN. Bulk payment rejected"
            );
        }

        // Then we validate all payment items
        List<String> validationErrors = new ArrayList<>();


        for (int i = 0; i < dto.getPayments().size(); i++) {
            BulkPaymentItemDTO item = dto.getPayments().get(i);

            try {
                validationService.validateAmount(item.getAmount());
                // checking amount is valid for each payment

                if (fromAccountNumber.equals(item.getToAccountNumber())) {
                    validationErrors.add(
                            "Payment " + (i + 1) + ": " +
                                    "Cannot pay to your own account"
                    );
                }
            } catch (BusinessException e) {
                validationErrors.add(
                        "Payment " + (i + 1) + ": " + e.getMessage()
                );
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new BusinessException(
                    "BULK_VALIDATION_FAILED",
                    "Bulk payment validation failed: " +
                            String.join(", ", validationErrors)
            );
            // reject entire batch if ANY item has errors
        }

        // Generating batch ID
        String batchId = "BATCH-" +
                java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter
                                .ofPattern("yyyyMMdd-HHmmss"));
        // e.g. "BATCH-20240328-143022"
        // unique ID for this entire salary run

        // Saving all payments as PENDING and publish to Kafka
        int itemNumber = 0;

        for (BulkPaymentItemDTO item : dto.getPayments()) {
            itemNumber++;

            String paymentReference = batchId + "-" +
                    String.format("%03d", itemNumber);
            // e.g. "BATCH-20240328-143022-001"
            // unique reference per payment within the batch

            // Save payment record as PENDING
            Payment payment = Payment.builder()
                    .paymentReference(paymentReference)
                    .fromAccountNumber(fromAccountNumber)
                    .toAccountNumber(item.getToAccountNumber())
                    .fromAccountId(senderAccountId)
                    .customerId(customerId)
                    .amount(item.getAmount())
                    .description(item.getDescription() != null
                            ? item.getDescription()
                            : dto.getBatchDescription())
                    .paymentType(PaymentType.BULK)
                    .status(PaymentStatus.PENDING)
                    .transactionReference(paymentReference)
                    .build();

            paymentRepository.save(payment);
            // saved to database before publishing to Kafka
            // if Kafka fails we can then see what was attempted

            // Publishing to Kafka for async processing
            BulkPaymentEventDTO event = BulkPaymentEventDTO.builder()
                    .batchId(batchId)
                    .paymentReference(paymentReference)
                    .fromAccountId(senderAccountId)
                    .fromAccountNumber(fromAccountNumber)
                    .toAccountNumber(item.getToAccountNumber())
                    .amount(item.getAmount())
                    .description(item.getDescription() != null
                            ? item.getDescription()
                            : dto.getBatchDescription())
                    .customerId(customerId)
                    .token(token) //pass token so Banking API consumer can authenticate
                    .build();

            bulkPaymentProducer.publishPaymentEvent(event);
            // publishes to Kafka topic "payment.bulk.initiated"
            // Banking API consumer picks this up and processes it
            // this is will happen INSTANT we don't wait for processing
        }

        //Return immediately with batchId
        return BulkPaymentResponseDTO.builder()
                .batchId(batchId)
                .totalPayments(dto.getPayments().size())
                .status("PROCESSING")
                .message(dto.getPayments().size() + " payments queued " +
                        "for processing. Use batchId '" + batchId +
                        "' to track progress.")
                .submittedAt(LocalDateTime.now())
                .build();
        // returned IMMEDIATELY customer doesn't wait
        // payments are processing in background via Kafka
    }


    private Long getLongValue(Object value) {
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Long) return (Long) value;
        return null;
    }
}