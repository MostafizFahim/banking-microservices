package com.banking.account.controller;

import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.TransactionDTO;
import com.banking.account.dto.TransactionSummary;
import com.banking.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getMyTransactions() {
        List<TransactionDTO> transactions = transactionService.getMyTransactions();
        String message = transactions.isEmpty() ? "No accounts found" : "Transactions retrieved successfully";
        return ResponseEntity.ok(new ApiResponse<>(true, message, transactions));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getAccountTransactions(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Transactions retrieved successfully",
                transactionService.getAccountTransactions(accountNumber)
        ));
    }

    @GetMapping("/account/{accountNumber}/type/{type}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByType(
            @PathVariable String accountNumber,
            @PathVariable String type) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                type + " transactions retrieved successfully",
                transactionService.getTransactionsByType(accountNumber, type)
        ));
    }

    @GetMapping("/account/{accountNumber}/daterange")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByDateRange(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Transactions retrieved successfully",
                transactionService.getTransactionsByDateRange(accountNumber, startDate, endDate)
        ));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<ApiResponse<TransactionDTO>> getTransactionByReference(
            @PathVariable String reference) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Transaction found",
                transactionService.getTransactionByReference(reference)
        ));
    }

    @GetMapping("/account/{accountNumber}/summary")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummary(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Summary retrieved successfully",
                transactionService.getTransactionSummary(accountNumber)
        ));
    }
}
