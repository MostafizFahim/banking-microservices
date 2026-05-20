package com.banking.account.service;

import com.banking.account.dto.AuthRequest;
import com.banking.account.dto.AuthResponse;
import com.banking.account.entity.User;
import com.banking.account.exception.ApiException;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.UserRepository;
import com.banking.account.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultAuthService implements AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUserService;

    @Value("${admin.secret.key:SUPER_SECRET_KEY_123}")
    private String adminSecret;

    @Transactional
    public User register(AuthRequest request) {
        validateUniqueUser(request);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("CUSTOMER");
        user.setEmail(request.getEmail() != null ? request.getEmail() : request.getUsername() + "@bank.com");
        user.setAccountNumber("");

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        List<String> accountNumbers = getAccountNumbers(user);
        String defaultAccountNumber = accountNumbers.isEmpty() ? "" : accountNumbers.get(0);
        String token = jwtService.generateToken(user.getUsername(), user.getRole(), defaultAccountNumber);

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole(),
                defaultAccountNumber,
                accountNumbers
        );
    }

    @Transactional
    public User registerAdmin(AuthRequest request) {
        if (adminSecret == null || !adminSecret.equals(request.getAdminKey())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid admin key");
        }

        if (userRepository.existsByRole("ADMIN") && !currentUserService.isCurrentUserAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin registration requires an existing admin");
        }

        validateUniqueUser(request);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ADMIN");
        user.setEmail(request.getEmail() != null ? request.getEmail() : request.getUsername() + "@admin.com");
        user.setAccountNumber("");

        return userRepository.save(user);
    }

    private void validateUniqueUser(AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
    }

    private List<String> getAccountNumbers(User user) {
        List<String> accountNumbers = accountRepository.findByOwnerUsernameOrderByCreatedAtDesc(user.getUsername())
                .stream()
                .map(account -> account.getAccountNumber())
                .toList();

        if (accountNumbers.isEmpty() && user.getAccountNumber() != null && !user.getAccountNumber().isBlank()) {
            return List.of(user.getAccountNumber());
        }

        return accountNumbers;
    }
}
