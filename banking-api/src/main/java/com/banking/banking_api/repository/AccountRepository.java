package com.banking.banking_api.repository;

import com.banking.banking_api.model.Account;
import com.banking.banking_api.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByCustomerId(Long customerId);
    //getting ALL accounts belonging to a customer
    //one customer can have muiltiple accounts
    //so now we're returning the list could be one or many

    Optional<Account> findByAccountNumber(String accountNumber);
    //so now we're saying find a specific account by its account number
    //optional coz the account number might not exist

    List<Account> findByCustomerIdAndStatus(Long customerId, AccountStatus status);
    //we're getting all ACTIVE accounts for a customer

    Optional<Account> findByIdAndCustomerId(Long id, Long customerId);
    //so now we're saying find a specific account that belongs to apecific customer
}
