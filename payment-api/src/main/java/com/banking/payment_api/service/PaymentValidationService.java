package com.banking.payment_api.service;

import com.banking.payment_api.dto.PaymentRequestDTO;
import com.banking.payment_api.exception.BusinessException;
import com.banking.payment_api.util.BranchCodeValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
//this class handles all payment validation rules
public class PaymentValidationService {

    private final BranchCodeValidator branchCodeValidator;

    public PaymentValidationService(BranchCodeValidator branchCodeValidator) {
        this.branchCodeValidator = branchCodeValidator;
    }

    private static final BigDecimal MAX_SINGLE_TRANSACTION = new BigDecimal("100000.00");
    //this will be a maximum amaunt for single payment = R100,000


    public void validatePaymentRequest(PaymentRequestDTO dto) {
        //runs all validation rules on the payment request
        //throws businesexception if any rule fails

        validateAccountNumbersNotSame(dto);
        validateAmount(dto.getAmount());
        validateBranchCode(dto.getBranchCode());
    }

    //Validate Account Numbers if Not the same
    private void validateAccountNumbersNotSame(PaymentRequestDTO dto) {
        //this avoid sender and reveiver to be the same, so you can not send it to yourself

    }


    //Validating the amount
    private void validateAmount(BigDecimal amount) {
        //this ensures or checks if the amount is above zero, abd canot exceed single transaction limit

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "INVALID_AMOUNT",
                    "Payment amount must be greater than zero"
            );
        }

        if(amount.compareTo(MAX_SINGLE_TRANSACTION) > 0) {
            throw new BusinessException(
                    "AMOUNT_EXCEEDS_LIMIT",
                    "Payment amount exceeds maximum limit of R" + MAX_SINGLE_TRANSACTION
            );
        }
    }


    //Validating the bank Branch Code
    private void validateBranchCode(String branchCode) {
        //branch code must be valid SA Bank code, only validated when provided since this part is opt

        if (!branchCodeValidator.isValid(branchCode)) {
            throw new BusinessException(
                    "INVALID_BRANCH_CODE",
                    "Invalid branch code: " + branchCode + " . Please provide a valid SA bank branch code"
            );
        }
    }


    //validating the account ownership
    public void validateSenderOwnerShip(
            Map<String, Object> senderAccount, Long loggedInCustomerId) {
        //this ensure the logged in user owns the account

        Object accountCustomerId = senderAccount.get("customerId");
        Long senderCustomerId = null;

        if (accountCustomerId instanceof Integer) {
            senderCustomerId = ((Integer) accountCustomerId).longValue();
        }else if (accountCustomerId instanceof Long) {
            senderCustomerId = (Long) accountCustomerId;
        }

        if (!loggedInCustomerId.equals(senderCustomerId)) {
            throw new BusinessException(
                    "ACCOUNT_VALIDATION_OWNERSHIP",
                    "You can only make payments from your own account"
            );
        }
    }


    //validating if the account is active, meaning the PIN has been set for this account
    public void validateAccountIsActive(
            Map<String, Object> account, String accountType) {

        String status = (String) account.get("status");

        if (!"ACTIVE".equals(status)) {
            throw new BusinessException(
                    "ACCOUNT_NOT_VALID",
                    accountType + " account is not active. " + "Current status is: " + status
            );
        }
    }


    //validating if the is enough balance/funds in your account to make payment
    public void validateSufficientBalance(
            Map<String, Object> senderAccount, BigDecimal amount) {
                //check is user has enough money

        Object balanceObj = senderAccount.get("balance");
        BigDecimal balance = new BigDecimal(balanceObj.toString());

        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(
                    "INSUFFICIENT_FUNDS",
                    "Insufficient funds. Available balance: R" + balance
            );
        }
    }
}
