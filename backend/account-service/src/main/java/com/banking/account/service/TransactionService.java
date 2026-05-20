package com.banking.account.service;

import com.banking.account.dto.TransactionDTO;
import com.banking.account.dto.TransactionSummary;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    List<TransactionDTO> getMyTransactions();

    List<TransactionDTO> getAccountTransactions(String accountNumber);

    List<TransactionDTO> getTransactionsByType(String accountNumber, String type);

    List<TransactionDTO> getTransactionsByDateRange(String accountNumber, LocalDateTime startDate, LocalDateTime endDate);

    TransactionDTO getTransactionByReference(String reference);

    TransactionSummary getTransactionSummary(String accountNumber);
}
