package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionResult {
    private AccountDTO account;
    private String reference;
}
