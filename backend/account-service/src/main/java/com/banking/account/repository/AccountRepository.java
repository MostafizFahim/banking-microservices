package com.banking.account.repository;

import com.banking.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByAccountHolderNameContainingIgnoreCase(String name);

    List<Account> findByStatus(String status);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);
}