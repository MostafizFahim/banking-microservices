package com.banking.account.service;

import com.banking.account.dto.AccountDTO;
import com.banking.account.dto.TransactionRequest;
import com.banking.account.dto.TransactionResult;
import com.banking.account.entity.Account;
import com.banking.account.entity.Transaction;
import com.banking.account.entity.User;
import com.banking.account.exception.ApiException;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.TransactionRepository;
import com.banking.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultAccountService implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public AccountDTO createAccount(AccountDTO accountDTO) {
        String accountNumber = resolveAccountNumber(accountDTO.getAccountNumber());
        log.info("Creating new account: {}", accountNumber);

        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Account number already exists");
        }

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountHolderName(accountDTO.getAccountHolderName());
        account.setEmail(accountDTO.getEmail());
        account.setBalance(accountDTO.getBalance());
        account.setAccountType(accountDTO.getAccountType());
        account.setOwnerUsername(currentUserService.getCurrentUsername());
        account.setStatus("ACTIVE");

        Account savedAccount = accountRepository.save(account);
        linkAccountToCurrentUser(savedAccount);
        return convertToDTO(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        log.info("Fetching visible accounts");

        User user = currentUserService.getCurrentUser();
        if (user == null) {
            return List.of();
        }

        if ("ADMIN".equals(user.getRole())) {
            return accountRepository.findAll()
                    .stream()
                    .map(this::convertToDTO)
                    .toList();
        }

        return findCustomerAccounts(user)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountDTO getAccountById(String id) {
        log.info("Fetching account by id: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        assertCanView(account.getAccountNumber());
        return convertToDTO(account);
    }

    @Transactional(readOnly = true)
    public AccountDTO getAccountByNumber(String accountNumber) {
        log.info("Fetching account by number: {}", accountNumber);

        Account account = findAccountByNumber(accountNumber);
        assertCanView(accountNumber);
        return convertToDTO(account);
    }

    @Transactional
    public AccountDTO deposit(String accountNumber, BigDecimal amount) {
        log.info("Depositing {} to account: {}", amount, accountNumber);
        assertCanView(accountNumber);

        Account account = findAccountByNumber(accountNumber);
        assertAccountCanTransact(account, "deposit");

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);
        saveTransaction(updatedAccount, "DEPOSIT", amount, updatedAccount.getBalance(), "Deposit transaction", "COMPLETED");
        return convertToDTO(updatedAccount);
    }

    @Transactional
    public AccountDTO withdraw(String accountNumber, BigDecimal amount) {
        log.info("Withdrawing {} from account: {}", amount, accountNumber);
        assertCanView(accountNumber);

        Account account = findAccountByNumber(accountNumber);
        assertAccountCanTransact(account, "withdraw");

        if (account.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account updatedAccount = accountRepository.save(account);
        saveTransaction(updatedAccount, "WITHDRAWAL", amount, updatedAccount.getBalance(), "Withdrawal transaction", "COMPLETED");
        return convertToDTO(updatedAccount);
    }

    @Transactional
    public TransactionResult processTransaction(TransactionRequest request) {
        log.info("Processing transaction: {}", request);
        assertCanView(request.getAccountNumber());

        Account account = findAccountByNumber(request.getAccountNumber());

        if ("FROZEN".equals(account.getStatus())) {
            log.warn("Transaction rejected - Account is FROZEN: {}", account.getAccountNumber());
            saveTransaction(account, request.getTransactionType(), request.getAmount(), account.getBalance(),
                    "REJECTED: Account is frozen. " + safeDescription(request.getDescription()), "FAILED");
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is frozen. Please contact bank to unfreeze.");
        }

        if ("INACTIVE".equals(account.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is inactive.");
        }

        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription() != null
                ? request.getDescription()
                : request.getTransactionType() + " transaction");

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
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient funds");
            }

            BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
            account.setBalance(newBalance);
            transaction.setBalanceAfter(newBalance);
            transaction.setStatus("COMPLETED");
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid transaction type");
        }

        Account updatedAccount = accountRepository.save(account);
        Transaction savedTransaction = transactionRepository.save(transaction);
        return new TransactionResult(convertToDTO(updatedAccount), savedTransaction.getReference());
    }

    @Transactional
    public void deleteAccount(String id) {
        log.info("Deleting account: {}", id);
        assertAdmin();

        if (!accountRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Account not found");
        }

        accountRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> getAccountsByStatus(String status) {
        log.info("Fetching accounts with status: {}", status);
        assertAdmin();

        return accountRepository.findByStatus(status)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public AccountDTO updateAccountStatus(String accountNumber, String status) {
        log.info("Updating status for account: {} to {}", accountNumber, status);
        assertAdmin();

        Account account = findAccountByNumber(accountNumber);
        account.setStatus(status);
        return convertToDTO(accountRepository.save(account));
    }

    public boolean hasPermission(String accountNumber) {
        User user = currentUserService.getCurrentUser();
        String username = currentUserService.getCurrentUsername();

        if (user == null) {
            return false;
        }
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> username.equals(account.getOwnerUsername())
                        || accountNumber.equals(user.getAccountNumber()))
                .orElse(false);
    }

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

    private Account findAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private void assertCanView(String accountNumber) {
        if (!hasPermission(accountNumber)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void assertAdmin() {
        if (!currentUserService.isCurrentUserAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Admin only.");
        }
    }

    private void assertAccountCanTransact(Account account, String action) {
        if ("FROZEN".equals(account.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is frozen. Cannot " + action + ".");
        }
    }

    private void linkAccountToCurrentUser(Account savedAccount) {
        User user = currentUserService.getCurrentUser();
        if (user != null && (user.getAccountNumber() == null || user.getAccountNumber().isEmpty())) {
            user.setAccountNumber(savedAccount.getAccountNumber());
            userRepository.save(user);
            log.info("Linked account {} to user {}", savedAccount.getAccountNumber(), user.getUsername());
        }
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

    private String safeDescription(String description) {
        return description == null ? "" : description;
    }
}
