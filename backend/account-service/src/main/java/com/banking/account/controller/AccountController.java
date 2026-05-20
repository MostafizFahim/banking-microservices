package com.banking.account.controller;

import com.banking.account.dto.AccountDTO;
import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.TransactionRequest;
import com.banking.account.dto.TransactionResult;
import com.banking.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountDTO>> createAccount(@Valid @RequestBody AccountDTO accountDTO) {
        AccountDTO response = accountService.createAccount(accountDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Account created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAllAccounts() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Accounts retrieved successfully",
                accountService.getAllAccounts()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountById(@PathVariable String id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Account found",
                accountService.getAccountById(id)
        ));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Account found",
                accountService.getAccountByNumber(accountNumber)
        ));
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<ApiResponse<AccountDTO>> deposit(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        AccountDTO account = accountService.deposit(accountNumber, amount);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                String.format("Deposited $%.2f successfully", amount),
                account
        ));
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<ApiResponse<AccountDTO>> withdraw(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        AccountDTO account = accountService.withdraw(accountNumber, amount);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                String.format("Withdrew $%.2f successfully", amount),
                account
        ));
    }

    @PostMapping("/transactions")
    public ResponseEntity<ApiResponse<AccountDTO>> processTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResult result = accountService.processTransaction(request);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                String.format("%s of $%.2f completed successfully. Reference: %s",
                        request.getTransactionType(), request.getAmount(), result.getReference()),
                result.getAccount()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable String id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Account deleted successfully", null));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAccountsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Accounts retrieved successfully",
                accountService.getAccountsByStatus(status)
        ));
    }

    @PutMapping("/{accountNumber}/status")
    public ResponseEntity<ApiResponse<AccountDTO>> updateAccountStatus(
            @PathVariable String accountNumber,
            @RequestParam String status) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Account status updated successfully",
                accountService.updateAccountStatus(accountNumber, status)
        ));
    }
}
