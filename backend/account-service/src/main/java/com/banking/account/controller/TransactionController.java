package com.banking.account.controller;

import com.banking.account.controller.TransactionController.TransactionSummary;
import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.TransactionDTO;
import com.banking.account.dto.TransactionHistoryRequest;
import com.banking.account.entity.Account;
import com.banking.account.entity.Transaction;
import com.banking.account.entity.User;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.TransactionRepository;
import com.banking.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getMyTransactions() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Unauthorized", List.of()));
        }

        List<String> accountNumbers = getVisibleAccountNumbers(user);
        if (accountNumbers.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(true, "No accounts found", List.of()));
        }

        List<TransactionDTO> transactions = transactionRepository
                .findByAccountNumberInOrderByTimestampDesc(accountNumbers)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Transactions retrieved successfully",
                transactions
        ));
    }

    // Get all transactions for an account
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getAccountTransactions(
            @PathVariable String accountNumber) {
        log.info("Fetching transactions for account: {}", accountNumber);

        if (!hasPermission(accountNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", List.of()));
        }

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

        if (!hasPermission(accountNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", List.of()));
        }

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

        if (!hasPermission(accountNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", List.of()));
        }

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

    @GetMapping("/reference/{reference}")
    public ResponseEntity<ApiResponse<TransactionDTO>> getTransactionByReference(
            @PathVariable String reference) {
        log.info("Fetching transaction by reference: {}", reference);

        return transactionRepository.findByReference(reference)
                .filter(transaction -> hasPermission(transaction.getAccountNumber()))
                .map(transaction -> ResponseEntity.ok(new ApiResponse<>(
                        true, "Transaction found", convertToDTO(transaction))))
                .orElse(ResponseEntity.notFound().build());
    }

    // Get transaction summary
    @GetMapping("/account/{accountNumber}/summary")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummary(
            @PathVariable String accountNumber) {
        log.info("Fetching transaction summary for account: {}", accountNumber);

        if (!hasPermission(accountNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", null));
        }

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

    private boolean hasPermission(String accountNumber) {
        User user = getCurrentUser();
        String username = getCurrentUsername();

        if (user == null) return false;
        if ("ADMIN".equals(user.getRole())) return true;

        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> username.equals(account.getOwnerUsername())
                        || accountNumber.equals(user.getAccountNumber()))
                .orElse(false);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    private List<String> getVisibleAccountNumbers(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return accountRepository.findAll()
                    .stream()
                    .map(Account::getAccountNumber)
                    .toList();
        }

        List<String> accountNumbers = new ArrayList<>(accountRepository.findByOwnerUsernameOrderByCreatedAtDesc(user.getUsername())
                .stream()
                .map(Account::getAccountNumber)
                .toList());

        if (accountNumbers.isEmpty() && user.getAccountNumber() != null && !user.getAccountNumber().isBlank()) {
            accountNumbers.add(user.getAccountNumber());
        }

        return accountNumbers;
    }
}
