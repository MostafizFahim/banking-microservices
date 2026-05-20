package com.banking.account.service;

import com.banking.account.dto.AccountDTO;
import com.banking.account.dto.TransactionRequest;
import com.banking.account.dto.TransactionResult;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountDTO createAccount(AccountDTO accountDTO);

    List<AccountDTO> getAllAccounts();

    AccountDTO getAccountById(String id);

    AccountDTO getAccountByNumber(String accountNumber);

    AccountDTO deposit(String accountNumber, BigDecimal amount);

    AccountDTO withdraw(String accountNumber, BigDecimal amount);

    TransactionResult processTransaction(TransactionRequest request);

    void deleteAccount(String id);

    List<AccountDTO> getAccountsByStatus(String status);

    AccountDTO updateAccountStatus(String accountNumber, String status);

    boolean hasPermission(String accountNumber);
}
