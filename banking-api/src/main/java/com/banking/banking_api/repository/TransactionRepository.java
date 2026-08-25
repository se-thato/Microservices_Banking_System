package com.banking.banking_api.repository;

import com.banking.banking_api.model.Transaction;
import com.banking.banking_api.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    //getting All transaction for an account
    //OrderByCreatedAtDesc means get newest first, like a bank statement - most recent at the top

    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
            "AND t.createdAt >= :cutoffDate ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactions(
            @Param("accountId") Long accountId,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );
    //getting transation NEWER than cutoffDate (synchronous - less than 5 days)



    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
            "AND t.createdAt < :cutoffDate ORDER BY t.createdAt DESC")
    List<Transaction> findOlderTransactions(
            @Param("accountId") Long accountId,
            @Param("cutoffDate")LocalDateTime cutoffDate
    );
    //so now we're getting transactions OLDER than cutoffDate meaning Async more than 5 days


    Optional<Transaction> findByTransactionReference(String reference);
    //finding a specific transaction by its reference number
    //used when customer asks a specific transaction

    List<Transaction> findByAccountIdAndStatus(Long accountId, TransactionStatus status);
    //find all transaction with a specific status for an account

}
