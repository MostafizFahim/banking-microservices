package com.banking.account.controller;

import com.banking.account.config.SecurityConfig;
import com.banking.account.dto.AccountDTO;
import com.banking.account.exception.ApiException;
import com.banking.account.security.JwtAuthenticationFilter;
import com.banking.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(
        controllers = AccountController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    private AccountDTO testAccountDTO;

    @BeforeEach
    void setUp() {
        testAccountDTO = AccountDTO.builder()
                .id("test-id-123")
                .accountNumber("1234567890")
                .accountHolderName("John Doe")
                .email("john@test.com")
                .ownerUsername("admin")
                .balance(new BigDecimal("1000.00"))
                .accountType("SAVINGS")
                .status("ACTIVE")
                .build();
    }

    @Test
    void testCreateAccount_Success() throws Exception {
        when(accountService.createAccount(any(AccountDTO.class))).thenReturn(testAccountDTO);

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
        when(accountService.createAccount(any(AccountDTO.class)))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Account number already exists"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAccountDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account number already exists"));
    }

    @Test
    void testCreateAccount_AllowsSameEmailForMultipleAccounts() throws Exception {
        when(accountService.createAccount(any(AccountDTO.class))).thenReturn(testAccountDTO);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAccountDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetAllAccounts() throws Exception {
        AccountDTO secondAccount = AccountDTO.builder()
                .id("test-id-456")
                .accountNumber("0987654321")
                .accountHolderName("Jane Smith")
                .email("jane@test.com")
                .ownerUsername("admin")
                .balance(new BigDecimal("2500.00"))
                .accountType("CHECKING")
                .status("ACTIVE")
                .build();

        when(accountService.getAllAccounts()).thenReturn(Arrays.asList(testAccountDTO, secondAccount));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testGetAccountById_Found() throws Exception {
        when(accountService.getAccountById("test-id-123")).thenReturn(testAccountDTO);

        mockMvc.perform(get("/api/accounts/test-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").value("1234567890"));
    }

    @Test
    void testGetAccountById_NotFound() throws Exception {
        when(accountService.getAccountById("non-existent"))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Account not found"));

        mockMvc.perform(get("/api/accounts/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void testDeposit_Success() throws Exception {
        when(accountService.deposit(anyString(), any(BigDecimal.class))).thenReturn(testAccountDTO);

        mockMvc.perform(post("/api/accounts/1234567890/deposit")
                        .param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Deposited $500.00")));
    }

    @Test
    void testWithdraw_Success() throws Exception {
        when(accountService.withdraw(anyString(), any(BigDecimal.class))).thenReturn(testAccountDTO);

        mockMvc.perform(post("/api/accounts/1234567890/withdraw")
                        .param("amount", "200.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Withdrew $200.00")));
    }

    @Test
    void testWithdraw_InsufficientFunds() throws Exception {
        when(accountService.withdraw(anyString(), any(BigDecimal.class)))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Insufficient funds"));

        mockMvc.perform(post("/api/accounts/1234567890/withdraw")
                        .param("amount", "2000.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void testDeleteAccount_Success() throws Exception {
        doNothing().when(accountService).deleteAccount("test-id-123");

        mockMvc.perform(delete("/api/accounts/test-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
    }
}
