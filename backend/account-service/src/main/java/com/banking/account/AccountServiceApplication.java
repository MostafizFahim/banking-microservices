package com.banking.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
        System.out.println("✅ Account Service Started Successfully!");
        System.out.println("📝 Access H2 Console at: http://localhost:8081/h2-console");
        System.out.println("🔗 JDBC URL: jdbc:h2:mem:bankingdb");
    }
}