package com.banking.banking_api.Service;

import com.banking.banking_api.client.CustomerClient;
import com.banking.banking_api.dto.AccountResponseDTO;
import com.banking.banking_api.dto.TransactionResponseDTO;
import com.banking.banking_api.model.Account;
import com.banking.banking_api.model.AccountStatus;
import com.banking.banking_api.model.Transaction;
import com.banking.banking_api.repository.AccountRepository;
import com.banking.banking_api.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerClient customerClient;

    public BankingService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          CustomerClient customerClient) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.customerClient = customerClient;
    }


    //Getting all accounts for a customer
    public List<AccountResponseDTO> getCustomerAccounts(Long customerId, String token) {
        //so this means return all accounts belonging to this customer
        //we also need TOKEN to call Customer API on behalf of the customer

        String accountHolder = customerClient.getCustomerFullName(customerId, token); //calling API to get customer's name
        //this make one call and resuse the name for all accounts

        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(account -> convertToAccountDTO(account, accountHolder))
                .collect(Collectors.toList());
            //convert each account to AccountResponseDTO
    }



    //Getting one account BALANCE
    public AccountResponseDTO getAccountById(Long accountId, Long customerId, String token) {
        //so now we get a specific account and verifies it belongs to this customer

        Account account = accountRepository
                .findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new RuntimeException("Account not found or does not belong to this customer"));
            //so if the accound does not exist or belong to someone it gives an error

        if (account.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Account is not active");
            //meaning closed or inactive account cannot be accessed
        }

        String accountHolder = customerClient.getCustomerFullName(customerId, token); //getting the customer name

        return convertToAccountDTO(account, accountHolder);
        //returning accont info including current balance
    }




    //Getting recent transactions(synchronous (less than 5 days))
    public List<TransactionResponseDTO> getRecentTransactions(
            Long accountId, Long customerId) {

        accountRepository.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new RuntimeException("Opps Account not found or does not belong to this customer"));
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
    public String requestOlderTransactions(Long accountId, Long customerId) {

        accountRepository.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new RuntimeException(
                        "Account not found or does not belong to this customer"
                ));
        //this verifies account belongs to this customer before processing

        return "Your transaction history request has been received. " +
                "This may take a few minutes to process. " +
                "Please check back shortly.";
    }



    //private helper section CONVERTING ACCOUT TO DTO
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


    //Converting Account To DTO
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
