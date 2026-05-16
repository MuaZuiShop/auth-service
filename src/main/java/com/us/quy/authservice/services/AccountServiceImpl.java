package com.us.quy.authservice.services;

import com.us.quy.authservice.dtos.AccountRegistrationRequest;
import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.kafka.providers.AccountPublisher;
import com.us.quy.authservice.mapper.AccountMapper;
import com.us.quy.authservice.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountPublisher accountPublisher;
    private final PasswordEncoder encoder;

    private static final String TOPIC = "auth.user-onboarding";

    @Override
    public void register(AccountRegistrationRequest request) {
        validateFieldNotNull(request);
        validateEmailFormat(request);
        validatePasswordCondition(request);

        request.setPassword(encoder.encode(request.getPassword()));
        AccountEntity account = accountRepository.save(accountMapper.fromRegisterRequest(request));
        accountPublisher.registerEvent(TOPIC, account);
    }

    void validateFieldNotNull(AccountRegistrationRequest request) {
        if (StringUtils.isEmpty(request.getUsername())
            || StringUtils.isEmpty(request.getPassword())
            || StringUtils.isEmpty(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    void validateEmailFormat(AccountRegistrationRequest request) {
        if (!request.getEmail().contains("@")
            || !request.getEmail().contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    void validatePasswordCondition(AccountRegistrationRequest request) {
        String password = request.getPassword();

        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (!password.matches(".*[a-z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (!password.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
