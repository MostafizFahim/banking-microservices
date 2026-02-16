package com.banking.account.controller;

import com.banking.account.dto.AccountDTO;
import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.TransactionRequest;
import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.account.entity.Transaction;
import com.banking.account.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")


public class AccountController {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // Create new account
    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody AccountDTO accountDTO) {
        log.info("Creating new account: {}", accountDTO.getAccountNumber());

        // Check if account already exists
        if (accountRepository.existsByAccountNumber(accountDTO.getAccountNumber())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Account number already exists", null));
        }

        if (accountRepository.existsByEmail(accountDTO.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Email already registered", null));
        }

        // Convert DTO to Entity
        Account account = new Account();
        account.setAccountNumber(accountDTO.getAccountNumber());
        account.setAccountHolderName(accountDTO.getAccountHolderName());
        account.setEmail(accountDTO.getEmail());
        account.setBalance(accountDTO.getBalance());
        account.setAccountType(accountDTO.getAccountType());
        account.setStatus("ACTIVE");

        // Save to database
        Account savedAccount = accountRepository.save(account);

        // Convert back to DTO for response
        AccountDTO response = convertToDTO(savedAccount);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Account created successfully", response));
    }

    // Get all accounts
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAllAccounts() {
        log.info("Fetching all accounts");
        List<AccountDTO> accounts = accountRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, "Accounts retrieved successfully", accounts));
    }

    // Get account by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountById(@PathVariable String id) {
        log.info("Fetching account by id: {}", id);

        return accountRepository.findById(id)
                .map(account -> ResponseEntity.ok(new ApiResponse<>(true, "Account found", convertToDTO(account))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Account not found", null)));
    }

    // Get account by account number
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountByNumber(@PathVariable String accountNumber) {
        log.info("Fetching account by number: {}", accountNumber);

        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> ResponseEntity.ok(new ApiResponse<>(true, "Account found", convertToDTO(account))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Account not found", null)));
    }

    // Deposit money
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<ApiResponse<AccountDTO>> deposit(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {

        log.info("Depositing {} to account: {}", amount, accountNumber);

        java.util.Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);

        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        Account account = accountOpt.get();
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);

        account.setBalance(newBalance);
        Account updatedAccount = accountRepository.save(account);

        // Record transaction
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setDescription("Deposit to account");
        transaction.setStatus("COMPLETED");
        transactionRepository.save(transaction);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                String.format("Deposited $%.2f successfully. Reference: %s",
                        amount, transaction.getReference()),
                convertToDTO(updatedAccount)
        ));
    }

    // Withdraw money
    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<ApiResponse<AccountDTO>> withdraw(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {

        log.info("Withdrawing {} from account: {}", amount, accountNumber);

        java.util.Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);

        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        Account account = accountOpt.get();

        if (account.getBalance().compareTo(amount) < 0) {
            // Record failed transaction
            Transaction failedTransaction = new Transaction();
            failedTransaction.setAccountId(account.getId());
            failedTransaction.setAccountNumber(account.getAccountNumber());
            failedTransaction.setTransactionType("WITHDRAWAL");
            failedTransaction.setAmount(amount);
            failedTransaction.setBalanceAfter(account.getBalance());
            failedTransaction.setDescription("Failed - Insufficient funds");
            failedTransaction.setStatus("FAILED");
            transactionRepository.save(failedTransaction);

            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Insufficient funds", null));
        }

        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.subtract(amount);

        account.setBalance(newBalance);
        Account updatedAccount = accountRepository.save(account);

        // Record successful transaction
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setDescription("Withdrawal from account");
        transaction.setStatus("COMPLETED");
        transactionRepository.save(transaction);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                String.format("Withdrew $%.2f successfully. Reference: %s",
                        amount, transaction.getReference()),
                convertToDTO(updatedAccount)
        ));
    }

    // Process transaction
    @PostMapping("/transactions")
    public ResponseEntity<ApiResponse<AccountDTO>> processTransaction(
            @Valid @RequestBody TransactionRequest request) {

        log.info("Processing transaction: {}", request);

        java.util.Optional<Account> accountOpt = accountRepository.findByAccountNumber(request.getAccountNumber());

        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        Account account = accountOpt.get();
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription() != null ?
                request.getDescription() : request.getTransactionType() + " transaction");

        if ("DEPOSIT".equals(request.getTransactionType())) {
            BigDecimal newBalance = account.getBalance().add(request.getAmount());
            account.setBalance(newBalance);
            transaction.setBalanceAfter(newBalance);
            transaction.setStatus("COMPLETED");

        } else if ("WITHDRAWAL".equals(request.getTransactionType())) {
            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                transaction.setBalanceAfter(account.getBalance());
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);

                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Insufficient funds", null));
            }

            BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
            account.setBalance(newBalance);
            transaction.setBalanceAfter(newBalance);
            transaction.setStatus("COMPLETED");

        } else {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Invalid transaction type", null));
        }

        Account updatedAccount = accountRepository.save(account);
        transactionRepository.save(transaction);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                String.format("%s of $%.2f completed successfully. Reference: %s",
                        request.getTransactionType(), request.getAmount(), transaction.getReference()),
                convertToDTO(updatedAccount)
        ));
    }

    // Delete account
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable String id) {
        log.info("Deleting account: {}", id);

        if (!accountRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        accountRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Account deleted successfully", null));
    }

    // Get accounts by status
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAccountsByStatus(@PathVariable String status) {
        log.info("Fetching accounts with status: {}", status);

        List<AccountDTO> accounts = accountRepository.findByStatus(status)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, "Accounts retrieved successfully", accounts));
    }

    // Helper method to convert Entity to DTO
    private AccountDTO convertToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountHolderName(account.getAccountHolderName());
        dto.setEmail(account.getEmail());
        dto.setBalance(account.getBalance());
        dto.setAccountType(account.getAccountType());
        dto.setStatus(account.getStatus());
        return dto;
    }
}