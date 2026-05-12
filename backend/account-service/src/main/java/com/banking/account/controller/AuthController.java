package com.banking.account.controller;

import com.banking.account.dto.AuthRequest;
import com.banking.account.dto.AuthResponse;
import com.banking.account.dto.ApiResponse;
import com.banking.account.entity.User;
import com.banking.account.repository.UserRepository;
import com.banking.account.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${admin.secret.key:SUPER_SECRET_KEY_123}")
    private String ADMIN_SECRET;  // Read from environment variable with default for dev

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Username already exists", null));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("CUSTOMER");
        user.setEmail(request.getEmail() != null ? request.getEmail() : request.getUsername() + "@bank.com");
        user.setAccountNumber("");

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "User registered successfully", savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
            String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getAccountNumber());

            AuthResponse response = new AuthResponse(
                    token,
                    user.getUsername(),
                    user.getRole(),
                    user.getAccountNumber()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Invalid username or password", null));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth endpoint is working!");
    }

    @PostMapping("/register-admin")
    public ResponseEntity<ApiResponse<User>> registerAdmin(@RequestBody AuthRequest request) {
        // Validate admin key from environment variable
        if (ADMIN_SECRET == null || !ADMIN_SECRET.equals(request.getAdminKey())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Invalid admin key", null));
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Username already exists", null));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ADMIN");
        user.setEmail(request.getEmail() != null ? request.getEmail() : request.getUsername() + "@admin.com");
        user.setAccountNumber("");

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Admin registered successfully", savedUser));
    }
}