package com.banking.account.controller;

import com.banking.account.dto.AccountDTO;
import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.TransactionRequest;
import com.banking.account.entity.Account;
import com.banking.account.entity.User;
import com.banking.account.repository.AccountRepository;
import com.banking.account.entity.Transaction;
import com.banking.account.repository.TransactionRepository;
import com.banking.account.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // Create new account
    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody AccountDTO accountDTO) {
        String accountNumber = resolveAccountNumber(accountDTO.getAccountNumber());
        log.info("Creating new account: {}", accountNumber);

        if (accountRepository.existsByAccountNumber(accountNumber)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Account number already exists", null));
        }

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountHolderName(accountDTO.getAccountHolderName());
        account.setEmail(accountDTO.getEmail());
        account.setBalance(accountDTO.getBalance());
        account.setAccountType(accountDTO.getAccountType());
        account.setOwnerUsername(getCurrentUsername());
        account.setStatus("ACTIVE");

        Account savedAccount = accountRepository.save(account);

        // Link account to current user
        String username = getCurrentUsername();
        User user = getCurrentUser();
        if (user != null && (user.getAccountNumber() == null || user.getAccountNumber().isEmpty())) {
            user.setAccountNumber(savedAccount.getAccountNumber());
            userRepository.save(user);
            log.info("Linked account {} to user {}", savedAccount.getAccountNumber(), username);
        }

        AccountDTO response = convertToDTO(savedAccount);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Account created successfully", response));
    }

    // Get all accounts - ADMIN sees all, CUSTOMER sees only theirs
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAllAccounts() {
        log.info("Fetching all accounts");

        String username = getCurrentUsername();
        User user = getCurrentUser();

        List<AccountDTO> accounts;

        if (user != null && "ADMIN".equals(user.getRole())) {
            // ADMIN: See all accounts
            accounts = accountRepository.findAll()
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            log.info("ADMIN {} viewing all {} accounts", username, accounts.size());
        } else if (user != null) {
            accounts = findCustomerAccounts(user)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            log.info("CUSTOMER {} viewing {} account(s)", username, accounts.size());
        } else {
            accounts = List.of();
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Accounts retrieved successfully", accounts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountById(@PathVariable String id) {
        log.info("Fetching account by id: {}", id);

        java.util.Optional<Account> accountOpt = accountRepository.findById(id);

        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        Account account = accountOpt.get();

        if (!hasPermission(account.getAccountNumber())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Account found", convertToDTO(account)));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountByNumber(@PathVariable String accountNumber) {
        log.info("Fetching account by number: {}", accountNumber);

        java.util.Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);

        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        Account account = accountOpt.get();

        if (!hasPermission(accountNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Account found", convertToDTO(account)));
    }

    private boolean hasPermission(String accountNumber) {
        String username = getCurrentUsername();
        User user = getCurrentUser();

        if (user == null) return false;
        if ("ADMIN".equals(user.getRole())) return true;

        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> username.equals(account.getOwnerUsername())
                        || accountNumber.equals(user.getAccountNumber()))
                .orElse(false);
    }

    // Deposit money
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<ApiResponse<AccountDTO>> deposit(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {

        log.info("Depositing {} to account: {}", amount, accountNumber);

        if (!hasPermission(accountNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", null));
        }

        java.util.Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);

        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        Account account = accountOpt.get();

        if ("FROZEN".equals(account.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Account is frozen. Cannot deposit.", null));
        }

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);
        saveTransaction(updatedAccount, "DEPOSIT", amount, updatedAccount.getBalance(), "Deposit transaction", "COMPLETED");

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                String.format("Deposited $%.2f successfully", amount),
                convertToDTO(updatedAccount)
        ));
    }

    // Withdraw money
    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<ApiResponse<AccountDTO>> withdraw(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {

        log.info("Withdrawing {} from account: {}", amount, accountNumber);

        if (!hasPermission(accountNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", null));
        }

        java.util.Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);

        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        Account account = accountOpt.get();

        if ("FROZEN".equals(account.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Account is frozen. Cannot withdraw.", null));
        }

        if (account.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Insufficient funds", null));
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account updatedAccount = accountRepository.save(account);
        saveTransaction(updatedAccount, "WITHDRAWAL", amount, updatedAccount.getBalance(), "Withdrawal transaction", "COMPLETED");

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                String.format("Withdrew $%.2f successfully", amount),
                convertToDTO(updatedAccount)
        ));
    }

    // Process transaction
    @PostMapping("/transactions")
    public ResponseEntity<ApiResponse<AccountDTO>> processTransaction(
            @Valid @RequestBody TransactionRequest request) {

        log.info("Processing transaction: {}", request);

        if (!hasPermission(request.getAccountNumber())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied", null));
        }

        java.util.Optional<Account> accountOpt = accountRepository.findByAccountNumber(request.getAccountNumber());

        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        Account account = accountOpt.get();

        if ("FROZEN".equals(account.getStatus())) {
            log.warn("Transaction rejected - Account is FROZEN: {}", account.getAccountNumber());

            Transaction failedTransaction = new Transaction();
            failedTransaction.setAccountId(account.getId());
            failedTransaction.setAccountNumber(account.getAccountNumber());
            failedTransaction.setTransactionType(request.getTransactionType());
            failedTransaction.setAmount(request.getAmount());
            failedTransaction.setBalanceAfter(account.getBalance());
            failedTransaction.setDescription("REJECTED: Account is frozen. " + (request.getDescription() != null ? request.getDescription() : ""));
            failedTransaction.setStatus("FAILED");
            failedTransaction.setReference("REJECTED_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
            transactionRepository.save(failedTransaction);

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Account is frozen. Please contact bank to unfreeze.", null));
        }

        if ("INACTIVE".equals(account.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Account is inactive.", null));
        }

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
                transaction.setDescription("FAILED: Insufficient funds. " + transaction.getDescription());
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

    // Delete account (Admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable String id) {
        log.info("Deleting account: {}", id);

        // Check if user is ADMIN
        String username = getCurrentUsername();
        User user = getCurrentUser();

        if (user == null || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied. Admin only.", null));
        }

        if (!accountRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Account not found", null));
        }

        accountRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Account deleted successfully", null));
    }

    // Get accounts by status (Admin only)
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAccountsByStatus(@PathVariable String status) {
        log.info("Fetching accounts with status: {}", status);

        // Check if user is ADMIN
        String username = getCurrentUsername();
        User user = getCurrentUser();

        if (user == null || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied. Admin only.", null));
        }

        List<AccountDTO> accounts = accountRepository.findByStatus(status)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, "Accounts retrieved successfully", accounts));
    }

    // Update account status (Admin only)
    @PutMapping("/{accountNumber}/status")
    public ResponseEntity<ApiResponse<AccountDTO>> updateAccountStatus(
            @PathVariable String accountNumber,
            @RequestParam String status) {

        log.info("Updating status for account: {} to {}", accountNumber, status);

        // Check if user is ADMIN
        String username = getCurrentUsername();
        User user = getCurrentUser();

        if (user == null || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Access denied. Admin only.", null));
        }

        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> {
                    account.setStatus(status);
                    Account updatedAccount = accountRepository.save(account);
                    return ResponseEntity.ok(new ApiResponse<>(
                            true,
                            "Account status updated successfully",
                            convertToDTO(updatedAccount)
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Helper method to convert Entity to DTO
    private AccountDTO convertToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountHolderName(account.getAccountHolderName());
        dto.setEmail(account.getEmail());
        dto.setOwnerUsername(account.getOwnerUsername());
        dto.setBalance(account.getBalance());
        dto.setAccountType(account.getAccountType());
        dto.setStatus(account.getStatus());
        return dto;
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

    private List<Account> findCustomerAccounts(User user) {
        List<Account> accounts = new ArrayList<>(accountRepository.findByOwnerUsernameOrderByCreatedAtDesc(user.getUsername()));

        if (accounts.isEmpty() && user.getAccountNumber() != null && !user.getAccountNumber().isEmpty()) {
            accountRepository.findByAccountNumber(user.getAccountNumber()).ifPresent(accounts::add);
        }

        return accounts;
    }

    private String resolveAccountNumber(String requestedAccountNumber) {
        if (requestedAccountNumber != null && !requestedAccountNumber.isBlank()) {
            return requestedAccountNumber;
        }

        String generated;
        do {
            generated = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(generated));

        return generated;
    }

    private void saveTransaction(
            Account account,
            String transactionType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description,
            String status) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }
}
