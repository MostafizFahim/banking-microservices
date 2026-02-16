package com.banking.account.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String accountId;  // References Account.id

    @Column(nullable = false)
    private String accountNumber;  // For easy lookup

    @Column(nullable = false)
    private String transactionType;  // DEPOSIT, WITHDRAWAL

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    private BigDecimal balanceAfter;  // Balance after transaction

    private String description;

    @Column(nullable = false)
    private String status;  // COMPLETED, FAILED

    @Column(nullable = false)
    private String reference;  // Unique transaction reference

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (reference == null) {
            reference = "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        }
    }
}