package com.banking.account.controller;

import com.banking.account.entity.Transaction;
import com.banking.account.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Transaction depositTransaction;
    private Transaction withdrawalTransaction;

    @BeforeEach
    void setUp() {
        depositTransaction = new Transaction();
        depositTransaction.setId("txn-1");
        depositTransaction.setAccountNumber("1234567890");
        depositTransaction.setTransactionType("DEPOSIT");
        depositTransaction.setAmount(new BigDecimal("500.00"));
        depositTransaction.setBalanceAfter(new BigDecimal("1500.00"));
        depositTransaction.setStatus("COMPLETED");
        depositTransaction.setReference("TXN123456");
        depositTransaction.setTimestamp(LocalDateTime.now());

        withdrawalTransaction = new Transaction();
        withdrawalTransaction.setId("txn-2");
        withdrawalTransaction.setAccountNumber("1234567890");
        withdrawalTransaction.setTransactionType("WITHDRAWAL");
        withdrawalTransaction.setAmount(new BigDecimal("200.00"));
        withdrawalTransaction.setBalanceAfter(new BigDecimal("1300.00"));
        withdrawalTransaction.setStatus("COMPLETED");
        withdrawalTransaction.setReference("TXN123457");
        withdrawalTransaction.setTimestamp(LocalDateTime.now().minusHours(1));
    }

    @Test
    void testGetAccountTransactions() throws Exception {
        when(transactionRepository.findByAccountNumberOrderByTimestampDesc("1234567890"))
                .thenReturn(Arrays.asList(depositTransaction, withdrawalTransaction));

        mockMvc.perform(get("/api/transactions/account/1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testGetTransactionsByType() throws Exception {
        when(transactionRepository.findByAccountNumberAndTransactionTypeOrderByTimestampDesc("1234567890", "DEPOSIT"))
                .thenReturn(Arrays.asList(depositTransaction));

        mockMvc.perform(get("/api/transactions/account/1234567890/type/DEPOSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].transactionType").value("DEPOSIT"));
    }

    @Test
    void testGetTransactionByReference() throws Exception {
        when(transactionRepository.findById("TXN123456")).thenReturn(Optional.of(depositTransaction));

        mockMvc.perform(get("/api/transactions/reference/TXN123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reference").value("TXN123456"));
    }

    @Test
    void testGetTransactionSummary() throws Exception {
        when(transactionRepository.getTotalByType("1234567890", "DEPOSIT"))
                .thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.getTotalByType("1234567890", "WITHDRAWAL"))
                .thenReturn(new BigDecimal("200.00"));
        when(transactionRepository.findByAccountNumberOrderByTimestampDesc("1234567890"))
                .thenReturn(Arrays.asList(depositTransaction, withdrawalTransaction));

        mockMvc.perform(get("/api/transactions/account/1234567890/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDeposits").value(500.00))
                .andExpect(jsonPath("$.data.totalWithdrawals").value(200.00))
                .andExpect(jsonPath("$.data.totalTransactions").value(2));
    }
}