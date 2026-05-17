package com.us.quy.authservice.services;

import com.us.quy.authservice.dtos.AccountRegistrationRequest;
import com.us.quy.authservice.dtos.LoginRequest;
import com.us.quy.authservice.dtos.LoginResponse;
import com.us.quy.authservice.dtos.RefreshTokenRequest;

public interface AccountService {
    void register(AccountRegistrationRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
