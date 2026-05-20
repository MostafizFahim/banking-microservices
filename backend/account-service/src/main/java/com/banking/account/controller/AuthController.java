package com.banking.account.controller;

import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.AuthRequest;
import com.banking.account.dto.AuthResponse;
import com.banking.account.entity.User;
import com.banking.account.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "User registered successfully",
                authService.register(request)
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Login successful",
                authService.login(request)
        ));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth endpoint is working!");
    }

    @PostMapping("/register-admin")
    public ResponseEntity<ApiResponse<User>> registerAdmin(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Admin registered successfully",
                authService.registerAdmin(request)
        ));
    }
}
