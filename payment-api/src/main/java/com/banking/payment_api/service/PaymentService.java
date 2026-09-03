package com.banking.payment_api.service;

import com.banking.payment_api.client.BankingClient;
import com.banking.payment_api.client.CustomerClient;
import com.banking.payment_api.dto.PaymentReceiptDTO;
import com.banking.payment_api.dto.PaymentRequestDTO;
import com.banking.payment_api.dto.PaymentResponseDTO;
import com.banking.payment_api.exception.AccessDeniedException;
import com.banking.payment_api.exception.BusinessException;
import com.banking.payment_api.exception.ResourceNotFoundException;
import com.banking.payment_api.model.Payment;
import com.banking.payment_api.model.PaymentStatus;
import com.banking.payment_api.model.PaymentType;
import com.banking.payment_api.repository.PaymentRepository;
import com.banking.payment_api.security.JwtTokenExtractor;
import com.banking.payment_api.util.PaymentReferenceGenerator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
// this will handle the complete payment processing flow
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BankingClient bankingClient;
    private final CustomerClient customerClient;
    private final PaymentValidationService paymentValidationService;
    private final PaymentReferenceGenerator referenceGenerator;
    private final JwtTokenExtractor jwtTokenExtractor;


    public PaymentService(PaymentRepository paymentRepository,
                          BankingClient bankingClient,
                          CustomerClient customerClient,
                          PaymentValidationService paymentValidationService,
                          PaymentReferenceGenerator referenceGenerator,
                          JwtTokenExtractor jwtTokenExtractor) {
        this.paymentRepository = paymentRepository;
        this.bankingClient = bankingClient;
        this.customerClient = customerClient;
        this.paymentValidationService = paymentValidationService;
        this.referenceGenerator = referenceGenerator;
        this.jwtTokenExtractor = jwtTokenExtractor;
    }


    public PaymentReceiptDTO processPayment(PaymentRequestDTO dto, Long customerId, String token) {

        //validate reuest format
        paymentValidationService.validatePaymentRequest(dto); //this checks if accounts are not the same, valid amount, branch code

        //Getting sender account from Banking API
        Map<String, Object> senderAccount = bankingClient.getCustomerDefaultAccount(customerId, token);
        //this auto get sender account using the customerId from token

        if (senderAccount == null) {
            throw new BusinessException(
                    "NO_ACTIVE_ACCOUNT",
                    "No active account was found. Please create and activate an account first"
            );
        }
         String fromAccountNumber = (String) senderAccount.get("accountNumber");
        //so this get the acc number from the fetched account, then auto place it under fromAccountNumber field when making payment


        //getting customer full name
        String accountHolderName = customerClient.getCustomerFullName(customerId, token);

        //Validate toAccountNumber is not same as fromAccountNumber
        if (fromAccountNumber.equals(dto.getToAccountNumber())) {
            throw new BusinessException(
                    "SAME_ACCOUNT_NUMBER",
                    "Cannot transfer to your own account"
            );
        }


        //Validating the sender account
        paymentValidationService.validateSenderOwnerShip(senderAccount, customerId);
        //making sure the logged in user owns the owns the sender account

        paymentValidationService.validateAccountIsActive(senderAccount, "Sender"); //ensure account is ACTIVE


        //Getting the receiver account from the Banking API
        Map<String, Object> receiverAccount = bankingClient.getAccountByNumber(
                dto.getToAccountNumber(), token); //receiver account details


        //Now we validate the receiver account, Receiver account must be also be active
        paymentValidationService.validateAccountIsActive(receiverAccount, "Receiver");

        //validate sufficient balance, doest the sender have enough funds?
        paymentValidationService.validateSufficientBalance(senderAccount, dto.getAmount());


        //Verifying the Pin
        Long senderAccountId = getLongValue(senderAccount.get("id")); //extract sender accId from the Map

        boolean pinValid = bankingClient.verifyPin(senderAccountId, dto.getPin(), token);
        //call banking api to verify PIN

        if (!pinValid) {
            throw new BusinessException(
                    "INVALID_PIN",
                    "Invalid PIN. Payment failed"
            );
        }


        //Generating the reference
        String paymentReference = referenceGenerator.generate(); //PAY-20250511-000001
        //shared between debit and credit

        Long receiverAccountId = getLongValue(receiverAccount.get("id")); //extract receiver account Id

        //Save payment as PENDING
        Payment payment = Payment.builder()
                .paymentReference(paymentReference)
                .fromAccountNumber(fromAccountNumber)
                .toAccountNumber(dto.getToAccountNumber())
                .fromAccountId(senderAccountId)
                .toAccountId(receiverAccountId)
                .customerId(customerId)
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .branchCode(dto.getBranchCode())
                .paymentType(PaymentType.SINGLE)
                .status(PaymentStatus.PENDING)
                .transactionReference(paymentReference)
                .build();

        paymentRepository.save(payment);
        //this record the payment BEFORE processing
        //if ever the system crashes, we can see it was attempted


        //Updating the payment to PROCESSING
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        try{
            //debit sender
            Map<String, Object> debitResult = bankingClient.debitAccount(
                 senderAccountId, dto.getAmount(),
                 "Payment to " + dto.getToAccountNumber() + (dto.getDescription() != null
                 ? " - " + dto.getDescription() : ""),
                 paymentReference,token);
            //this will call (POST /api/banking/internal/debit) from BANKING API


            //credit receiver
            bankingClient.creditAccount(
                    receiverAccountId,
                    dto.getAmount(),
                    "Payment from " + fromAccountNumber +
                            (dto.getDescription() != null ? " - " + dto.getDescription() : ""),
                    paymentReference,
                    token
                    //calls POST /api/banking/internal/credit, increasing the receiver balance
            );


            //Updating the payment status to COMPLETE
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);


            //building the receipt
            BigDecimal balanceAfter = getBigDecimalValue(debitResult.get("balanceAfter"));
            //get sender new balance from debit result

            return PaymentReceiptDTO.builder()
                    .paymentReference(paymentReference)
                    .transactionReference(paymentReference)
                    .accountHolderName(accountHolderName)
                    .fromAccountNumber(maskAccountNumber(fromAccountNumber))
                    //masking an account number for security (e.g 16765***87)
                    .toAccountNumber(maskAccountNumber(dto.getToAccountNumber()))
                    .amount(dto.getAmount())
                    .BalanceAfter(balanceAfter)
                    .description(dto.getDescription())
                    .status("Payment Successful")
                    .processedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            //this will handle failure
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment); //record of why payment failed

            throw new BusinessException(
                    "PAYMENT_FAILED",
                    "Payment processing failed: " + e.getMessage()
            );
        }
    }

    public PaymentResponseDTO getPaymentById(Long paymentId,
                                             Long loggedInCustomerId,
                                             String token) {
        //getting a specific payment by ID

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAYMENT_NOT_FOUND",
                        "Payment not found with id: " + paymentId
                ));

        //ownership check
        String role = jwtTokenExtractor.extractRole(token);
        // read role from token: "ROLE_CUSTOMER" or "ROLE_ADMIN"

        boolean isAdmin = "ROLE_ADMIN".equals(role);
        // admin can see payment made by customers

        boolean isOwner = payment.getCustomerId().equals(loggedInCustomerId);
        //user will only see their payments only

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "ACCESS_DENIED",
                    "You can only view your own payment details"
            );
            // if not admin and not owner, will then throw this exception 403 access denied
        }

        return convertToResponseDTO(payment);
    }


    //this will be private helpers
    private String maskAccountNumber(String accountNumber) {
        // mask middle digis for security purposes

        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        return accountNumber.substring(0, 5) + "*****" + accountNumber.substring(accountNumber.length() - 3);

    }

    private Long getLongValue(Object value) {
        //JSON number come back as Integer from RestTemplate

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long) {
            return (Long) value;
        }

        return null;
    }


    private  BigDecimal getBigDecimalValue(Object value) {
        //converting Map value to BigDecimal
        if(value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private PaymentResponseDTO convertToResponseDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .fromAccountNumber(payment.getFromAccountNumber())
                .toAccountNumber(payment.getToAccountNumber())
                .amount(payment.getAmount())
                .description(payment.getDescription())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .transactionReference(payment.getTransactionReference())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();

    }
}
