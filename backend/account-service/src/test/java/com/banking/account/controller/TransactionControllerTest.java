package com.banking.account.controller;

import com.banking.account.config.SecurityConfig;
import com.banking.account.dto.TransactionDTO;
import com.banking.account.dto.TransactionSummary;
import com.banking.account.security.JwtAuthenticationFilter;
import com.banking.account.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(
        controllers = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private TransactionDTO depositTransaction;
    private TransactionDTO withdrawalTransaction;

    @BeforeEach
    void setUp() {
        depositTransaction = TransactionDTO.builder()
                .id("txn-1")
                .accountNumber("1234567890")
                .transactionType("DEPOSIT")
                .amount(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status("COMPLETED")
                .reference("TXN123456")
                .timestamp(LocalDateTime.now())
                .build();

        withdrawalTransaction = TransactionDTO.builder()
                .id("txn-2")
                .accountNumber("1234567890")
                .transactionType("WITHDRAWAL")
                .amount(new BigDecimal("200.00"))
                .balanceAfter(new BigDecimal("1300.00"))
                .status("COMPLETED")
                .reference("TXN123457")
                .timestamp(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    void testGetAccountTransactions() throws Exception {
        when(transactionService.getAccountTransactions("1234567890"))
                .thenReturn(Arrays.asList(depositTransaction, withdrawalTransaction));

        mockMvc.perform(get("/api/transactions/account/1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testGetTransactionsByType() throws Exception {
        when(transactionService.getTransactionsByType("1234567890", "DEPOSIT"))
                .thenReturn(Arrays.asList(depositTransaction));

        mockMvc.perform(get("/api/transactions/account/1234567890/type/DEPOSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].transactionType").value("DEPOSIT"));
    }

    @Test
    void testGetTransactionByReference() throws Exception {
        when(transactionService.getTransactionByReference("TXN123456")).thenReturn(depositTransaction);

        mockMvc.perform(get("/api/transactions/reference/TXN123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reference").value("TXN123456"));
    }

    @Test
    void testGetTransactionSummary() throws Exception {
        when(transactionService.getTransactionSummary("1234567890"))
                .thenReturn(new TransactionSummary(
                        new BigDecimal("500.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("300.00"),
                        2
                ));

        mockMvc.perform(get("/api/transactions/account/1234567890/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDeposits").value(500.00))
                .andExpect(jsonPath("$.data.totalWithdrawals").value(200.00))
                .andExpect(jsonPath("$.data.totalTransactions").value(2));
    }
}
