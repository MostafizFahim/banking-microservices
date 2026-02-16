package com.banking.account.controller;

import com.banking.account.controller.TransactionController.TransactionSummary;
import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.TransactionDTO;
import com.banking.account.dto.TransactionHistoryRequest;
import com.banking.account.entity.Transaction;
import com.banking.account.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;  // ADD THIS IMPORT
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")


public class TransactionController {

    private final TransactionRepository transactionRepository;

    // Get all transactions for an account
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getAccountTransactions(
            @PathVariable String accountNumber) {
        log.info("Fetching transactions for account: {}", accountNumber);

        List<TransactionDTO> transactions = transactionRepository
                .findByAccountNumberOrderByTimestampDesc(accountNumber)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Transactions retrieved successfully",
                transactions
        ));
    }

    // Get transactions by type (DEPOSIT/WITHDRAWAL)
    @GetMapping("/account/{accountNumber}/type/{type}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByType(
            @PathVariable String accountNumber,
            @PathVariable String type) {
        log.info("Fetching {} transactions for account: {}", type, accountNumber);

        List<TransactionDTO> transactions = transactionRepository
                .findByAccountNumberAndTransactionTypeOrderByTimestampDesc(accountNumber, type)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                type + " transactions retrieved successfully",
                transactions
        ));
    }

    // Get transactions within date range
    @GetMapping("/account/{accountNumber}/daterange")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByDateRange(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Fetching transactions for account: {} from {} to {}",
                accountNumber, startDate, endDate);

        List<TransactionDTO> transactions = transactionRepository
                .findTransactionsByDateRange(accountNumber, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Transactions retrieved successfully",
                transactions
        ));
    }

    // Get single transaction by reference
    @GetMapping("/reference/{reference}")
    public ResponseEntity<ApiResponse<TransactionDTO>> getTransactionByReference(
            @PathVariable String reference) {
        log.info("Fetching transaction by reference: {}", reference);

        return transactionRepository.findById(reference)
                .map(transaction -> ResponseEntity.ok(new ApiResponse<>(
                        true, "Transaction found", convertToDTO(transaction))))
                .orElse(ResponseEntity.notFound().build());
    }

    // Get transaction summary
    @GetMapping("/account/{accountNumber}/summary")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummary(
            @PathVariable String accountNumber) {
        log.info("Fetching transaction summary for account: {}", accountNumber);

        BigDecimal totalDeposits = transactionRepository
                .getTotalByType(accountNumber, "DEPOSIT");
        BigDecimal totalWithdrawals = transactionRepository
                .getTotalByType(accountNumber, "WITHDRAWAL");

        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;
        if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;

        TransactionSummary summary = new TransactionSummary(
                totalDeposits,
                totalWithdrawals,
                totalDeposits.subtract(totalWithdrawals),
                transactionRepository.findByAccountNumberOrderByTimestampDesc(accountNumber).size()
        );

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Summary retrieved successfully", summary
        ));
    }

    // Inner class for summary - FIXED WITH IMPORTS
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TransactionSummary {
        private BigDecimal totalDeposits;
        private BigDecimal totalWithdrawals;
        private BigDecimal netBalance;
        private long totalTransactions;
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .accountNumber(transaction.getAccountNumber())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .reference(transaction.getReference())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}