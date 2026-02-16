package com.banking.account.repository;

import com.banking.account.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;  // ADD THIS IMPORT
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccountNumberOrderByTimestampDesc(String accountNumber);

    List<Transaction> findByAccountNumberAndTransactionTypeOrderByTimestampDesc(
            String accountNumber, String transactionType);

    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber " +
            "AND t.timestamp BETWEEN :startDate AND :endDate ORDER BY t.timestamp DESC")
    List<Transaction> findTransactionsByDateRange(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.accountNumber = :accountNumber " +
            "AND t.transactionType = :type AND t.status = 'COMPLETED'")
    BigDecimal getTotalByType(@Param("accountNumber") String accountNumber,
                              @Param("type") String type);
}