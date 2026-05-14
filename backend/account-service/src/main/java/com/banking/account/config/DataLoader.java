package com.banking.account.config;

import com.banking.account.entity.Account;
import com.banking.account.entity.Transaction;
import com.banking.account.entity.User;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.TransactionRepository;
import com.banking.account.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
@Slf4j
public class DataLoader {

    @Bean
    @Profile({"dev", "default"})
    public CommandLineRunner loadDevData(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (accountRepository.count() == 0) {
                log.info("Loading sample banking data for development");

                Account account = new Account();
                account.setAccountNumber("1234567890");
                account.setAccountHolderName("John Doe");
                account.setEmail("john@email.com");
                account.setOwnerUsername("john");
                account.setBalance(new BigDecimal("1325.00"));
                account.setAccountType("SAVINGS");
                account.setStatus("ACTIVE");
                account.setCreatedAt(LocalDateTime.now());
                account.setUpdatedAt(LocalDateTime.now());
                Account savedAccount = accountRepository.save(account);

                seedTransaction(
                        transactionRepository,
                        savedAccount,
                        "DEPOSIT",
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        "Opening balance",
                        LocalDateTime.now().minusDays(6));
                seedTransaction(
                        transactionRepository,
                        savedAccount,
                        "WITHDRAWAL",
                        new BigDecimal("125.00"),
                        new BigDecimal("875.00"),
                        "ATM withdrawal",
                        LocalDateTime.now().minusDays(4));
                seedTransaction(
                        transactionRepository,
                        savedAccount,
                        "DEPOSIT",
                        new BigDecimal("500.00"),
                        new BigDecimal("1375.00"),
                        "Salary deposit",
                        LocalDateTime.now().minusDays(2));
                seedTransaction(
                        transactionRepository,
                        savedAccount,
                        "WITHDRAWAL",
                        new BigDecimal("50.00"),
                        new BigDecimal("1325.00"),
                        "Bill payment",
                        LocalDateTime.now().minusDays(1));

                if (!userRepository.existsByUsername("john")) {
                    User demoUser = new User();
                    demoUser.setUsername("john");
                    demoUser.setPassword(passwordEncoder.encode("password"));
                    demoUser.setRole("CUSTOMER");
                    demoUser.setEmail("john@email.com");
                    demoUser.setAccountNumber(savedAccount.getAccountNumber());
                    userRepository.save(demoUser);
                }

                log.info("Loaded sample account, user, and transactions for development");
                log.info("Demo login: john / password");
            }
        };
    }

    @Bean
    @Profile("prod")
    public CommandLineRunner loadProdData() {
        return args -> log.info("Production mode: no sample data loaded");
    }

    private void seedTransaction(
            TransactionRepository transactionRepository,
            Account account,
            String type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description,
            LocalDateTime timestamp) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setStatus("COMPLETED");
        transaction.setTimestamp(timestamp);
        transactionRepository.save(transaction);
    }
}
