package com.banking.account.config;

import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class DataLoader {

    @Bean
    @Profile({"dev", "default"})  // Only loads in dev mode
    public CommandLineRunner loadDevData(AccountRepository accountRepository) {
        return args -> {
            if (accountRepository.count() == 0) {
                System.out.println("📝 Loading sample account data for DEVELOPMENT...");

                Account account1 = new Account();
                account1.setId(UUID.randomUUID().toString());
                account1.setAccountNumber("1234567890");
                account1.setAccountHolderName("John Doe");
                account1.setEmail("john@email.com");
                account1.setBalance(new BigDecimal("1000.00"));
                account1.setAccountType("SAVINGS");
                account1.setStatus("ACTIVE");
                account1.setCreatedAt(LocalDateTime.now());
                account1.setUpdatedAt(LocalDateTime.now());
                accountRepository.save(account1);

                System.out.println("✅ Loaded 1 sample account for development");
            }
        };
    }

    @Bean
    @Profile("prod")  // No sample data in production
    public CommandLineRunner loadProdData(AccountRepository accountRepository) {
        return args -> {
            System.out.println("🚀 Production mode - no sample data loaded");
        };
    }
}