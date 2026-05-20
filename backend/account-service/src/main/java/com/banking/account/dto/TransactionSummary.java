package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TransactionSummary {
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal netBalance;
    private long totalTransactions;
}
