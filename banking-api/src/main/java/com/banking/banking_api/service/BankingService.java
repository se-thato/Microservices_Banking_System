package com.banking.banking_api.service;

import com.banking.banking_api.client.CustomerClient;
import com.banking.banking_api.dto.*;
import com.banking.banking_api.exception.BusinessException;
import com.banking.banking_api.exception.ResourceNotFoundException;
import com.banking.banking_api.model.*;
import com.banking.banking_api.repository.AccountRepository;
import com.banking.banking_api.repository.CustomerPinRepository;
import com.banking.banking_api.repository.TransactionRepository;
import com.banking.banking_api.util.AccountNumberGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerClient customerClient;
    private final AccessControlService accessControlService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final PasswordEncoder passwordEncoder;
    //so we need password encoder to hash PIN
    private final CustomerPinRepository customerPinRepository;


    public BankingService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          CustomerClient customerClient,
                          AccessControlService accessControlService,
                          AccountNumberGenerator accountNumberGenerator,
                          PasswordEncoder passwordEncoder,
                          CustomerPinRepository customerPinRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.customerClient = customerClient;
        this.accessControlService = accessControlService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.passwordEncoder = passwordEncoder;
        this.customerPinRepository = customerPinRepository;
    }

    //Creating Default Account called by Customer API on registration
    public AccountResponseDTO createDefaultAccount(Long customerId) {
        //this will be called automatically when customer registers their profile
        //create one default SAVINGS account

        String accountNumber = accountNumberGenerator.generate();

        while (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            accountNumber = accountNumberGenerator.generate(); //keep generation until unique
        }

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customerId(customerId)
                .accountType(AccountType.SAVINGS) //always SAVING as default
                .status(AccountStatus.PENDING) //pending until PIN is created
                .balance(BigDecimal.ZERO)
                .currency("ZAR")
                .build();

        Account savedAccount = accountRepository.save(account);

        return convertToAccountDTO(savedAccount, null);
    }

    //creating additional ACCOUNTS, creating more Accounts, e.g BUSINESS account addition
    public AccountResponseDTO createAccount(
            Long customerId, AccountRequestDTO dto, String token) {
        //here a customer creates additional account, after registration
        //SAVINGS is default but can now choose any account

        accessControlService.verifyCustomerAccess(customerId, token);
        //making sure customer is creating their own account not someone else's

        boolean hasPinAlready = customerPinRepository.existsByCustomerId(customerId);
        //checks customer already have PIN being set

        String accountNumber = accountNumberGenerator.generate();
        //this generate unique account numbers

        while (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            accountNumber = accountNumberGenerator.generate();
            // this while loops says keep generating until we get a new unique account number
            //this is for safety check
        }

        String accountHolderName = customerClient
                .getCustomerFullName(customerId, token); //get customer name from customer API

        Account account = Account.builder()
                .accountNumber(accountNumber) //generate unique AccNumber
                .customerId(customerId) // link to customer in customer API
                .accountType(dto.getAccountType())
                .status(hasPinAlready ? AccountStatus.ACTIVE : AccountStatus.PENDING)
                //starts with pending, must create PIN to activate
                //if PIN already set new account is ACTIVE immediately
                .balance(BigDecimal.ZERO)//new account starts with R0.00 account balance
                .currency("ZAR") //South African
                .build();

        Account savedAccount = accountRepository.save(account); //save to db

        return convertToAccountDTO(savedAccount, accountHolderName);
    }


    //setting a bank PIN
    public void setupPin(
            Long customerId,
            PinSetupDTO dto, String token) {
        //here customer create their PIN for the first time, then their account be ACTIVATED

        accessControlService.verifyCustomerAccess(customerId, token); //verify if customer owns this account

        if (!dto.getPin().equals(dto.getConfirmPin())) {
            throw new BusinessException(
                    "PIN_MISMATCH",
                    "PINs do not match. Can you please try again"
            ); //both pin must match
        }

        if (customerPinRepository.existsByCustomerId(customerId)) {
            throw new BusinessException(
                    "PIN_ALREADY_SET",
                    "PIN has already has been set. Use change PIN to update it"
            ); //prevent setting PIN twice
        }


        CustomerPin customerPin = CustomerPin.builder()
                .customerId(customerId)
                .pin(passwordEncoder.encode(dto.getPin()))
                .build();

        customerPinRepository.save(customerPin); //save pin

        //activate all the accounts for this customer
        List<Account> accounts = accountRepository.findByCustomerId(customerId);

        accounts.forEach(account -> {
            account.setStatus(AccountStatus.ACTIVE);
        }); //now all the account become ACTIVE once PIN is set

        accountRepository.saveAll(accounts); //save all accounts with updated status
    }


    //Getting all accounts for a customer
    public List<AccountResponseDTO> getCustomerAccounts(Long customerId, String token) {
        //so this means return all accounts belonging to this customer
        //we also need TOKEN to call Customer API on behalf of the customer

        accessControlService.verifyCustomerAccess(customerId, token);
        //so if someone is trying to access someone else account throw the security exception
        //the if is Admin they can pass through and the owner as well

        String accountHolder = customerClient.getCustomerFullName(customerId, token);
        //calling API to get customer's name
        //this make one call and reuse the name for all accounts

        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(account -> convertToAccountDTO(account, accountHolder))
                .collect(Collectors.toList());
        //convert each account to AccountResponseDTO
    }


    //Getting one account
    public AccountResponseDTO getAccountById(Long accountId, Long customerId, String token) {
        //so now we get a specific account and verifies it belongs to this customer

        accessControlService.verifyCustomerAccess(customerId, token);

        Account account = accountRepository
                .findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "Account not found with id: " + accountId));
        //so if the account does not exist or belong to someone it gives an error

        if (account.getStatus() != AccountStatus.ACTIVE){
            throw new BusinessException(
                    "ACCOUNT_NOT_ACTIVE",
                    "Account is not active");
            //meaning closed or inactive account cannot be accessed
        }

        String accountHolder = customerClient.getCustomerFullName(customerId, token);
        //getting the customer name

        return convertToAccountDTO(account, accountHolder);
        //returning account info including current balance
    }


    //Getting recent transactions(synchronous (less than 5 days))
    public List<TransactionResponseDTO> getRecentTransactions(
            Long accountId, Long customerId, String token) {

        accessControlService.verifyCustomerAccess(customerId, token); //ownership check

        accountRepository.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "Account not found with id: " + accountId));
        //first verify the account belongs to this customer before showing transactions

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(5); //go back 5 days

        List<Transaction> transactions = transactionRepository
                .findRecentTransactions(accountId, cutoffDate);
        //so this calls custom @query that finds transactions newer than cutoffDate

        return transactions.stream()
                .map(this::convertToTransactionDTO)
                .collect(Collectors.toList());
        //converting each transaction to TransactionResponseDTO and return
    }


    //Getting older transactions Asynch (more than 5 days)
    public String requestOlderTransactions(Long accountId, Long customerId, String token) {

        accessControlService.verifyCustomerAccess(customerId, token); //ownership check

        accountRepository.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "Account not found or does not belong to this customer"
                ));
        //this verifies account belongs to this customer before processing

        return "Your transaction history request has been received. " +
                "This may take a few minutes to process. " +
                "Please check back shortly.";
    }


    //Getting Account by Account Number(internal)
    public AccountResponseDTO getAccountByAccountNumber(String accountNumber) {
        //so the Payment api will call this to look an account by using its number

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "Sorry account not found with this number: " + accountNumber
                ));
        return convertToAccountDTO(account, null);
        //null is for accountHolder since internal calls won't need it
    }


    //Now verify PIN(internal)
    public PinVerificationResponseDTO verifyAccountPin(PinVerificationRequestDTO dto) {
        //this will be called by Payment api before processing the payment
        //checks first if the customer entered the correct PIN

        Account account = accountRepository.findById(dto.getAccountId())
                // ☝️ dto.getAccountId() — calling method ON the dto object
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "No Account found with id: " + dto.getAccountId()
                ));

        //find customer's PIN using customerId
        CustomerPin customerPin = customerPinRepository
                .findByCustomerId(account.getCustomerId())
                .orElseThrow(() -> new BusinessException(
                        "PIN_NOT_SET",
                        "No PIN has been created. Please set a PIN first"
                ));
        //PIN is now stored in customer_pins table not on the account
        //one PIN per customer works for ALL their accounts

        boolean isValid = passwordEncoder.matches(
                dto.getPin(),
                // ☝️ dto.getPin() — calling method ON the dto object
                customerPin.getPin()
                //compare the plain pin entered by the customer against customer level PIN
        );

        return PinVerificationResponseDTO.builder()
                .valid(isValid)
                .message(isValid ? "PIN verified successfully"
                        : "Invalid PIN. Please try again")
                .build();
    }


    //Debit accounts(internal)
    public TransactionResponseDTO debitAccount(DebitCreditRequestDTO dto) {
        //so payment api will call this to deduct money from the sender's account

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "Account not found with id: " + dto.getAccountId()
                ));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "ACCOUNT_NOT_ACTIVE",
                    // ☝️ FIXED: was ACCOUNT_NOT_FOUND — wrong error code
                    "Account is not active and can not make transactions"
            );
        }

        if (account.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new BusinessException(
                    "INSUFFICIENT_FUNDS",
                    "Insufficient funds. Available balance: R" + account.getBalance()
            ); // show your available balance before making transaction
        }

        BigDecimal newBalance = account.getBalance()
                .subtract(dto.getAmount());
        //this calculates new balance after deduction

        account.setBalance(newBalance);
        accountRepository.save(account); //update the account balance in DB

        Transaction transaction = Transaction.builder()
                .transactionReference(dto.getTransactionReference())
                .accountId(account.getId())
                .amount(dto.getAmount())
                .transactionType(TransactionType.DEBIT) //money that went out
                .status(TransactionStatus.COMPLETED)
                .description(dto.getDescription())
                .balanceAfter(newBalance) //record new balance after every transaction
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction); //save the transactions record

        return convertToTransactionDTO(savedTransaction);
    }


    //checking credit account records
    public TransactionResponseDTO creditAccount(DebitCreditRequestDTO dto) {
        //this being called by Payment API to add money to receiver account

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "Account not found with id: " + dto.getAccountId()
                ));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "ACCOUNT_NOT_ACTIVE",
                    "Receiver account is not yet active"
            );
        }

        BigDecimal newBalance = account.getBalance()
                .add(dto.getAmount()); //calculating the new balance after addition

        account.setBalance(newBalance);
        accountRepository.save(account); //updating receiver balance

        Transaction transaction = Transaction.builder()
                .transactionReference(dto.getTransactionReference())
                .accountId(account.getId())
                .amount(dto.getAmount())
                .transactionType(TransactionType.CREDIT) //money that comes in
                .status(TransactionStatus.COMPLETED)
                .description(dto.getDescription())
                .balanceAfter(newBalance)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        return convertToTransactionDTO(savedTransaction);
    }


    //private helper section CONVERTING ACCOUNT TO DTO
    private AccountResponseDTO convertToAccountDTO(Account account, String accountHolder) {
        return AccountResponseDTO.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountHolder(accountHolder)
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .build();
    }


    //Converting Transaction To DTO
    private TransactionResponseDTO convertToTransactionDTO(Transaction transaction) {
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .accountId(transaction.getAccountId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .balanceAfter(transaction.getBalanceAfter())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}