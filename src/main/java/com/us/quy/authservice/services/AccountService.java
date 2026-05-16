package com.us.quy.authservice.services;

import com.us.quy.authservice.dtos.AccountRegistrationRequest;

public interface AccountService {
    void register(AccountRegistrationRequest request);
}
