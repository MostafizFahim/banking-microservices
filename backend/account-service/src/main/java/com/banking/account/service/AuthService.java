package com.banking.account.service;

import com.banking.account.dto.AuthRequest;
import com.banking.account.dto.AuthResponse;
import com.banking.account.entity.User;

public interface AuthService {

    User register(AuthRequest request);

    AuthResponse login(AuthRequest request);

    User registerAdmin(AuthRequest request);
}
