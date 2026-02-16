package com.banking.account.repository;

import com.banking.account.entity.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    public void testSaveAccount() {
        // Arrange
        Account account = new Account();
        account.setAccountNumber("9999999999");
        account.setAccountHolderName("Test User");
        account.setEmail("test@test.com");
        account.setBalance(new BigDecimal("5000.00"));
        account.setAccountType("SAVINGS");
        account.setStatus("ACTIVE");

        // Act
        Account savedAccount = accountRepository.save(account);

        // Assert
        assertThat(savedAccount).isNotNull();
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getAccountNumber()).isEqualTo("9999999999");
    }

    @Test
    public void testFindByAccountNumber() {
        // Arrange
        Account account = new Account();
        account.setAccountNumber("8888888888");
        account.setAccountHolderName("Find Test");
        account.setEmail("find@test.com");
        account.setBalance(new BigDecimal("3000.00"));
        account.setAccountType("CHECKING");
        account.setStatus("ACTIVE");

        entityManager.persistAndFlush(account);

        // Act
        Optional<Account> found = accountRepository.findByAccountNumber("8888888888");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getAccountHolderName()).isEqualTo("Find Test");
    }

    @Test
    public void testExistsByAccountNumber() {
        // Arrange
        Account account = new Account();
        account.setAccountNumber("7777777777");
        account.setAccountHolderName("Exists Test");
        account.setEmail("exists@test.com");
        account.setBalance(new BigDecimal("1000.00"));
        account.setAccountType("SAVINGS");
        account.setStatus("ACTIVE");

        entityManager.persistAndFlush(account);

        // Act
        boolean exists = accountRepository.existsByAccountNumber("7777777777");
        boolean notExists = accountRepository.existsByAccountNumber("0000000000");

        // Assert
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}