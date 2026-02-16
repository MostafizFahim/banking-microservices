package com.banking.account.controller;

import com.banking.account.dto.AccountDTO;
import com.banking.account.dto.TransactionRequest;
import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AccountController.class)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Account testAccount;
    private AccountDTO testAccountDTO;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId("test-id-123");
        testAccount.setAccountNumber("1234567890");
        testAccount.setAccountHolderName("John Doe");
        testAccount.setEmail("john@test.com");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setAccountType("SAVINGS");
        testAccount.setStatus("ACTIVE");

        testAccountDTO = new AccountDTO();
        testAccountDTO.setAccountNumber("1234567890");
        testAccountDTO.setAccountHolderName("John Doe");
        testAccountDTO.setEmail("john@test.com");
        testAccountDTO.setBalance(new BigDecimal("1000.00"));
        testAccountDTO.setAccountType("SAVINGS");
    }

    @Test
    void testCreateAccount_Success() throws Exception {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAccountDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account created successfully"))
                .andExpect(jsonPath("$.data.accountNumber").value("1234567890"));
    }

    @Test
    void testCreateAccount_DuplicateAccountNumber() throws Exception {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAccountDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account number already exists"));
    }

    @Test
    void testCreateAccount_DuplicateEmail() throws Exception {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.existsByEmail(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAccountDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void testGetAllAccounts() throws Exception {
        Account secondAccount = new Account();
        secondAccount.setId("test-id-456");
        secondAccount.setAccountNumber("0987654321");
        secondAccount.setAccountHolderName("Jane Smith");
        secondAccount.setEmail("jane@test.com");
        secondAccount.setBalance(new BigDecimal("2500.00"));
        secondAccount.setAccountType("CHECKING");
        secondAccount.setStatus("ACTIVE");

        when(accountRepository.findAll()).thenReturn(Arrays.asList(testAccount, secondAccount));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testGetAccountById_Found() throws Exception {
        when(accountRepository.findById("test-id-123")).thenReturn(Optional.of(testAccount));

        mockMvc.perform(get("/api/accounts/test-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").value("1234567890"));
    }

    @Test
    void testGetAccountById_NotFound() throws Exception {
        when(accountRepository.findById("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void testDeposit_Success() throws Exception {
        when(accountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts/1234567890/deposit")
                        .param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Deposited $500.00")));
    }

    @Test
    void testWithdraw_Success() throws Exception {
        when(accountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts/1234567890/withdraw")
                        .param("amount", "200.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Withdrew $200.00")));
    }

    @Test
    void testWithdraw_InsufficientFunds() throws Exception {
        when(accountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(testAccount));

        mockMvc.perform(post("/api/accounts/1234567890/withdraw")
                        .param("amount", "2000.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void testDeleteAccount_Success() throws Exception {
        when(accountRepository.existsById("test-id-123")).thenReturn(true);
        doNothing().when(accountRepository).deleteById("test-id-123");

        mockMvc.perform(delete("/api/accounts/test-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
    }
}