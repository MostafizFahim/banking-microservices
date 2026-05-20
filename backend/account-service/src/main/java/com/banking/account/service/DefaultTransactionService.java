package com.banking.account.service;

import com.banking.account.dto.TransactionDTO;
import com.banking.account.dto.TransactionSummary;
import com.banking.account.entity.Account;
import com.banking.account.entity.Transaction;
import com.banking.account.entity.User;
import com.banking.account.exception.ApiException;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultTransactionService implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public List<TransactionDTO> getMyTransactions() {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        List<String> accountNumbers = getVisibleAccountNumbers(user);
        if (accountNumbers.isEmpty()) {
            return List.of();
        }

        return transactionRepository.findByAccountNumberInOrderByTimestampDesc(accountNumbers)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getAccountTransactions(String accountNumber) {
        log.info("Fetching transactions for account: {}", accountNumber);
        assertCanView(accountNumber);

        return transactionRepository.findByAccountNumberOrderByTimestampDesc(accountNumber)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByType(String accountNumber, String type) {
        log.info("Fetching {} transactions for account: {}", type, accountNumber);
        assertCanView(accountNumber);

        return transactionRepository.findByAccountNumberAndTransactionTypeOrderByTimestampDesc(accountNumber, type)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByDateRange(
            String accountNumber,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        log.info("Fetching transactions for account: {} from {} to {}", accountNumber, startDate, endDate);
        assertCanView(accountNumber);

        return transactionRepository.findTransactionsByDateRange(accountNumber, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionDTO getTransactionByReference(String reference) {
        log.info("Fetching transaction by reference: {}", reference);

        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!accountService.hasPermission(transaction.getAccountNumber())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Transaction not found");
        }

        return convertToDTO(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionSummary getTransactionSummary(String accountNumber) {
        log.info("Fetching transaction summary for account: {}", accountNumber);
        assertCanView(accountNumber);

        BigDecimal totalDeposits = transactionRepository.getTotalByType(accountNumber, "DEPOSIT");
        BigDecimal totalWithdrawals = transactionRepository.getTotalByType(accountNumber, "WITHDRAWAL");

        if (totalDeposits == null) {
            totalDeposits = BigDecimal.ZERO;
        }
        if (totalWithdrawals == null) {
            totalWithdrawals = BigDecimal.ZERO;
        }

        return new TransactionSummary(
                totalDeposits,
                totalWithdrawals,
                totalDeposits.subtract(totalWithdrawals),
                transactionRepository.findByAccountNumberOrderByTimestampDesc(accountNumber).size()
        );
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

    private void assertCanView(String accountNumber) {
        if (!accountService.hasPermission(accountNumber)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
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
